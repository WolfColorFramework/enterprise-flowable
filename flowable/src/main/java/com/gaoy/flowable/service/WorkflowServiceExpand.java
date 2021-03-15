package com.gaoy.flowable.service;

import com.gaoy.flowable.chat.DefaultViewNode;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowStep;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WorkflowServiceExpand {

    /**
     * 流程处理状态
     *
     * @param workflow 工作流
     * @return
     */
    List<WorkflowStep> workflowActionStatus(Workflow workflow);

    /**
     * 流程当前所属状态
     *
     * @param workflow 工作流
     * @return
     */
    String workflowCurrentStatus(Workflow workflow);

    /**
     * 获取有效的流程步骤
     *
     * @param workflow 工作流
     * @return
     */
    List<WorkflowStep> effectiveSteps(Workflow workflow);

    /**
     * 画图
     *
     * @param workflow 工作流
     * @return
     */
    DefaultViewNode chartWorkflow(Workflow workflow);
}
