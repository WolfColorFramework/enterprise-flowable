package com.gaoy.flowable.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.WorkflowStep;
import com.gaoy.flowable.service.NodeService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class DefaultNode implements NodeCore {

    @Autowired
    private NodeService nodeService;

    @Override
    public Node stepOfNode(WorkflowStep step) {
        return nodeService.getById(step.getNodeId());
    }

    @Override
    public List<Node> nextNodes(Node node) {
        return nextNodes(node.getTemplateId(), node.getOrderIndex());
    }

    @Override
    public List<Node> nextNodes(String templateId, Integer orderIndex) {
        int nextOrderIndex = orderIndex + 1;
        QueryWrapper<Node> queryWrapper = new QueryWrapper();
        queryWrapper.eq("template_id", templateId)
                .eq("order_index", nextOrderIndex)
                .isNull("delete_time")
                .orderByAsc("order_index");
        return nodeService.list(queryWrapper);
    }

    @Override
    public Node nearestBackNode(String nodeId) {
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

    @Override
    public Boolean validityMultiple(Node node) {
        return node.getMultipleApprove();
    }
}
