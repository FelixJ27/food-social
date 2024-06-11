package com.imooc.commons.constant;

import lombok.Getter;

@Getter
public enum MQExchangeConstant {

    FEEDS_FOLLOW("exchange.feeds.follow", "关注微服务发送来feeds的交换机"),
    ORDER_RESTAURANT("exchange.order.restaurant", "餐厅微服务发送来订单微服务的交换机"),
    ORDER_DELIVERYMAN("exchange.order.deliveryman", "外卖骑手服务发送到订单微服务的交换机"),
    ORDER_SETTLEMENT("exchange.order.settlement", "结算微服务发送到订单微服务的交换机"),
    ;

    private String exchange;
    private String desc;

    MQExchangeConstant(String exchange, String desc) {
        this.exchange = exchange;
        this.desc = desc;
    }
}
