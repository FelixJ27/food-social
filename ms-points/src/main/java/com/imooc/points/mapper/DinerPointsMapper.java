package com.imooc.points.mapper;

import com.imooc.commons.model.pojo.DinerPoints;
import org.apache.ibatis.annotations.Insert;

/**
 * 积分 Mapper
 */
public interface DinerPointsMapper {

    // 添加积分
    @Insert("insert into t_diner_points (fk_diner_id, points, types, is_valid, create_date, update_date) " +
            " values (#{fkDinerId}, #{points}, #{types}, 1, now(), now())")
    void save(DinerPoints dinerPoints);

}
