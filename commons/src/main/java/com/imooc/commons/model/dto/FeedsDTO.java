package com.imooc.commons.model.dto;

import com.imooc.commons.model.base.BaseModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class FeedsDTO extends BaseModel {
    @ApiModelProperty("内容")
    private String content;
    @ApiModelProperty("食客")
    private Integer fkDinerId;
    @ApiModelProperty("点赞")
    private int praiseAmount;
    @ApiModelProperty("评论")
    private int commentAmount;
    @ApiModelProperty("关联的餐厅")
    private Integer fkRestaurantId;
    @ApiModelProperty("登录用户id")
    private Integer signInDinerId;
}
