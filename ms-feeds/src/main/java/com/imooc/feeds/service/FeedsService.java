package com.imooc.feeds.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.imooc.commons.constant.ApiConstant;
import com.imooc.commons.constant.RedisKeyConstant;
import com.imooc.commons.exception.ParameterException;
import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.pojo.Feeds;
import com.imooc.commons.model.vo.FeedsVO;
import com.imooc.commons.model.vo.ShortDinerInfo;
import com.imooc.commons.model.vo.SignInDinerInfo;
import com.imooc.commons.utils.AssertUtil;
import com.imooc.feeds.mapper.FeedsMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedsService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-follow-server}")
    private String followServerName;
    @Value("${service.name.ms-diners-server}")
    private String dinersServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private FeedsMapper feedsMapper;

    /**
     * 添加 Feed
     *
     * @param feeds       feed数据
     * @param accessToken 登录token
     */
    @Transactional(rollbackFor = Exception.class)
    public void create(Feeds feeds, String accessToken) {
        // 非空校验
        AssertUtil.isNotEmpty(feeds.getContent(), "请输入内容");
        AssertUtil.isTrue(feeds.getContent().length() > 255, "输入内容太多，请重新输入");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // Feed 关联用户信息
        feeds.setFkDinerId(dinerInfo.getId());
        // 添加 Feed
        int count = feedsMapper.save(feeds);
        AssertUtil.isTrue(count == 0, "添加失败");

        // 推送到粉丝的列表中 -- 后续这里应该采用异步消息队列解决性能问题

        // 先获取我的粉丝
        List<Integer> followers = findFollowers(dinerInfo.getId());

        // 推送 Feeds，以时间作为分数存储至 ZSet
        long now = System.currentTimeMillis();
        followers.forEach(follower -> {
            String key = RedisKeyConstant.following_feeds.getKey() + follower;
            redisTemplate.opsForZSet().add(key, feeds.getId(), now);
        });
    }

    /**
     * 获取用户粉丝
     *
     * @param dinerId 用户id
     * @return
     */
    private List<Integer> findFollowers(Integer dinerId) {
        String url = followServerName + "followers/" + dinerId;
        ResultInfo followersResultInfo = restTemplate.getForObject(url, ResultInfo.class);
        if (followersResultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(followersResultInfo.getCode(), followersResultInfo.getMessage());
        }

        List<Integer> followers = (List<Integer>) followersResultInfo.getData();
        return followers;
    }

    /**
     * 获取登录用户信息
     *
     * @param accessToken
     * @return
     */
    private SignInDinerInfo loadSignInDinerInfo(String accessToken) {
        // 是否有 accessToken
        AssertUtil.mustLogin(accessToken);
        String url = oauthServerName + "user/me?access_token={accessToken}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken);
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }
        // 这里的data是一个LinkedHashMap，SignInDinerInfo
        SignInDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new SignInDinerInfo(), false);
        return dinerInfo;
    }

    /**
     * 变更 Feed
     *
     * @param followingDinerId 关注的好友的 ID
     * @param accessToken      登录用户token
     * @param type             1 关注 0 取关
     */
    @Transactional(rollbackFor = Exception.class)
    public void addFollowingFeeds(Integer followingDinerId, String accessToken, int type) {
        // 请选择关注的好友
        AssertUtil.isTrue(followingDinerId == null || followingDinerId < 1,
                "请选择关注的好友");
        // 获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        // 获取关注/取关食客所有 Feed
        List<Feeds> followingFeeds = feedsMapper.findByDinerId(followingDinerId);
        if (followingFeeds == null || followingFeeds.isEmpty()) {
            return;
        }
        // 我关注的好友的 FeedsKey
        String key = RedisKeyConstant.following_feeds.getKey() + dinerInfo.getId();
        if (type == 0) { // 取关
            List<Integer> feedIds = followingFeeds.stream()
                    .map(feed -> feed.getId())
                    .collect(Collectors.toList());
            redisTemplate.opsForZSet().remove(key, feedIds.toArray(new Integer[]{}));
        } else { // 关注
            Set<ZSetOperations.TypedTuple> typedTuples = followingFeeds.stream()
                    .map(feed -> new DefaultTypedTuple<>(feed.getId(), (double) feed.getUpdateDate().getTime()))
                    .collect(Collectors.toSet());
            redisTemplate.opsForZSet().add(key, typedTuples);
        }
    }

    public List<FeedsVO> selectForPage(Integer page, String accessToken) {
        if (page == null) {
            page = 1;
        }
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        String key = RedisKeyConstant.following_feeds.getKey() + dinerInfo.getId();
        long pageStart = (page - 1) * ApiConstant.PAGE_SIZE;
        long pageEnd = page * ApiConstant.PAGE_SIZE - 1;
        Set<Integer> feedIds = redisTemplate.opsForZSet().reverseRange(key, pageStart, pageEnd);
        if (CollectionUtil.isEmpty(feedIds)) {
            return new ArrayList<>();
        }

        List<Integer> feedsDinerIds = new ArrayList<>();
        List<Feeds> feeds = feedsMapper.findFeedsByIds(feedIds);
        List<FeedsVO> feedsVOS = feeds.stream().map(feed -> {
            FeedsVO feedsVO = new FeedsVO();
            BeanUtil.copyProperties(feed, feedsVO, true);
            feedsDinerIds.add(feed.getFkDinerId());
            return feedsVO;
        }).collect(Collectors.toList());

        String url = dinersServerName + "findByIds?access_token=${accessToken}&ids={ids}";
        ResultInfo resultInfo = restTemplate.getForObject(url, ResultInfo.class, accessToken, StrUtil.join(",", feedsDinerIds));
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            throw new ParameterException(resultInfo.getCode(), resultInfo.getMessage());
        }

        List<LinkedHashMap> dinerInfoMap = (List<LinkedHashMap>) resultInfo.getData();
        Map<Integer, ShortDinerInfo> dinerInfos = dinerInfoMap.stream()
                .collect(Collectors.toMap(
                        diner -> (Integer) diner.get("id"),
                        diner -> BeanUtil.fillBeanWithMap(diner, new ShortDinerInfo(), true)
                ));
        feedsVOS.forEach(feedsVO -> {
            feedsVO.setDinerInfo(dinerInfos.get(feedsVO.getFkDinerId()));
        });
        return feedsVOS;
    }
}
