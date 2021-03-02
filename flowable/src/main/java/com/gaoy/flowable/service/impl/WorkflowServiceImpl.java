package com.gaoy.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gaoy.flowable.chat.ChartCore;
import com.gaoy.flowable.chat.DefaultViewNode;
import com.gaoy.flowable.core.NodeCore;
import com.gaoy.flowable.core.StepCore;
import com.gaoy.flowable.core.WorkflowCore;
import com.gaoy.flowable.core.WorkflowUser;
import com.gaoy.flowable.dao.WorkflowMapper;
import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
import com.gaoy.flowable.domain.WorkflowStep;
import com.gaoy.flowable.enums.ActionStatus;
import com.gaoy.flowable.enums.WorkflowStatus;
import com.gaoy.flowable.service.NodeService;
import com.gaoy.flowable.service.WorkflowService;
import com.gaoy.flowable.service.WorkflowStepService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WorkflowServiceImpl extends ServiceImpl<WorkflowMapper, Workflow> implements WorkflowService {

    @Autowired
    WorkflowCore workFlowCore;
    @Autowired
    WorkflowService workflowService;
    @Autowired
    NodeService nodeService;
    @Autowired
    WorkflowStepService stepService;
    @Autowired
    NodeCore nodeCore;
    @Autowired
    StepCore stepCore;
    @Autowired
    ChartCore chartCore;

    /**
     * 发起流程,并创建对应的待办
     *
     * @param args
     * @return
     * @throws Exception
     */
    @Override
    public String start(WorkflowArgs args, String loginUserId) {
        //启动流程
        List<WorkflowStep> list = workFlowCore.start(args, loginUserId);
        if (list == null) {
            throw new IllegalArgumentException("创建流程步骤失败");
        }
        return list.get(0).getWorkflowId();
    }

    /**
     * 通过
     *
     * @param args
     * @return
     */
    @Override
    public Boolean pass(WorkflowArgs args, String loginUserId) {
        List<WorkflowStep> pass = workFlowCore.pass(args, loginUserId);
        return pass != null;
    }

    /**
     * 退回
     *
     * @param args
     * @return
     */
    @Override
    public Boolean back(WorkflowArgs args) {
        WorkflowStep workflowStep = workFlowCore.back(args);
        if (workflowStep != null) {
            List<WorkflowStep> steps = new ArrayList<>();
            steps.add(workflowStep);
        }
        return true;
    }

    /**
     * 撤回
     *
     * @param workflow
     * @return
     */
    @Override
    public Boolean cancel(Workflow workflow) {
        workFlowCore.cancel(workflow);
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
        workFlowCore.obsolete(workflow);
        return true;
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
        workFlowCore.abort(workflow, firstUser);
        return true;
    }

    @Override
    public List<WorkflowStep> workflowActionStatus(String domainId) {
        Workflow workflow = this.getOne(new LambdaQueryWrapper<Workflow>().eq(Workflow::getDomainId, domainId));
        if (workflow == null) {
            return null;
        }

        List<WorkflowStep> steps = stepService.stepsOfWorkflow(workflow);
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            if (step.getResult() != null) {
                step.setResultDescription(ActionStatus.values()[step.getResult()].name());
            } else {
                step.setResultDescription("等待处理");
            }
        }
        return steps;
    }

    @Override
    public String lastStepStatus(String domainId) {
        Workflow workflow = this.getOne(new LambdaQueryWrapper<Workflow>().eq(Workflow::getDomainId, domainId));
        List<WorkflowStep> steps = stepService.stepsOfWorkflow(workflow);
        if (CollectionUtils.isEmpty(steps)) {
            throw new RuntimeException("无流程");
        } else {
            // 未处理完的流程步骤
            List<WorkflowStep> unActionSteps = steps.stream()
                    .filter(c -> StringUtils.isEmpty(c.getResult())).collect(Collectors.toList());

            // 无待处理流程，[通过] or [中止]
            if (CollectionUtils.isEmpty(unActionSteps)) {
                // 通过 or 中止
                WorkflowStep workflowStep = steps.get(steps.size() - 1);
                if (workflowStep.getResult() == ActionStatus.中止.ordinal()) {
                    return WorkflowStatus.中止.name();
                } else {
                    return WorkflowStatus.已完成.name();
                }
            } else if (unActionSteps.size() > 1) {
                // 待处理流程>1，[会审]
                return unActionSteps.get(0).getNodeName();
            } else {
                // unActionSteps.size() == 1
                WorkflowStep unActionStep = unActionSteps.get(0);
                Node node = nodeCore.stepOfNode(unActionStep);
                if (node.getBackNode()) {
                    // [退回]
                    WorkflowStep preStep = stepService.getById(unActionStep.getPreStepId());
                    Node preNode = nodeCore.stepOfNode(preStep);
                    if (preNode.getOrderIndex() > node.getOrderIndex()) {
                        return WorkflowStatus.退回修改.name();
                    }
                }
                return unActionSteps.get(0).getNodeName();
            }
        }
    }

    @Override
    public WorkflowStep unActionStep(Workflow workflow, String loginUserId) {
        return stepService.unActionStep(workflow, loginUserId);
    }

    @Override
    public WorkflowStep unActionStep(String domainId, String loginUserId) {
        Workflow workFlow = this.getOne(new LambdaQueryWrapper<Workflow>().eq(Workflow::getDomainId, domainId));
        return unActionStep(workFlow, loginUserId);
    }

    @Override
    public List<WorkflowStep> effectiveSteps(String workflowId) {

        List<WorkflowStep> workflowSteps = stepService.list(new QueryWrapper<WorkflowStep>()
                .eq("workflow_id", workflowId)
                .isNull("delete_time")
                .orderByAsc("create_time"));

        if (!CollectionUtils.isEmpty(workflowSteps)) {

            // 中止 | 废除 直接退出
            if (workflowSteps.stream().filter(c -> c.getResult() != null).anyMatch(c -> c.getResult().equals(ActionStatus.中止.ordinal())
                    || c.getResult().equals(ActionStatus.废除.ordinal()))) {
                return Lists.newArrayList();
            }

            // 有效流程步骤（含中间退回）
            String firstNodeId = workflowSteps.get(0).getNodeId();
            int lastIndexOf = workflowSteps.stream().map(c -> c.getNodeId()).collect(Collectors.toList()).lastIndexOf(firstNodeId);
            List<WorkflowStep> lastSteps = workflowSteps.stream().skip(lastIndexOf).collect(Collectors.toList());

            // 过滤掉【中间退回】
            List<Integer> backIndexes = backIndexes(lastSteps); // 根据【退回】对lastStep进行分割
            if (CollectionUtils.isEmpty(backIndexes)) {
                // 无中间退回，直接返回
                return lastSteps;
            } else {
                // 有中间退回，分段处理
                List<Node> nodes = nodeService.listByIds(lastSteps.stream().map(c -> c.getNodeId()).collect(Collectors.toList()));
                lastSteps.forEach(c -> c.setNodeIndex(nodes.stream().filter(d -> d.getId().equals(c.getNodeId())).findFirst().get().getOrderIndex()));

                List<WorkflowStep> filterSteps = new ArrayList<>();
                Integer leastNodeIndex = 0;
                Map<Integer, List<WorkflowStep>> splitSteps = splitSteps(lastSteps, backIndexes);   // 根据【退回索引】，分割流程步
                for (int i = splitSteps.size() - 1; i >= 0; i--) {
                    // 最后一段，所有步骤全部有效
                    if (i == splitSteps.size() - 1) {
                        List<WorkflowStep> steps = splitSteps.get(i);
                        leastNodeIndex = steps.get(0).getNodeIndex();
                        filterSteps.addAll(splitSteps.get(i));
                        continue;
                    }

                    // 其余分段，进行合并
                    unionSteps(filterSteps, splitSteps.get(i), leastNodeIndex);
                }

                filterSteps.sort(Comparator.comparing(WorkflowStep::getNodeIndex));
                return filterSteps;
            }
        }
        return Lists.newArrayList();

    }

    @Override
    public DefaultViewNode chartWorkflow(String workflowId) {

        List<WorkflowStep> effectiveSteps = effectiveSteps(workflowId);
        Workflow workflow = workflowService.getById(workflowId);

        List<Node> nodes = nodeService.list(new QueryWrapper<Node>()
                .eq("template_id", workflow.getTemplateId())
                .orderByAsc("order_index"));

        return chartCore.drawWorkflow(nodes, effectiveSteps);
    }

    private List<Integer> backIndexes(List<WorkflowStep> lastSteps) {
        List<Integer> result = Lists.newArrayList();

        for (int i = 0; i < lastSteps.size(); i++) {
            WorkflowStep step = lastSteps.get(i);
            if (step.getResult() != null && step.getResult().equals(ActionStatus.退回.ordinal())) {
                result.add(i);
            }
        }
        return result;
    }

    private Map<Integer, List<WorkflowStep>> splitSteps(List<WorkflowStep> lastSteps, List<Integer> backIndexes) {
        Map<Integer, List<WorkflowStep>> result = Maps.newHashMap();
        int backIndexSize = backIndexes.size();
        int skip = 0;
        for (int i = 0; i < backIndexSize; i++) {
            int limit = backIndexes.get(i) - skip;
            result.put(i, lastSteps.stream().skip(skip).limit(limit).collect(Collectors.toList()));
            skip = backIndexes.get(i) + 1;  // 移动至下一个有效索引
        }

        // 最后一段，单独处理
        result.put(backIndexes.size(), lastSteps.stream().skip(skip).collect(Collectors.toList()));
        return result;
    }

    private void unionSteps(List<WorkflowStep> masterSteps, List<WorkflowStep> tempSteps, Integer leastNodeIndex) {
        for (WorkflowStep tempStep : tempSteps) {

            if (tempStep.getNodeIndex() >= leastNodeIndex)
                continue;

            if (masterSteps.stream().noneMatch(c -> c.getNodeIndex().equals(tempStep.getNodeIndex()))) {
                masterSteps.add(tempStep);
            }
        }
    }

}
