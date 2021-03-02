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
@TableName("f_node")
public class Node extends Model<Node> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    @TableField("template_id")
    private String templateId;

    /**
     * 节点顺序（从0开始）
     */
    @TableField("order_index")
    private Integer orderIndex;

    /**
     * 节点名称
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 多人审批（false：单人审批；true：多人审批）
     */
    @TableField("multiple_approve")
    private Boolean multipleApprove;

    /**
     * 是否是退回节点（false：非退回节点；true：退回节点）
     */
    @TableField("back_node")
    private Boolean backNode;

    /**
     * 表达式
     */
    private String expression;

    /**
     * 表达式描述
     */
    @TableField("expression_description")
    private String expressionDescription;

    /**
     * 可以退回（true：可以；false：不可以）
     */
    @TableField("enable_back")
    private Boolean enableBack;

    /**
     * 跳跃标记
     */
    private Boolean jump;

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
