package com.imooc.diners.controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.diners.service.SignService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController()
@RequestMapping("sign")
public class SignController {

    @Resource
    private HttpServletRequest request;
    @Resource
    private SignService signService;

    /**
     * 签到，可以补签
     *
     * @param access_token
     * @param date         某个日期 yyyy-MM-dd 默认当天
     * @return
     */
    @PostMapping
    public ResultInfo<Integer> sign(String access_token,
                                    @RequestParam(required = false) String date) {

        int count = signService.doSign(access_token, date);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), count);
    }

    /**
     * 获取签到次数 默认当月
     *
     * @param access_token
     * @param dateStr      某个日期 yyyy-MM-dd
     * @return
     */
    @GetMapping("count")
    public ResultInfo<Long> getSignCount(String access_token, String dateStr) {
        Long count = signService.getSignCount(access_token, dateStr);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), count);
    }

    /**
     * 获取用户签到情况
     */
    @GetMapping()
    public ResultInfo<Map<String, Boolean>> getSignInfo(String access_token, String dateStr) {
        Map<String, Boolean> signInfo = signService.getSignInfo(access_token, dateStr);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), signInfo);
    }

}
