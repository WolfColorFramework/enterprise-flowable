package com.gaoy.flowable.chat;

import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.WorkflowStep;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultChat implements ChartCore {

    @Override
    public DefaultViewNode drawWorkflow(List<Node> nodes, List<WorkflowStep> steps) {

        // 根据orderIndex分组
        Map<Integer, List<Node>> groupNodes = nodes.stream().collect(Collectors.groupingBy(c -> c.getOrderIndex()));

        // 转换
        List<DefaultViewNode> viewNodes = new ArrayList<>();
        for (int i = 0; i < groupNodes.keySet().size(); i++) {
            DefaultViewNode transfer = transfer(groupNodes.get(i));
            viewNodes.add(transfer);
        }

        // 状态
        changeViewNodeStatus(viewNodes, steps);

        // 组装
        return assemblyViewNode(viewNodes);
    }

    // node -> singleViewNode
    private DefaultViewNode transfer(List<Node> nodes) {

        Node node = nodes.get(0);

        // 通用
        DefaultViewNode singleViewNode = new DefaultViewNode()
                .setNodeId(node.getId())
                .setNodeName(node.getNodeName())
                .setJump(node.getJump())
                .setNodeType(DefaultViewNode.ViewNodeType.审核节点.ordinal());

        // 条件节点
        if (!StringUtils.isEmpty(node.getExpression())) {
            singleViewNode.setNodeId(null)
                    .setNodeName(null)
                    .setNodeType(DefaultViewNode.ViewNodeType.路由节点.ordinal());

            List<DefaultViewNode> conditionNodes = new ArrayList<>();
            for (Node expressionNode : nodes) {
                // 条件节点
                DefaultViewNode conditionNode = new DefaultViewNode()
                        .setNodeId(expressionNode.getId())
                        .setNodeName(expressionNode.getExpressionDescription())
                        .setNodeType(DefaultViewNode.ViewNodeType.条件节点.ordinal());

                // 审批节点
                DefaultViewNode actionNode = new DefaultViewNode()
                        .setNodeId(expressionNode.getId())
                        .setNodeName(expressionNode.getNodeName())
                        .setNodeType(DefaultViewNode.ViewNodeType.审核节点.ordinal());

                conditionNode.setChildrenNode(actionNode);

                conditionNodes.add(conditionNode);
            }

            // 单个查询条件(jump节点)，需要补一个空节点
            if (conditionNodes.size() == 1) {
                // 空条件节点
                DefaultViewNode conditionNode = new DefaultViewNode()
                        .setNodeId("null")  // 特殊标记，标记是额外创造的节点
                        .setNodeType(DefaultViewNode.ViewNodeType.条件节点.ordinal());

                conditionNodes.add(conditionNode);
            }

            singleViewNode.setConditionList(conditionNodes);
        }

        // jump节点
        if (node.getJump() != null && node.getJump()) {

        }

        return singleViewNode;
    }

    // 修改viewNode状态（状态、处理人）
    private void changeViewNodeStatus(List<DefaultViewNode> viewNodes, List<WorkflowStep> steps) {

        Map<String, List<WorkflowStep>> groupSteps = steps.stream().collect(Collectors.groupingBy(c -> c.getNodeId()));

        for (int i = 0; i < viewNodes.size(); i++) {

            DefaultViewNode viewNode = viewNodes.get(i);

            // 审核节点
            if (viewNode.getNodeType().equals(DefaultViewNode.ViewNodeType.审核节点.ordinal())) {
                String nodeId = viewNode.getNodeId();
                List<WorkflowStep> workflowSteps = groupSteps.get(nodeId);
                changeViewNodeStatus_actionNode(i, viewNode, viewNodes, workflowSteps);
            }

            // 路由节点
            if (viewNode.getNodeType().equals(DefaultViewNode.ViewNodeType.路由节点.ordinal())) {
                changeViewNodeStatus_routeNode(viewNode, groupSteps);
            }
        }
    }

    // 装配viewNode
    private DefaultViewNode assemblyViewNode(List<DefaultViewNode> viewNodes) {

        DefaultViewNode viewNode = viewNodes.get(0);
        for (int i = 1; i < viewNodes.size(); i++) {
            DefaultViewNode currentViewNode = viewNodes.get(i);
            viewNode.setChildrenNode(currentViewNode);
            viewNode = currentViewNode;
        }

        return viewNodes.get(0);
    }

    private void changeViewNodeStatus_actionNode(int currentIndex, DefaultViewNode viewNode, List<DefaultViewNode> viewNodes, List<WorkflowStep> workflowSteps) {
        if (!CollectionUtils.isEmpty(workflowSteps)) {
            boolean b = workflowSteps.stream().anyMatch(c -> c.getResult() == null);
            if (b) {
                viewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.进行中.ordinal());
            } else {
                viewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.已完成.ordinal());
            }

            List<String> actionUsers = workflowSteps.stream().map(c -> c.getActionUserName()).collect(Collectors.toList());
            viewNode.setActionUsers(actionUsers);
        }

        // 进行jump处理
        if (viewNode.getNodeStatus().equals(DefaultViewNode.ViewNodeStatus.已完成.ordinal())) {
            // jump判断
            int jumpViewNode = currentIndex - 2;
            if (jumpViewNode >= 0) {
                DefaultViewNode pre_pre_viewNode = viewNodes.get(jumpViewNode);
                if (pre_pre_viewNode.getJump() != null && pre_pre_viewNode.getJump()) {
                    Optional<DefaultViewNode> nullViewNode = pre_pre_viewNode.getConditionList().stream()
                            .filter(c -> c.getNodeId().equals("null")).findFirst();
                    if (nullViewNode.isPresent()) {
                        nullViewNode.get().setNodeStatus(DefaultViewNode.ViewNodeStatus.已完成.ordinal());
                    }
                }
            }
        }
    }

    private void changeViewNodeStatus_routeNode(DefaultViewNode viewNode, Map<String, List<WorkflowStep>> groupSteps) {
        List<DefaultViewNode> conditionNodes = viewNode.getConditionList();
        List<String> nodeIds = conditionNodes.stream().map(c -> c.getNodeId()).collect(Collectors.toList());

        for (String nodeId : nodeIds) {
            DefaultViewNode conditionViewNode = conditionNodes.stream().filter(c -> c.getNodeId().equals(nodeId)).findFirst().get();
            DefaultViewNode actionViewNode = conditionViewNode.getChildrenNode();

            List<WorkflowStep> workflowSteps = groupSteps.get(nodeId);
            if (!CollectionUtils.isEmpty(workflowSteps)) {
                boolean action = workflowSteps.stream().anyMatch(c -> c.getResult() == null);
                if (action) {
                    // 路由节点
                    viewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.进行中.ordinal());
                    // 条件节点
                    conditionViewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.进行中.ordinal());
                    // 审批节点
                    actionViewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.进行中.ordinal());
                } else {
                    // 路由节点
                    viewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.已完成.ordinal());
                    // 条件节点
                    conditionViewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.已完成.ordinal());
                    // 审批节点
                    actionViewNode.setNodeStatus(DefaultViewNode.ViewNodeStatus.已完成.ordinal());
                }
                // 审批人
                actionViewNode.setActionUsers(workflowSteps.stream().map(c -> c.getActionUserName()).collect(Collectors.toList()));
                break;
            }
        }
    }

}
