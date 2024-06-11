package com.imooc.commons.constant;

import lombok.Getter;

@Getter
public enum MQQueueConstant {

    FEEDS("queue.feeds", "feeds队列"),
    ORDER("queue.order", "订单队列")
    ;

    private String queue;
    private String desc;


    MQQueueConstant(String queue, String desc) {
        this.queue = queue;
        this.desc = desc;
    }
}
