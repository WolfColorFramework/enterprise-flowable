package com.gaoy.flowable.core;

import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
import com.gaoy.flowable.domain.WorkflowStep;

import java.util.List;

public interface WorkflowCore {

    Workflow create(WorkflowArgs args);

    List<WorkflowStep> start(WorkflowArgs args, String loginUserId);

    List<WorkflowStep> pass(WorkflowArgs args, String loginUserId);

    WorkflowStep back(WorkflowArgs args);

    /**
     * 撤回
     *
     * @param workflow
     * @return
     */
    Boolean cancel(Workflow workflow);

    /**
     * 中止
     *
     * @param workflow
     * @return
     */
    Boolean abort(Workflow workflow, WorkflowUser user);

    /**
     * 废除
     *
     * @param workflow
     * @return
     */
    Boolean obsolete(Workflow workflow);

}
