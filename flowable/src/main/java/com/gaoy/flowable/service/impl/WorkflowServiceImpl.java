package com.gaoy.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gaoy.flowable.chat.ChartCore;
import com.gaoy.flowable.chat.DefaultViewNode;
import com.gaoy.flowable.core.WorkflowUser;
import com.gaoy.flowable.dao.WorkflowMapper;
import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
import com.gaoy.flowable.domain.WorkflowStep;
import com.gaoy.flowable.enums.ActionStatus;
import com.gaoy.flowable.service.NodeService;
import com.gaoy.flowable.service.WorkflowService;
import com.gaoy.flowable.service.WorkflowServiceExpand;
import com.gaoy.flowable.service.WorkflowStepService;
import com.gaoy.flowable.utils.UuidPlus;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    @Autowired
    NodeService nodeService;
    @Autowired
    WorkflowServiceExpand workflowServiceExpand;
    @Autowired
    WorkflowStepService stepService;

    /**
     * 发起流程,并创建对应的待办
     *
     * @param args
     * @return
     * @throws Exception
     */
    @Override
    public List<WorkflowStep> start(WorkflowArgs args, String loginUserId) {
        //启动流程
        Workflow workflow = this.create(args);
        WorkflowStep step = stepService.createFirstStep(workflow);
        List<WorkflowStep> steps = stepService.pass(step, args);

        if (steps == null) {
            throw new IllegalArgumentException("创建流程步骤失败");
        }
        return steps;
    }

    /**
     * 通过
     *
     * @param args
     * @return
     */
    @Override
    public Boolean pass(Workflow workflow, WorkflowArgs args, String loginUserId) {
        WorkflowStep unActionStep = stepService.unActionStep(workflow, loginUserId);
        List<WorkflowStep> pass = stepService.pass(unActionStep, args);
        return !CollectionUtils.isEmpty(pass);
    }

    /**
     * 退回
     *
     * @param args
     * @return
     */
    @Override
    public Boolean back(Workflow workflow, WorkflowArgs args) {
        WorkflowStep step = stepService.unActionStep(workflow, args.getApplyUserId());
        List<WorkflowStep> backSteps = stepService.back(step, false, args);
        return !CollectionUtils.isEmpty(backSteps);
    }

    /**
     * 撤回
     *
     * @param workflow
     * @return
     */
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
        this.updateById(workflow);
        return true;
    }

    /**
     * 废除
     *
     * @param workflow
     * @return
     */
    @Override
    public Boolean obsolete(Workflow workflow) {
        WorkflowStep step = stepService.unActionStep(workflow, workflow.getUserId())
                .setResult(ActionStatus.废除.ordinal())
                .setActionTime(LocalDateTime.now());
        return stepService.updateById(step);
    }

    /**
     * 流程中止
     *
     * @param workflow
     * @param firstUser
     * @return
     */
    @Override
    public Boolean abort(Workflow workflow, WorkflowUser firstUser) {
        List<WorkflowStep> steps = stepService.stepsOfWorkflow(workflow);
        // 流程的发起人，才可以中止
        WorkflowStep firstStep = steps.get(0);
        if (firstUser == null) {
            throw new RuntimeException("当前登录人id未传");
        }
        if (!firstStep.getActionUserId().equalsIgnoreCase(firstUser.getUserId())) {
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
                .setActionUserId(firstUser.getUserId())
                .setActionUserName(firstUser.getUserName())
                .setResult(ActionStatus.中止.ordinal())
                .setCreateTime(LocalDateTime.now())
                .setActionTime(LocalDateTime.now())
                .setId(UuidPlus.getUUIDPlus());
        stepService.save(abortStep);
        return true;
    }

    private Workflow create(WorkflowArgs args) {
        Workflow workflow = new Workflow()
                .setId(UuidPlus.getUUIDPlus())
                .setDomainId(args.getDomainId())
                .setDomainName(args.getDomainName())
                .setCreateTime(LocalDateTime.now())
                .setUserId(args.getApplyUserId())
                .setTemplateId(args.getTemplateId())
                .setTitle(args.getTitle());
        this.save(workflow);
        return workflow;
    }
}
