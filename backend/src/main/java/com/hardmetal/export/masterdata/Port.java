package com.hardmetal.export.masterdata;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 港口/机场/陆路口岸。UN/LOCODE 为业务主键。
 * <p>类型取值与 DDL CHECK 一致：{@code seaport / airport / landport}（契约里旧的 dryport 已废弃）。
 */
@Data
@TableName("port")
public class Port {

    @TableId(value = "code", type = IdType.INPUT)
    private String code;

    private String nameCn;
    private String nameEn;
    private String countryCode;

    /** seaport / airport / landport */
    private String portType;

    private String route;

    @TableLogic
    private Boolean isDeleted;
}