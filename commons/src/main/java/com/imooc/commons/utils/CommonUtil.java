package com.imooc.commons.utils;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.imooc.commons.exception.ParameterException;

import java.util.Date;

public class CommonUtil {

    /**
     * 获取日期
     *
     * @param dateStr yyyy-MM-dd 默认当天
     * @return
     */
    public static Date getDate(String dateStr) {
        if (StrUtil.isBlank(dateStr)) {
            return new Date();
        }
        try {
            return DateUtil.parseDate(dateStr);
        } catch (Exception e) {
            throw new ParameterException("请传入yyyy-MM-dd的日期格式");
        }
    }
}
