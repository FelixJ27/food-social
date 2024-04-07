package com.imooc.feeds.controller;

import com.imooc.commons.model.domain.ResultInfo;
import com.imooc.commons.model.pojo.Feeds;
import com.imooc.commons.model.vo.FeedsVO;
import com.imooc.commons.utils.ResultInfoUtil;
import com.imooc.feeds.service.FeedsService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class FeedsController {

    @Resource
    private FeedsService feedsService;
    @Resource
    private HttpServletRequest request;

    /**
     * 添加 Feed
     *
     * @param feeds
     * @param access_token
     * @return
     */
    @PostMapping
    public ResultInfo<String> create(@RequestBody Feeds feeds, String access_token) {
        feedsService.create(feeds, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "添加成功");
    }

    /**
     * 变更 Feed
     *
     * @param followingDinerId 关注的好友的 ID
     * @param access_token     登录用户token
     * @param type             1 关注 0 取关
     * @return
     */
    @PostMapping("updateFollowingFeeds/{followingDinerId}")
    public ResultInfo<String> addFollowingFeeds(@PathVariable Integer followingDinerId,
                                                String access_token, @RequestParam int type) {
        feedsService.addFollowingFeeds(followingDinerId, access_token, type);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), "操作成功");
    }

    /**
     * 分页获取关注的 Feed 数据
     *
     * @param page
     * @return
     */
    @GetMapping("{page}")
    public ResultInfo selectForPage(@PathVariable Integer page, String access_token) {
        List<FeedsVO> feedsVOS = feedsService.selectForPage(page, access_token);
        return ResultInfoUtil.buildSuccess(request.getServletPath(), feedsVOS);
    }


}