package com.gaoy.flowable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
import com.gaoy.flowable.domain.WorkflowStep;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WorkflowStepService extends IService<WorkflowStep> {

    /**
     * 通过
     *
     * @param step 流程步
     * @param args 流程参数
     * @return
     */
    List<WorkflowStep> pass(WorkflowStep step, WorkflowArgs args);

    /**
     * 退回
     * @param step 流程步
     * @param backFirst 是否退回到开始节点
     * @param args 参数
     * @return
     */
    List<WorkflowStep> back(WorkflowStep step, Boolean backFirst, WorkflowArgs args);

    /**
     * 用户未处理流程步
     *
     * @param workflow 流程
     * @param loginUserId 登录用户Id
     * @return
     */
    WorkflowStep unActionStep(Workflow workflow, String loginUserId);

    /**
     * 流程的所有步骤
     *
     * @param workflow 流程
     * @return
     */
    List<WorkflowStep> stepsOfWorkflow(Workflow workflow);

    /**
     * 创建流程第一个步骤
     * @param workFlow
     * @return
     */
    WorkflowStep createFirstStep(Workflow workFlow);

}
