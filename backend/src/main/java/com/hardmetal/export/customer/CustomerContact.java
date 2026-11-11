package com.hardmetal.export.customer;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 客户联系人。customer_contact
 */
@Data
@TableName("customer_contact")
public class CustomerContact {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long customerId;
    private String name;
    private String position;
    private String email;
    private String phone;
    private String imType;
    private String imAccount;
    private String language;
    private Boolean isDecisionMaker;
    @TableLogic
    private Boolean isDeleted;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}