package com.imooc.commons.constant;

import lombok.Getter;

@Getter
public enum MQQueueConstant {

    FEEDS_FOLLOW("queue.feeds.follow", "feeds流粉丝列表"),
    ;

    private String queue;
    private String desc;


    MQQueueConstant(String queue, String desc) {
        this.queue = queue;
        this.desc = desc;
    }
}
