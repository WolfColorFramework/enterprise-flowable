package com.gaoy.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gaoy.flowable.core.WorkflowUser;
import com.gaoy.flowable.dao.WorkflowStepMapper;
import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
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

public class WorkflowStepServiceImpl extends ServiceImpl<WorkflowStepMapper, WorkflowStep> implements WorkflowStepService {

    @Autowired
    private NodeService nodeService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private WorkflowUser workflowUser;

    @Override
    public List<WorkflowStep> pass(WorkflowStep step, WorkflowArgs args) {
        Workflow workFlow = workflowService.getById(step.getWorkflowId());
        Node currentNode = nodeService.getById(step.getNodeId());

        if (isAbort(workFlow)) {
            return null;
        }

        if (!isAction(step)) {
            //更新当前步骤
            if (StringUtils.isEmpty(step.getPreStepId())) {
                step.setResult(ActionStatus.发起.ordinal());
            } else if (currentNode.getOrderIndex() == 0) {
                step.setResult(ActionStatus.重新发起.ordinal());
            } else {
                step.setResult(ActionStatus.通过.ordinal());
            }
            step.setActionTime(LocalDateTime.now());
            step.setSuggest(args.getOption());
            step.setFileUrl(args.getFileUrl());
            step.setFileDescription(args.getFileDescription());
            this.updateById(step);

            // 下一步处理人为空，不再生成下一步流程，直接返回
            if (CollectionUtils.isEmpty(args.getApproveUserIds())) {
                return null;
            }

            // 判断当前节点是否为多人审批节点
            if (currentNode.getMultipleApprove()) {
                // 所有同级流程步都处理完=>不含当前步骤
                List<WorkflowStep> siblingSteps = siblingSteps(step);
                boolean isAction = isAction(siblingSteps);

                // 所有流程步处理完，才可以进行下一步操作
                if (isAction) {
                    // 有一个退回，退回审批
                    boolean isBack = isBack(siblingSteps);
                    if (isBack) {
                        List<WorkflowStep> steps = new ArrayList<>();
                        steps.addAll(this.createNearestBackStep(step));
                        return steps;
                    }
                } else {
                    return null;
                }
            }
            List<WorkflowUser> actionUsers = workflowUser.UsersByIds(args.getApproveUserIds());
            return this.createNextStep(step, currentNode, actionUsers, args.getExpressionValue());
        } else {
            throw new RuntimeException("流程发生变化");
        }
    }

    @Override
    public List<WorkflowStep> back(WorkflowStep step, Boolean backFirst, WorkflowArgs args) {
        Workflow workFlow = new Workflow().setId(step.getWorkflowId());
        Node node = nodeService.getById(step.getNodeId());
        if (isAbort(workFlow)) {
            return null;
        }
        if (!isAction(step)) {
            // 设置当前流程为：back
            step.setResult(ActionStatus.退回.ordinal());
            step.setActionTime(LocalDateTime.now());
            step.setSuggest(args.getOption());
            step.setFileUrl(args.getFileUrl());
            this.updateById(step);

            if (node.getMultipleApprove()) {
                // 所有同级流程步都处理完
                List<WorkflowStep> siblingSteps = this.siblingSteps(step);
                boolean isAction = isAction(siblingSteps);
                // 所有流程步处理完，才可以进行退回
                if (isAction) {
                    return this.createBackStep(step, backFirst);
                } else {
                    return null;
                }
            } else {
                return this.createBackStep(step, backFirst);
            }
        } else {
            throw new RuntimeException("流程发生变化");
        }
    }

    @Override
    public WorkflowStep unActionStep(Workflow workflow, String loginUserId) {
        List<WorkflowStep> steps = stepsOfWorkflow(workflow);
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            // 判断当前登陆人是否是处理人并且步骤未处理
            if (step.getResult() == null && step.getActionUserId().equals(loginUserId)) {
                return step;
            }
        }
        return null;
    }

    @Override
    public List<WorkflowStep> stepsOfWorkflow(Workflow workflow) {
        QueryWrapper<WorkflowStep> queryWrapper = new QueryWrapper();
        queryWrapper.eq("workflow_id", workflow.getId())
                .isNull("delete_time")
                .orderByAsc("create_time");
        return this.list(queryWrapper);
    }

    @Override
    public WorkflowStep createFirstStep(Workflow workFlow) {
        // 获取template中的第一个Node
        List<Node> nodes = nodeService.nodesOfTemplate(workFlow.getTemplateId());
        if (CollectionUtils.isEmpty(nodes)) {
            throw new RuntimeException("流程步节点数据异常");
        }

        // 创建流程步
        WorkflowStep step = new WorkflowStep()
                .setId(UuidPlus.getUUIDPlus())
                .setActionUserId(workFlow.getUserId())
                .setActionUserName(workFlow.getUserName())
                .setWorkflowId(workFlow.getId())
                .setDomainId(workFlow.getDomainId())
                .setDomainName(workFlow.getDomainName())
                .setNodeId(nodes.get(0).getId())
                .setNodeName(nodes.get(0).getNodeName())
                .setCreateTime(LocalDateTime.now());
        this.save(step);
        return step;
    }

    private List<WorkflowStep> siblingSteps(WorkflowStep step) {
        QueryWrapper<WorkflowStep> queryWrapper = new QueryWrapper();
        queryWrapper.eq("domain_id", step.getDomainId())
                .eq("domain_name", step.getDomainName())
                .eq("node_id", step.getNodeId())
                .ne("id", step.getId())
                .eq("pre_step_id", step.getPreStepId())
                .isNull("delete_time");
        return this.list(queryWrapper);
    }

    private Boolean isAction(WorkflowStep step) {
        QueryWrapper<WorkflowStep> queryWrapper = new QueryWrapper();
        queryWrapper.eq("id", step.getId())
                .isNull("result")
                .isNull("delete_time");
        return this.count(queryWrapper) < 1;
    }

    private Boolean isAction(List<WorkflowStep> steps) {
        for (WorkflowStep step : steps) {
            if (step.getResult() == null) {
                return false;
            }
        }
        return true;
    }

    private Boolean isBack(List<WorkflowStep> steps) {
        for (WorkflowStep step : steps) {
            if (step.getResult().equals(ActionStatus.退回)) {
                return true;
            }
        }
        return false;
    }

    private Boolean isAbort(Workflow workflow) {
        List<WorkflowStep> steps = this.stepsOfWorkflow(workflow);
        if (steps.size() == 0) {
            return false;
        }

        WorkflowStep lastStep = steps.get(steps.size() - 1);
        if (lastStep.getResult() != null && lastStep.getResult() == ActionStatus.中止.ordinal()) {
            return true;
        }
        return false;
    }

    private List<WorkflowStep> createBackStep(WorkflowStep step, Boolean backFirst) {
        Node node;
        Workflow workFlow = workflowService.getById(step.getWorkflowId());
        if (backFirst) {
            node = nodeService.nodesOfTemplate(workFlow.getTemplateId(), 0).get(0);
        } else {
            // 根据当前node，查找最近的一个退回节点
            node = this.nearestBackNode(step.getNodeId());
        }

        // 创建新的流程步
        List<WorkflowStep> newSteps = this.list(new QueryWrapper<WorkflowStep>().eq("node_id", node.getId())
                .isNull("delete_time"));

        for (WorkflowStep newStep : newSteps) {
            newStep.setId(UuidPlus.getUUIDPlus())
                    .setPreStepId(step.getId())
                    .setResult(null)
                    .setActionTime(null)
                    .setCreateTime(LocalDateTime.now())
                    .setSuggest(null);
        }
        this.saveBatch(newSteps);
        return newSteps;
    }

    private List<WorkflowStep> createNearestBackStep(WorkflowStep step) {
        return createBackStep(step, false);
    }

    private List<WorkflowStep> createNextStep(WorkflowStep step, Node node, List<WorkflowUser> actionUsers, Object expressionValue) {
        List<WorkflowStep> steps;
        List<Node> nextNodes = nodeService.nextNodes(node);
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
            List<Node> nextNextNodes = nodeService.nextNodes(node.getTemplateId(), orderIndex);
            if (!CollectionUtils.isEmpty(nextNextNodes)) {
                return createSteps(step, nextNextNodes.get(0), actionUsers);
            }
        }
        return null;
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
            this.save(newStep);
        }
        return steps;
    }

    private Boolean expressionHandler(Node node, Object value) {
        Map valueMap = new HashMap(16);
        valueMap.put("key", value);
        boolean compareValue = (boolean) JsExecution.result(valueMap, node.getExpression());
        return compareValue;
    }

    private Node nearestBackNode(String nodeId) {
        Node node = nodeService.getById(nodeId);
        QueryWrapper<Node> queryWrapper = new QueryWrapper();
        queryWrapper.eq("template_id", node.getTemplateId())
                .lt("order_index", node.getOrderIndex())
                .isNull("delete_time")
                .orderByDesc("order_index");
        List<Node> nodes = nodeService.list(queryWrapper);

        Node backNode = null;
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getBackNode()) {
                backNode = nodes.get(i);
                break;
            }
        }
        if (backNode == null) {
            throw new RuntimeException("流程节点数据错误");
        }
        return backNode;
    }
}
