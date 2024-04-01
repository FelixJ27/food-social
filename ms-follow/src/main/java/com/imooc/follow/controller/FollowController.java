package com.imooc.follow.controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.follow.service.FollowService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class FollowController {

    @Resource
    private FollowService followService;
    @Resource
    private HttpServletRequest request;

    @PostMapping
    public ResultInfo follow(@PathVariable Integer followDinnerId,
                             @RequestParam int isFollowed,
                             String access_token) {
        return followService.follow(followDinnerId, isFollowed, access_token, request.getServletPath());
    }
}
