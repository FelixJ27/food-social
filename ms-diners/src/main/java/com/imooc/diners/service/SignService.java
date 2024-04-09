package com.imooc.diners.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.StrUtil;
import com.imooc.commons.constant.ApiConstant;
import com.imooc.commons.exception.ParameterException;
import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.vo.SignInDinerInfo;
import com.imooc.commons.utils.AssertUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class SignService {

    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private RestTemplate restTemplate;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户签到
     *
     * @param accessToken 登录用户 token
     * @param dateStr     查询的日期，默认当天 yyyy-MM-dd
     * @return 连续签到次数
     */
    public int doSign(String accessToken, String dateStr) {
        // 获取登录用户信息
        SignInDinerInfo signInDinerInfo = loadSignInDinerInfo(accessToken);
        // 获取日期
        Date date = getDate(dateStr);
        // 获取日期对应的天数，多少号
        int offset = DateUtil.dayOfMonth(date) - 1; // 从 0 开始
        // 构建 Key
        String signKey = buildSignKey(signInDinerInfo.getId(), date);
        // 查看是否已签到
        boolean isSigned = redisTemplate.opsForValue().getBit(signKey, offset);
        AssertUtil.isTrue(isSigned, "当前日期已完成签到，无需再签");
        // 签到
        redisTemplate.opsForValue().setBit(signKey, offset, true);
        // 统计连续签到次数
        int count = getContinuousSignCount(signInDinerInfo.getId(), date);
        return count;
    }

    /**
     * 统计某月连续签到次数
     *
     * @param dinerId 用户ID
     * @param date    日期
     * @return 当月连续签到次数
     */
    private int getContinuousSignCount(Integer dinerId, Date date) {
        // 获取日期对应的天数，多少号
        int dayOfMonth = DateUtil.dayOfMonth(date);
        // 构建 Key
        String signKey = buildSignKey(dinerId, date);
        // 命令：bitfield key get [u/i]offset value
        // 此命令就是get取出key对应的位图，指定value索引位开始，取offset位偏移量的二进制
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0);
        List<Long> list = redisTemplate.opsForValue().bitField(signKey, bitFieldSubCommands);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        int signCount = 0;
        long v = list.get(0) == null ? 0 : list.get(0);
        // 取低位连续不为0的个数即为连续签到次数，需考虑当天尚未签到的情况
        for (int i = dayOfMonth; i > 0; i--) {// i 表示位移次数
            // 右移再左移，如果等于自己说明最低位是 0，表示未签到
            if (v >> 1 << 1 == v) {
                // 低位为 0 且非当天说明连续签到中断了
                if (i != dayOfMonth) break;
            } else {
                // 签到了 签到数加1
                signCount += 1;
            }
            // 右移一位并重新赋值，相当于把最右边一位去除
            v >>= 1;
        }
        return signCount;
    }

    /**
     * 构建key
     *
     * @param dinerId
     * @param date
     * @return
     */
    private String buildSignKey(Integer dinerId, Date date) {
        return String.format("user:sign:%d:%s",
                dinerId, DateUtil.format(date, "yyyyMM"));
    }

    /**
     * 获取日期
     *
     * @param dateStr yyyy-MM-dd 默认当天
     * @return
     */
    private static Date getDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return new Date();
        }
        try {
            return DateUtil.parseDate(dateStr);
        } catch (Exception e) {
            throw new ParameterException("请传入yyyy-MM-dd的日期格式");
        }
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
     * 获取用户签到次数
     *
     * @param accessToken
     * @param dateSer
     * @return
     */
    public Long getSignCount(String accessToken, String dateSer) {
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        Date date = getDate(dateSer);
        String key = buildSignKey(dinerInfo.getId(), date);
        return (Long) redisTemplate.execute((RedisCallback<Long>) con -> con.bitCount(key.getBytes()));
    }

    /**
     * 获取用户签到信息
     *
     * @param accessToken
     * @param dateStr
     * @return
     */
    public Map<String, Boolean> getSignInfo(String accessToken, String dateStr) {
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        Date date = getDate(dateStr);
        String key = buildSignKey(dinerInfo.getId(), date);
        Map<String, Boolean> signInfo = new HashMap<>();
        int dayOfMonth = DateUtil.lengthOfMonth(DateUtil.month(date) + 1,
                DateUtil.isLeapYear(DateUtil.year(date)));
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0);
        List<Long> list = redisTemplate.opsForValue().bitField(key, bitFieldSubCommands);
        if (CollectionUtil.isEmpty(list)) {
            return signInfo;
        }
        long v = list.get(0) == null ? 0 : list.get(0);
        for (int i = dayOfMonth; i > 0; i--) {
            LocalDateTime localDateTime = LocalDateTimeUtil.of(date).withDayOfMonth(i);
            boolean flag = v >> 1 << 1 == v;
            signInfo.put(DateUtil.format(localDateTime, "yyyy-MM-dd"), flag);
            v >>= 1;
        }
        return signInfo;
    }
}
