package com.gaoy.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gaoy.flowable.core.NodeCore;
import com.gaoy.flowable.core.StepCore;
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
import com.gaoy.flowable.utils.UuidPlus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class WorkflowStepServiceImpl extends ServiceImpl<WorkflowStepMapper, WorkflowStep> implements WorkflowStepService {

    @Autowired
    private StepCore stepCore;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private WorkflowService workflowService;
    @Autowired
    private NodeCore nodeCore;
    @Autowired
    private WorkflowUser workflowUser;

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

    @Override
    public List<WorkflowStep> pass(WorkflowStep step, WorkflowArgs args) {
        Workflow workFlow = workflowService.getById(step.getWorkflowId());
        Node currentNode = nodeService.getById(step.getNodeId());

        if (stepCore.isAbort(workFlow)) {
            return null;
        }

        if (!stepCore.isAction(step)) {
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
            if (nodeCore.validityMultiple(currentNode)) {
                // 所有同级流程步都处理完=>不含当前步骤
                List<WorkflowStep> siblingSteps = stepCore.siblingSteps(step);
                WorkflowStep[] siblingStepArray = siblingSteps.toArray(new WorkflowStep[0]);
                boolean isAction = isAction(siblingStepArray);

                // 所有流程步处理完，才可以进行下一步操作
                if (isAction) {
                    // 有一个退回，退回审批
                    boolean isBack = isBack(siblingStepArray);
                    if (isBack) {
                        List<WorkflowStep> steps = new ArrayList<>();
                        steps.add(stepCore.createNearestBackStep(step));
                        return steps;
                    }
                } else {
                    return null;
                }
            }
            List<WorkflowUser> actionUsers = workflowUser.UsersByIds(args.getApproveUserIds());
            return stepCore.createNextStep(step, currentNode, actionUsers, args.getExpressionValue());
        } else {
            throw new RuntimeException("流程发生变化");
        }
    }

    @Override
    public WorkflowStep back(WorkflowStep step, Boolean backFirst, WorkflowArgs args) {
        Workflow workFlow = new Workflow().setId(step.getWorkflowId());
        Node node = nodeService.getById(step.getNodeId());
        if (stepCore.isAbort(workFlow)) {
            return null;
        }
        if (!stepCore.isAction(step)) {
            // 设置当前流程为：back
            step.setResult(ActionStatus.退回.ordinal());
            step.setActionTime(LocalDateTime.now());
            step.setSuggest(args.getOption());
            step.setFileUrl(args.getFileUrl());
            this.updateById(step);

            if (nodeCore.validityMultiple(node)) {
                // 所有同级流程步都处理完
                List<WorkflowStep> siblingSteps = stepCore.siblingSteps(step);
                WorkflowStep[] siblingStepArray = siblingSteps.toArray(new WorkflowStep[0]);
                boolean isAction = isAction(siblingStepArray);
                // 所有流程步处理完，才可以进行退回
                if (isAction) {
                    return stepCore.createBackStep(step, backFirst);
                } else {
                    return null;
                }
            } else {
                return stepCore.createBackStep(step, backFirst);
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
                // 获取下一个节点信息
                // FNode node = getNodeByStep(step);
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

    private Boolean isAction(WorkflowStep... steps) {
        for (WorkflowStep step : steps) {
            if (step.getResult() == null) {
                return false;
            }
        }
        return true;
    }

    private Boolean isBack(WorkflowStep... steps) {
        for (WorkflowStep step : steps) {
            if (step.getResult().equals(ActionStatus.退回)) {
                return true;
            }
        }
        return false;
    }

}
