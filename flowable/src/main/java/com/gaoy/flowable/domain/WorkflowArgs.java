package com.gaoy.flowable.domain;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowArgs {

    /**
     * 处理意见
     */
    private String option;

    private String fileUrl;

    private String fileDescription;

    /**
     * 表达式的值
     */
    private Object expressionValue;

    /**
     * 下一步审批人集合
     */
    private List<String> approveUserIds;

    /**
     * 发起人Id
     */
    private String applyUserId;

    /**
     * 模板Id
     */
    private String templateId;

    private String domainName;

    private String domainId;

    private String title;

    /**
     * 额外的数据
     */
    private Object data;

}
