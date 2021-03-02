package com.gaoy.flowable.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@TableName("f_workflow")
public class Workflow extends Model<Workflow> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 工作流描述
     */
    private String title;

    /**
     * 创建人id
     */
    @TableField("user_id")
    private String userId;

    /**
     * 创建人名称
     */
    @TableField("user_name")
    private String userName;

    /**
     * 模块id
     */
    @TableField("domain_id")
    private String domainId;

    /**
     * 模块name
     */
    @TableField("domain_name")
    private String domainName;

    /**
     * 模板id
     */
    @TableField("template_id")
    private String templateId;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 删除时间
     */
    @TableField("delete_time")
    private LocalDateTime deleteTime;

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}
