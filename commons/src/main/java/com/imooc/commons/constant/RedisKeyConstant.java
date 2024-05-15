package com.imooc.commons.constant;

import lombok.Getter;

@Getter
public enum RedisKeyConstant {

    verify_code("verify_code:", "验证码"),
    seckill_vouchers("seckill_vouchers:", "秒杀券的key"),
    lock_key("lockby:", "分布式锁的key"),
    following("following:", "关注集合Key"),
    followers("followers:", "粉丝集合Key"),
    following_feeds("following_feeds:", "我关注的好友的FeedsKey"),
    diner_points("diner:points", "diner用户的积分key"),
    diner_location("diner:location", "diner地理位置信息"),
    restaurants("restaurants:", "餐厅的key"),
    ;

    private String key;
    private String desc;

    RedisKeyConstant(String key, String desc) {
        this.key = key;
        this.desc = desc;
    }

}