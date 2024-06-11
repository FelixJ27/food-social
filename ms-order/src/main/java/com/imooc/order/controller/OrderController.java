package com.imooc.order.controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.order.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class OrderController {
    @Resource
    private HttpServletRequest request;
    @Resource
    private OrderService orderService;

    @GetMapping("test")
    public ResultInfo test() {
        orderService.test();
        return ResultInfoUtil.buildSuccess("发送成功", request.getServletPath());
    }
}
