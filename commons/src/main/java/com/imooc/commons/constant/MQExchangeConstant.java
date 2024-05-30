package com.imooc.commons.constant;

import lombok.Getter;

@Getter
public enum MQExchangeConstant {

    FEEDS_FOLLOW("exchange.feeds.follow", "feeds流粉丝列表"),
    ;

    private String exchange;
    private String desc;

    MQExchangeConstant(String exchange, String desc) {
        this.exchange = exchange;
        this.desc = desc;
    }
}
