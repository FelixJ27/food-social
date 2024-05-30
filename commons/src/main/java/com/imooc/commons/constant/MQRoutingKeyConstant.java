package com.imooc.commons.constant;

import lombok.Getter;

@Getter
public enum MQRoutingKeyConstant {

    FEEDS_FOLLOW("key.feeds", "feeds流路由键"),
    ;

    private String key;
    private String desc;

    MQRoutingKeyConstant(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }
}
