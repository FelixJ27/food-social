package com.imooc.diners.controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.dto.DinersDTO;
import com.imooc.commons.model.vo.ShortDinerInfo;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.diners.service.DinersService;
import com.imooc.diners.service.SignService;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 食客服务控制层
 */
@RestController
@Api(tags = "食客相关接口")
public class DinersController {

    @Resource
    private DinersService dinersService;
    @Resource
    private HttpServletRequest request;

    /**
     * 注册
     *
     * @param dinersDTO
     * @return
     */
    @PostMapping("register")
    public ResultInfo register(@RequestBody DinersDTO dinersDTO) {
        return dinersService.register(dinersDTO, request.getServletPath());
    }

    /**
     * 校验手机号是否已注册
     *
     * @param phone
     * @return
     */
    @GetMapping("checkPhone")
    public ResultInfo checkPhone(String phone) {
        dinersService.checkPhoneIsRegistered(phone);
        return ResultInfoUtil.buildSuccess(request.getServletPath());
    }

    /**
     * 登录
     *
     * @param account
     * @param password
     * @return
     */
    @GetMapping("signin")
    public ResultInfo signIn(String account, String password) {
        return dinersService.signIn(account, password, request.getServletPath());
    }

    /**
     * 根据 ids 查询食客信息
     *
     * @param ids
     * @return
     */
    @GetMapping("findByIds")
    public ResultInfo<List<ShortDinerInfo>> findByIds(String ids) {
        List<ShortDinerInfo> dinerInfos = dinersService.findByIds(ids);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), dinerInfos);
    }

    @GetMapping("getLoginInfo")
    public ResultInfo<Map<String, Boolean>> getLoginInfo(String access_token, String dateStr) {
        Map<String, Boolean> loginInfo = dinersService.getLoginInfo(access_token, dateStr);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), loginInfo);
    }

}
