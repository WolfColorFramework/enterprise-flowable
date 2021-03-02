package com.gaoy.flowable.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("f_template")
public class Template extends Model<Template> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 名称
     */
    private String name;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建人Id
     */
    @TableField("user_id")
    private String userId;

    /**
     * 发布状态（false：私有；true：公有）
     */
    @TableField("publish_status")
    private Boolean publishStatus;

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
