package com.imooc.feeds.mapper;

import com.imooc.commons.model.pojo.Feeds;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FeedsMapper {

    // 添加 Feed
    @Insert("insert into t_feeds (content, fk_diner_id, praise_amount, " +
            " comment_amount, fk_restaurant_id, create_date, update_date, is_valid) " +
            " values (#{content}, #{fkDinerId}, #{praiseAmount}, #{commentAmount}, #{fkRestaurantId}, " +
            " now(), now(), 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(Feeds feeds);

    // 根据食客 ID 查询 Feed
    @Select("select id, content, update_date from t_feeds " +
            " where fk_diner_id = #{dinerId} and is_valid = 1")
    List<Feeds> findByDinerId(@Param("dinerId") Integer dinerId);

}