package com.gaoy.flowable.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowStep;
import com.gaoy.flowable.enums.ActionStatus;
import com.gaoy.flowable.service.NodeService;
import com.gaoy.flowable.service.WorkflowService;
import com.gaoy.flowable.service.WorkflowStepService;
import com.gaoy.flowable.utils.JsExecution;
import com.gaoy.flowable.utils.UuidPlus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultStep implements StepCore {

    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private WorkflowStepService stepService;
    @Autowired
    private NodeCore nodeCore;

    @Override
    public List<WorkflowStep> createNextStep(WorkflowStep step, Node node, List<WorkflowUser> actionUsers, Object expressionValue) {
        List<WorkflowStep> steps;
        List<Node> nextNodes = nodeCore.nextNodes(node);
        for (Node nextNode : nextNodes) {
            // 表达式节点
            if (!StringUtils.isEmpty(nextNode.getExpression())) {
                // 创建表达式节点流程
                if (expressionHandler(nextNode, expressionValue)) {
                    steps = createSteps(step, nextNode, actionUsers);
                } else {
                    continue;
                }
            } else {
                // 非表达式节点流程
                steps = createSteps(step, nextNode, actionUsers);
            }

            if (steps != null) {
                return steps;
            }
        }

        // jump节点，跳转至【分支节点】后面的节点
        if (node.getJump() != null && node.getJump()) {
            int orderIndex = node.getOrderIndex() + 1;
            List<Node> nextNextNodes = nodeCore.nextNodes(node.getTemplateId(), orderIndex);
            if (!CollectionUtils.isEmpty(nextNextNodes)) {
                return createSteps(step, nextNextNodes.get(0), actionUsers);
            }
        }
        return null;
    }

    @Override
    public WorkflowStep createNearestBackStep(WorkflowStep step) {
        return createBackStep(step, false);
    }

    @Override
    public WorkflowStep createBackStep(WorkflowStep step, Boolean backFirst) {
        WorkflowStep newStep;
        Node node;
        Workflow workFlow = workflowService.getById(step.getWorkflowId());
        if (backFirst) {
            node = nodeService.nodesOfTemplate(workFlow.getTemplateId(), 0).get(0);
        } else {
            // 根据当前node，查找最近的一个退回节点
            node = nodeCore.nearestBackNode(step.getNodeId());
        }

        // 创建新的流程步
        newStep = stepService.getById(node.getId());
        newStep.setId(UuidPlus.getUUIDPlus())
                .setPreStepId(step.getId())
                .setResult(null)
                .setActionTime(null)
                .setCreateTime(LocalDateTime.now())
                .setSuggest(null);
        stepService.save(newStep);
        return newStep;
    }

    @Override
    public List<WorkflowStep> siblingSteps(WorkflowStep step) {
        QueryWrapper<WorkflowStep> queryWrapper = new QueryWrapper();
        queryWrapper.eq("domain_id", step.getDomainId())
                .eq("domain_name", step.getDomainName())
                .eq("node_id", step.getNodeId())
                .ne("id", step.getId())
                .eq("pre_step_id", step.getPreStepId())
                .isNull("delete_time");
        return stepService.list(queryWrapper);
    }

    @Override
    public Boolean isAction(WorkflowStep step) {
        QueryWrapper<WorkflowStep> queryWrapper = new QueryWrapper();
        queryWrapper.eq("id", step.getId())
                .isNull("result")
                .isNull("delete_time");
        return stepService.count(queryWrapper) < 1;
    }

    @Override
    public Boolean isBack(WorkflowStep step) {
        QueryWrapper<WorkflowStep> queryWrapper = new QueryWrapper();
        queryWrapper.eq("id", step.getId())
                .eq("result", ActionStatus.退回.ordinal())
                .isNull("delete_time");
        return stepService.count(queryWrapper) > 0;
    }

    @Override
    public Boolean isAbort(Workflow workflow) {
        List<WorkflowStep> steps = stepService.stepsOfWorkflow(workflow);
        if (steps.size() == 0) {
            return false;
        }

        WorkflowStep lastStep = steps.get(steps.size() - 1);
        if (lastStep.getResult() != null && lastStep.getResult() == ActionStatus.中止.ordinal()) {
            return true;
        }
        return false;
    }

    private List<WorkflowStep> createSteps(WorkflowStep step, Node node, List<WorkflowUser> users) {
        List<WorkflowStep> steps = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            WorkflowStep newStep = new WorkflowStep()
                    .setId(UuidPlus.getUUIDPlus())
                    .setWorkflowId(step.getWorkflowId())
                    .setDomainId(step.getDomainId())
                    .setDomainName(step.getDomainName())
                    .setNodeId(node.getId())
                    .setNodeName(node.getNodeName())
                    .setActionUserId(users.get(i).getUserId())
                    .setActionUserName(users.get(i).getUserName())
                    .setPreStepId(step.getId())
                    .setCreateTime(LocalDateTime.now());
            steps.add(newStep);
            stepService.save(newStep);
        }
        return steps;
    }

    private Boolean expressionHandler(Node node, Object value) {
        Map valueMap = new HashMap(16);
        valueMap.put("key", value);
        boolean compareValue = (boolean) JsExecution.result(valueMap, node.getExpression());
        return compareValue;
    }

}
