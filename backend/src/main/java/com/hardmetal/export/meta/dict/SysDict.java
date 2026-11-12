package com.hardmetal.export.meta.dict;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 枚举字典。sys_dict
 * <p>{@code (dict_type, dict_key)} 唯一；中文标签以本表为唯一来源，接口由
 * {@code GET /api/v1/meta/dict/{type}} 对外提供。</p>
 */
@Data
@TableName("sys_dict")
public class SysDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String dictType;

    private String dictKey;

    private String dictLabel;

    private Integer sortNo;

    private String remark;

    @TableLogic
    private Boolean isDeleted;
}