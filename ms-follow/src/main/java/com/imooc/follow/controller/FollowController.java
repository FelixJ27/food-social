package com.imooc.follow.controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.follow.service.FollowService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
public class FollowController {

    @Resource
    private FollowService followService;
    @Resource
    private HttpServletRequest request;

    /**
     * 关注/取关
     *
     * @param followDinnerId 关注的食客ID
     * @param isFollowed     是否关注 1-关注 0-取消
     * @param access_token   登录用户token
     * @return
     */
    @PostMapping("{followDinnerId}")
    public ResultInfo follow(@PathVariable Integer followDinnerId,
                             @RequestParam int isFollowed,
                             String access_token) {
        ResultInfo resultInfo = followService.follow(followDinnerId, isFollowed, access_token, request.getServletPath());
        return resultInfo;
    }

    /**
     * 共同好友
     *
     * @param dinerId      要查看的食客ID
     * @param access_token 登录用户token
     * @return
     */
    @GetMapping("commons/{dinerId}")
    public ResultInfo findCommonsFriends(@PathVariable Integer dinerId,
                                         String access_token) {
        ResultInfo resultInfo = followService.findCommonsFriends(dinerId, access_token, request.getServletPath());
        return resultInfo;
    }

    /**
     * 关注列表
     *
     * @param dinerId 要查看的食客id
     * @return
     */
    @GetMapping("followList/{dinerId}")
    public ResultInfo followList(@PathVariable Integer dinerId,
                                 String access_token) {
        ResultInfo resultInfo = followService.followList(dinerId, access_token, request.getServletPath());
        return resultInfo;
    }
}
