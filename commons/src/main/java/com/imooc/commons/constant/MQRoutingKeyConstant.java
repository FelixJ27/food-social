package com.imooc.commons.constant;

import lombok.Getter;

@Getter
public enum MQRoutingKeyConstant {

    FEEDS_KEY("key.feeds", "feeds流路由键"),
    ORDER_KEY("key.order", "订单路由键"),
    ;

    private String key;
    private String desc;

    MQRoutingKeyConstant(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }
}
