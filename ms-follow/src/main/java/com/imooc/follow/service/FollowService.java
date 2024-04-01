package com.imooc.follow.service;

import cn.hutool.core.bean.BeanUtil;
import com.imooc.commons.constant.ApiConstant;
import com.imooc.commons.constant.RedisKeyConstant;
import com.imooc.commons.exception.ParameterException;
import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.pojo.Follow;
import com.imooc.commons.model.vo.SignInDinerInfo;
import com.imooc.commons.utils.AssertUtil;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.follow.mapper.FollowMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;

@Service
public class FollowService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private FollowMapper followMapper;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RestTemplate restTemplate;


    public ResultInfo follow(Integer followDinnerId, int isFollowed, String accessToken, String path) {
        AssertUtil.isTrue(followDinnerId == null || followDinnerId < 1, "请选择要关注的人");
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        Follow follow = followMapper.selectFollow(dinerInfo.getId(), followDinnerId);
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

}
