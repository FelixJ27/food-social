package com.imooc.diners.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.imooc.commons.constant.ApiConstant;
import com.imooc.commons.constant.RedisKeyConstant;
import com.imooc.commons.exception.ParameterException;
import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.vo.NearMeDinerVO;
import com.imooc.commons.model.vo.ShortDinerInfo;
import com.imooc.commons.model.vo.SignInDinerInfo;
import com.imooc.commons.utils.AssertUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class NearMeService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private DinersService dinersService;


    /**
     * 更新食客坐标
     *
     * @param accessToken
     * @param lon
     * @param lat
     */
    public void updateDinerLocation(String accessToken, Float lon, Float lat) {
        AssertUtil.isTrue(lon == null, "获取经度失败");
        AssertUtil.isTrue(lat == null, "获取纬度失败");
        SignInDinerInfo signInDinerInfo = loadSignInDinerInfo(accessToken);
        RedisGeoCommands.GeoLocation geoLocation =
                new RedisGeoCommands.GeoLocation(signInDinerInfo.getId(), new Point(lon, lat));
        redisTemplate.opsForGeo().add(RedisKeyConstant.diner_location.getKey(),
                geoLocation);
    }

    /**
     * 获取附近的人
     *
     * @param accessToken 登录 token
     * @param radius      半径(m)，默认1000m
     * @param lon         经度
     * @param lat         纬度
     * @return
     */
    public List<NearMeDinerVO> findNearMe(String accessToken,
                                          Integer radius,
                                          Float lon, Float lat) {
        // 获取登录用户信息
        SignInDinerInfo signInDinerInfo = loadSignInDinerInfo(accessToken);
        // 食客 ID
        Integer dinerId = signInDinerInfo.getId();
        // 查询半径，默认 1000m
        if (radius == null) {
            radius = 1000;
        }
        // 获取 Key diner:location
        String key = RedisKeyConstant.diner_location.getKey();
        // 获取用户经纬度
        Point point = null;
        if (lon == null || lat == null) {
            // 如果经纬度没传，那么从 Redis 中获取，但客户端传入会比较精确
            List<Point> points = redisTemplate.opsForGeo().position(key, dinerId);
            AssertUtil.isTrue(points == null || points.isEmpty(), "获取经纬度失败！");
            point = points.get(0);
        } else {
            point = new Point(lon, lat);
        }
        // 初始化距离对象，单位 m
        Distance distance = new Distance(radius, RedisGeoCommands.DistanceUnit.METERS);
        // 初始化 Geo 命令参数对象
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs();
        // 附近的人限制 20，包含距离，按由近及远排序
        args.limit(20).includeDistance().sortAscending();
        // 以用户经纬度为圆心，范围 1000m
        Circle circle = new Circle(point, distance);
        // 获取附近的人 GeoLocation 信息
        GeoResults<RedisGeoCommands.GeoLocation<Integer>> results =
                redisTemplate.opsForGeo().radius(key, circle, args);
        // 构建有序 Map
        Map<Integer, NearMeDinerVO> nearMeDinerVOMap = Maps.newLinkedHashMap();
        // 循环处理距离信息
        results.forEach(result -> {
            // 获取 locationName 也就是食客 ID
            RedisGeoCommands.GeoLocation<Integer> location = result.getContent();
            NearMeDinerVO nearMeDinerVO = new NearMeDinerVO();
            nearMeDinerVO.setId(location.getName());
            // 格式化距离
            Double dist = result.getDistance().getValue();
            // 四舍五入精确到小数点 1 位，为了方便客户端显示
            // 这里后期需要扩展处理，根据距离显示 m km
            String distanceStr = NumberUtil.round(dist, 1).toString() + "m";
            nearMeDinerVO.setDistance(distanceStr);
            nearMeDinerVOMap.put(location.getName(), nearMeDinerVO);
        });
        // 获取附近的人的信息
        Integer[] dinerIds = nearMeDinerVOMap.keySet().toArray(new Integer[]{});
        List<ShortDinerInfo> shortDinerInfos = dinersService.findByIds(StrUtil.join(",", dinerIds));
        // 完善昵称头像信息
        shortDinerInfos.forEach(shortDinerInfo -> {
            NearMeDinerVO nearMeDinerVO = nearMeDinerVOMap.get(shortDinerInfo.getId());
            nearMeDinerVO.setNickname(shortDinerInfo.getNickname());
            nearMeDinerVO.setAvatarUrl(shortDinerInfo.getAvatarUrl());
        });
        return Lists.newArrayList(nearMeDinerVOMap.values());
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
}
