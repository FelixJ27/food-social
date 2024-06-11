package com.imooc.follow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.commons.constant.RedisKeyConstant;
import com.imooc.commons.model.dto.FeedsDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
public class FollowMessageService {

    @Resource
    private FollowService followService;
    @Resource
    private RedisTemplate redisTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "queue.feeds"),
            exchange = @Exchange(name = "exchange.feeds.follow", type = ExchangeTypes.DIRECT),
            key = "key.follow"
    ))
    public void handleMessage(byte[] messageBody) throws IOException {
        FeedsDTO feedsDTO = objectMapper.readValue(messageBody, FeedsDTO.class);
        // 先获取我的粉丝
        Set<Integer> followers = followService.findFollowers(feedsDTO.getSignInDinerId());

        // 推送 Feeds，以时间作为分数存储至 ZSet
        long now = System.currentTimeMillis();
        followers.forEach(follower -> {
            String key = RedisKeyConstant.following_feeds.getKey() + follower;
            redisTemplate.opsForZSet().add(key, feedsDTO.getId(), now);
        });
    }
}
