package com.imooc.diners.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.imooc.commons.constant.ApiConstant;
import com.imooc.commons.exception.ParameterException;
import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.dto.DinersDTO;
import com.imooc.commons.model.pojo.Diners;
import com.imooc.commons.model.vo.ShortDinerInfo;
import com.imooc.commons.model.vo.SignInDinerInfo;
import com.imooc.commons.utils.AssertUtil;
import com.imooc.commons.utils.CommonUtil;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.diners.config.OAuth2ClientConfiguration;
import com.imooc.diners.domain.OAuthDinerInfo;
import com.imooc.diners.mapper.DinersMapper;
import com.imooc.diners.vo.LoginDinerInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.chrono.IsoChronology;
import java.util.*;

/**
 * 食客服务业务逻辑层
 */
@Service
public class DinersService {

    @Resource
    private RestTemplate restTemplate;
    @Value("${service.name.ms-oauth-server}")
    private String oauthServerName;
    @Resource
    private OAuth2ClientConfiguration oAuth2ClientConfiguration;
    @Resource
    private DinersMapper dinersMapper;
    @Resource
    private SendVerifyCodeService sendVerifyCodeService;
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 用户注册
     *
     * @param dinersDTO
     * @param path
     * @return
     */
    public ResultInfo register(DinersDTO dinersDTO, String path) {
        // 参数非空校验
        String username = dinersDTO.getUsername();
        AssertUtil.isNotEmpty(username, "请输入用户名");
        String password = dinersDTO.getPassword();
        AssertUtil.isNotEmpty(password, "请输入密码");
        String phone = dinersDTO.getPhone();
        AssertUtil.isNotEmpty(phone, "请输入手机号");
        String verifyCode = dinersDTO.getVerifyCode();
        AssertUtil.isNotEmpty(verifyCode, "请输入验证码");
        // 获取验证码
        String code = sendVerifyCodeService.getCodeByPhone(phone);
        // 验证是否过期
        AssertUtil.isNotEmpty(code, "验证码已过期，请重新发送");
        // 验证码一致性校验
        AssertUtil.isTrue(!dinersDTO.getVerifyCode().equals(code), "验证码不一致，请重新输入");
        // 验证用户名是否已注册
        Diners diners = dinersMapper.selectByUsername(username.trim());
        AssertUtil.isTrue(diners != null, "用户名已存在，请重新输入");
        // 注册
        // 密码加密
        dinersDTO.setPassword(DigestUtil.md5Hex(password.trim()));
        dinersMapper.save(dinersDTO);
        // 自动登录
        return signIn(username.trim(), password.trim(), path);
    }

    /**
     * 校验手机号是否已注册
     */
    public void checkPhoneIsRegistered(String phone) {
        AssertUtil.isNotEmpty(phone, "手机号不能为空");
        Diners diners = dinersMapper.selectByPhone(phone);
        AssertUtil.isTrue(diners == null, "该手机号未注册");
        AssertUtil.isTrue(diners.getIsValid() == 0, "该用户已锁定，请先解锁");
    }

    /**
     * 登录
     *
     * @param account  帐号：用户名或手机或邮箱
     * @param password 密码
     * @param path     请求路径
     * @return
     */
    public ResultInfo signIn(String account, String password, String path) {
        //校验参数
        AssertUtil.isNotEmpty(account, "请输入账号");
        AssertUtil.isNotEmpty(password, "请输入密码");
        //构建请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        //构建请求体
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("username", account);
        body.add("password", password);
        body.setAll(BeanUtil.beanToMap(oAuth2ClientConfiguration));
        HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);
        //设置authorization
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(oAuth2ClientConfiguration.getClientId(),
                oAuth2ClientConfiguration.getSecret()));
        //发送请求
        ResponseEntity<ResultInfo> result = restTemplate.postForEntity(oauthServerName + "oauth/token", entity, ResultInfo.class);
        //处理返回结果
        AssertUtil.isTrue(result.getStatusCode() != HttpStatus.OK, "登录失败");
        ResultInfo resultInfo = result.getBody();
        if (resultInfo.getCode() != ApiConstant.SUCCESS_CODE) {
            resultInfo.setData(resultInfo.getMessage());
            return resultInfo;
        }
        //这里的data是一个LinkedHashMap转成了域对象OAuthDinerInfo
        OAuthDinerInfo dinerInfo = BeanUtil.fillBeanWithMap((LinkedHashMap) resultInfo.getData(),
                new OAuthDinerInfo(), false);
        //根据业务需求返回视图对象
        LoginDinerInfo loginDinerInfo = new LoginDinerInfo();
        loginDinerInfo.setToken(dinerInfo.getAccessToken());
        loginDinerInfo.setAvatarUrl(dinerInfo.getAvatarUrl());
        loginDinerInfo.setNickname(dinerInfo.getNickname());
        Date date = new Date();
        Diners diner = dinersMapper.selectByUsername(account);
        long offset = DateUtil.dayOfMonth(date) - 1;
        String key = buildLoginKey(diner.getId(), date);
        redisTemplate.opsForValue().setBit(key, offset, true);
        return ResultInfoUtil.buildSuccess(path, loginDinerInfo);
    }

    private String buildLoginKey(Integer dinerId, Date date) {
        return String.format("user:login:%d:%s",
                dinerId, DateUtil.format(date, "yyyyMM"));
    }

    /**
     * 根据 ids 查询食客信息
     *
     * @param ids 主键 id，多个以逗号分隔，逗号之间不用空格
     * @return
     */
    public List<ShortDinerInfo> findByIds(String ids) {
        AssertUtil.isNotEmpty(ids);
        String[] idArr = ids.split(",");
        List<ShortDinerInfo> dinerInfos = dinersMapper.findByIds(idArr);
        return dinerInfos;
    }


    /**
     * 获取用户登录情况
     *
     * @param accessToken
     * @param dateStr
     * @return
     */
    public Map<String, Boolean> getLoginInfo(String accessToken, String dateStr) {
        SignInDinerInfo dinerInfo = loadSignInDinerInfo(accessToken);
        Date date = CommonUtil.getDate(dateStr);
        String loginKey = buildLoginKey(dinerInfo.getId(), date);
        int dayOfMonth = DateUtil.lengthOfMonth(DateUtil.month(date) + 1,
                DateUtil.isLeapYear(DateUtil.year(date)));
        BitFieldSubCommands bitFieldSubCommands = BitFieldSubCommands.create()
                .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0);
        List<Long> list = redisTemplate.opsForValue().bitField(loginKey, bitFieldSubCommands);
        Map<String, Boolean> loginInfo = new HashMap<>();
        if (CollectionUtil.isEmpty(list)) {
            return loginInfo;
        }
        Long v = list.get(0) == null ? 0 : list.get(0);
        for (int i = dayOfMonth; i > 0; i--) {
            boolean flag = v >> 1 << 1 != v;
            LocalDateTime dateTime = LocalDateTimeUtil.of(date).withDayOfMonth(i);
            loginInfo.put(DateUtil.format(dateTime, "yyyy-MM-dd"), flag);
            v >>= 1;
        }
        return loginInfo;
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
