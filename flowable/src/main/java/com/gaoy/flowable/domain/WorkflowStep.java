package com.gaoy.flowable.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("f_workflow_step")
@Accessors(chain = true)
@Data
public class WorkflowStep extends Model<WorkflowStep> {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private String id;

    /**
     * 流程id
     */
    @TableField("workflow_id")
    private String workflowId;

    /**
     * 模块Id
     */
    @TableField("domain_id")
    private String domainId;

    /**
     * 模块名称
     */
    @TableField("domain_name")
    private String domainName;

    /**
     * node外键
     */
    @TableField("node_id")
    private String nodeId;

    /**
     * 节点名称
     */
    @TableField("node_name")
    private String nodeName;

    /**
     * 处理人id
     */
    @TableField("action_user_id")
    private String actionUserId;

    /**
     * 处理人名称
     */
    @TableField("action_user_name")
    private String actionUserName;

    /**
     * 上一步id
     */
    @TableField("pre_step_id")
    private String preStepId;

    /**
     * 处理意见
     */
    private String suggest;

    /**
     * 处理结果（ActionStatus枚举）
     */
    private Integer result;

    /**
     * 结果描述(显示result的字符串信息)
     */
    @TableField(exist = false)
    private String resultDescription;

    /**
     * 文件路径
     */
    @TableField("file_url")
    private String fileUrl;

    /**
     * 文件描述
     */
    @TableField("file_description")
    private String fileDescription;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("action_time")
    private LocalDateTime actionTime;

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

    /**
     * 节点索引
     */
    @TableField("node_index")
    private Integer nodeIndex;

    @Override
    protected Serializable pkVal() {
        return this.id;
    }

}
