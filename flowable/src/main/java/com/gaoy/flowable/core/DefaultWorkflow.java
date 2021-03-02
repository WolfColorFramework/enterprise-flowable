package com.gaoy.flowable.core;

import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
import com.gaoy.flowable.domain.WorkflowStep;
import com.gaoy.flowable.enums.ActionStatus;
import com.gaoy.flowable.service.WorkflowService;
import com.gaoy.flowable.service.WorkflowStepService;
import com.gaoy.flowable.utils.UuidPlus;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultWorkflow implements WorkflowCore {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WorkflowStepService stepService;

    @Override
    public Workflow create(WorkflowArgs args) {
        Workflow workflow = new Workflow()
                .setId(UuidPlus.getUUIDPlus())
                .setDomainId(args.getDomainId())
                .setDomainName(args.getDomainName())
                .setCreateTime(LocalDateTime.now())
                .setUserId(args.getApplyUserId())
                .setTemplateId(args.getTemplateId())
                .setTitle(args.getTitle());
        workflowService.save(workflow);
        return workflow;
    }

    @Override
    public List<WorkflowStep> start(WorkflowArgs args, String loginUserId) {
        Workflow workflow = create(args);
        WorkflowStep step = stepService.createFirstStep(workflow);
        stepService.unActionStep(workflow, loginUserId);
        return stepService.pass(step, args);
    }

    @Override
    public List<WorkflowStep> pass(WorkflowArgs args, String loginUserId) {
        WorkflowStep unActionStep = workflowService.unActionStep(args.getDomainId(), loginUserId);
        return stepService.pass(unActionStep, args);
    }

    @Override
    public WorkflowStep back(WorkflowArgs args) {
        WorkflowStep step = workflowService.unActionStep(args.getDomainId(), args.getApplyUserId());
        return stepService.back(step, false, args);
    }

    @Override
    public Boolean cancel(Workflow workflow) {
        List<WorkflowStep> steps = stepService.stepsOfWorkflow(workflow);
        // 当前流程步不是“发起”流程步，不允许撤回
        Map<String, List<String>> nodeIdMap = steps.stream().map(c -> c.getNodeId()).collect(Collectors.groupingBy(d -> d));
        if (nodeIdMap.size() != 2) {
            return false;
        }

        // 下一步流程步(i从1开始)，已经被处理，不可撤回
        for (int i = 1; i < steps.size(); i++) {
            if (steps.get(i).getResult() != null) {
                return false;
            }
        }

        // 撤回操作
        // 删除所有流程步
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            step.setDeleteTime(LocalDateTime.now());
            stepService.updateById(step);
        }

        // 删除流程
        workflow.setDeleteTime(LocalDateTime.now());
        workflowService.updateById(workflow);
        return true;
    }

    @Override
    public Boolean abort(Workflow workflow, WorkflowUser user) {
        List<WorkflowStep> steps = stepService.stepsOfWorkflow(workflow);
        // 流程的发起人，才可以中止
        WorkflowStep firstStep = steps.get(0);
        if (user == null) {
            throw new RuntimeException("当前登录人id未传");
        }
        if (!firstStep.getActionUserId().equalsIgnoreCase(user.getUserId())) {
            throw new RuntimeException("没有权限的操作");
        }

        // 流程未结束，才可以中止
        List<WorkflowStep> unActionSteps = steps.stream().filter(c -> c.getResult() == null).collect(Collectors.toList());
        if (unActionSteps.size() == 0) {
            return false;
        }

        // 删除未处理的workflowStep
        for (WorkflowStep step : unActionSteps) {
            step.setDeleteTime(LocalDateTime.now());
            stepService.updateById(step);
        }

        // 插入废除的workflowStep
        WorkflowStep abortStep = new WorkflowStep()
                .setDomainId(firstStep.getDomainId())
                .setDomainName(firstStep.getDomainName())
                .setWorkflowId(firstStep.getWorkflowId())
                .setActionUserId(user.getUserId())
                .setActionUserName(user.getUserName())
                .setResult(ActionStatus.中止.ordinal())
                .setCreateTime(LocalDateTime.now())
                .setActionTime(LocalDateTime.now())
                .setId(UuidPlus.getUUIDPlus());
        stepService.save(abortStep);
        return true;
    }

    /**
     * 废除
     *
     * @param workflow 工作流
     * @return
     */
    @Override
    public Boolean obsolete(Workflow workflow) {
        WorkflowStep step = stepService.unActionStep(workflow, workflow.getUserId())
                .setNodeName("")
                .setResult(ActionStatus.废除.ordinal())
                .setActionTime(LocalDateTime.now());
        return stepService.updateById(step);
    }

}
