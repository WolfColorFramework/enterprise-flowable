package com.gaoy.flowable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gaoy.flowable.chat.DefaultViewNode;
import com.gaoy.flowable.core.WorkflowUser;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
import com.gaoy.flowable.domain.WorkflowStep;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WorkflowService extends IService<Workflow> {

    String start(WorkflowArgs args, String loginUserId);

    Boolean pass(WorkflowArgs args, String loginUserId);

    Boolean cancel(Workflow workflow);

    Boolean back(WorkflowArgs args);

    Boolean abort(Workflow workflow, WorkflowUser firstUser);

    Boolean obsolete(Workflow workflow);

    /**
     * 流程处理状态
     *
     * @param domainId
     * @return
     */
    List<WorkflowStep> workflowActionStatus(String domainId);

    /**
     * 最后一步状态
     *
     * @param domainId
     * @return
     */
    String lastStepStatus(String domainId);

    WorkflowStep unActionStep(Workflow workflow, String loginUserId);

    WorkflowStep unActionStep(String domainId, String loginUserId);

    /**
     * 获取有效的流程步骤
     *
     * @param workflowId 工作流Id
     * @return
     */
    List<WorkflowStep> effectiveSteps(String workflowId);

    /**
     * 画图
     *
     * @param workflowId 工作流Id
     * @return
     */
    DefaultViewNode chartWorkflow(String workflowId);
}
