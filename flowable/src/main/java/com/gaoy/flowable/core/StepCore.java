package com.gaoy.flowable.core;

import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowStep;

import java.util.List;

public interface StepCore {
    /**
     * 创建下一个流程步
     * @param step 步骤
     * @param node 节点
     * @param actionUsers 处理人集合
     * @param expressionValue 表达式值
     * @return
     */
    List<WorkflowStep> createNextStep(WorkflowStep step, Node node, List<WorkflowUser> actionUsers, Object expressionValue);

    /**
     * 创建最近的退回步骤
     *
     * @param step
     */
    WorkflowStep createNearestBackStep(WorkflowStep step);

    /**
     * 创建退回步骤
     *
     * @param step      流程步骤集合
     * @param backFirst 退回到起始节点
     * @return
     */
    WorkflowStep createBackStep(WorkflowStep step, Boolean backFirst);

    /**
     * 同级步骤
     *
     * @return
     */
    List<WorkflowStep> siblingSteps(WorkflowStep step);

    /**
     * 流程步骤是否处理
     *
     * @param step
     */
    Boolean isAction(WorkflowStep step);

    /**
     * 流程步骤是否退回
     *
     * @param step 流程步骤
     * @return
     */
    Boolean isBack(WorkflowStep step);

    /**
     * 流程是否被中止
     *
     * @param workflow 流程
     * @return
     */
    Boolean isAbort(Workflow workflow);
}
