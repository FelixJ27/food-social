package com.imooc.follow.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import com.imooc.commons.constant.ApiConstant;
import com.imooc.commons.constant.RedisKeyConstant;
import com.imooc.commons.exception.ParameterException;
import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.pojo.Follow;
import com.imooc.commons.model.vo.ShortDinerInfo;
import com.imooc.commons.model.vo.SignInDinerInfo;
import com.imooc.commons.utils.AssertUtil;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.follow.mapper.FollowMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Value("${service.name.ms-diners-server}")
    private String dinerServerName;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RestTemplate restTemplate;

    /**
     * 关注/取关
     *
     * @param followDinnerId 关注的食客id
     * @param isFollowed     是否关注 1-关注 0-取关
     * @param accessToken    登录用户token
     * @param path           访问地址
     * @return
     */
    public ResultInfo follow(Integer followDinnerId, int isFollowed, String accessToken, String path) {
        AssertUtil.isTrue(followDinnerId == null || followDinnerId < 1, "请选择要关注的人");
        //获取登录用户信息
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        //获取当前用户与需要关注用户的关注信息
        Follow follow = followMapper.selectFollow(dinerInfo.getId(), followDinnerId);
        //如果没有关注信息，且要进行关注
        if (follow == null && isFollowed == 1) {
            int count = followMapper.save(dinerInfo.getId(), followDinnerId);
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followDinnerId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,
                    "关注成功", path, "关注成功");
        }
        if (follow != null && isFollowed == 1 && follow.getIsValid() == 0) {
            int count = followMapper.update(follow.getId(), isFollowed);
            if (count == 1) {
                addToRedisSet(dinerInfo.getId(), followDinnerId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,
                    "关注成功", path, "关注成功");
        }
        if (follow != null && isFollowed == 0 && follow.getIsValid() == 1) {
            int count = followMapper.update(follow.getId(), isFollowed);
            if (count == 1) {
                removeFromRedisSet(dinerInfo.getId(), followDinnerId);
            }
            return ResultInfoUtil.build(ApiConstant.SUCCESS_CODE,
                    "取关成功", path, "取关成功");
        }
        return ResultInfoUtil.buildSuccess(path, "操作成功");
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
     * 添加关注列表到 Redis
     *
     * @param dinerId
     * @param followDinerId
     */
    private void addToRedisSet(Integer dinerId, Integer followDinerId) {
        redisTemplate.opsForSet().add(RedisKeyConstant.following.getKey() + dinerId, followDinerId);
        redisTemplate.opsForSet().add(RedisKeyConstant.followers.getKey() + followDinerId, dinerId);
    }

    /**
     * 移除 Redis 关注列表
     *
     * @param dinerId
     * @param followDinerId
     */
    private void removeFromRedisSet(Integer dinerId, Integer followDinerId) {
        redisTemplate.opsForSet().remove(RedisKeyConstant.following.getKey() + dinerId, followDinerId);
        redisTemplate.opsForSet().remove(RedisKeyConstant.followers.getKey() + followDinerId, dinerId);
    }

    /**
     * 共同好友
     *
     * @param dinerId     要查看的食客ID
     * @param accessToken 登录用户token
     * @param path 访问地址
     * @return
     */
    public ResultInfo findCommonsFriends(Integer dinerId, String accessToken, String path) {
        AssertUtil.isTrue(dinerId == null || dinerId < 1, "请选择要查看的人");
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        String loginDinerKey = RedisKeyConstant.following.getKey() + dinerInfo.getId();
        String dinerKey = RedisKeyConstant.following.getKey() + dinerId;
        //计算交集
        Set<Integer> followingDinerIds = redisTemplate.opsForSet().intersect(loginDinerKey, dinerKey);
        //没有共同好友
        if (CollectionUtil.isEmpty(followingDinerIds)) {
            return ResultInfoUtil.buildSuccess(path, new ArrayList<ShortDinerInfo>());
        }
        //根据ids查询食客信息
        ResultInfo resultInfo = restTemplate.getForObject(dinerServerName + "findByIds?access_token={accessToken}&ids={ids}",
                ResultInfo.class, accessToken, StrUtil.join(",", followingDinerIds));
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            resultInfo.setPath(path);
            return resultInfo;
        }
        //处理结果
        List<LinkedHashMap> dinerInfoMaps = (List<LinkedHashMap>) resultInfo.getData();
        List<ShortDinerInfo> dinerInfos = dinerInfoMaps.stream()
                .map(diner -> BeanUtil.fillBeanWithMap(diner, new ShortDinerInfo(), true))
                .collect(Collectors.toList());
        return ResultInfoUtil.buildSuccess(path, dinerInfos);
    }
}
