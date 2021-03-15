package com.gaoy.flowable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gaoy.flowable.dao.NodeMapper;
import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.service.NodeService;

import java.util.List;
import java.util.stream.Collectors;

public class NodeServiceImpl extends ServiceImpl<NodeMapper, Node> implements NodeService {

    @Override
    public List<Node> nodesOfTemplate(String templateId) {
        QueryWrapper<Node> queryWrapper = new QueryWrapper();
        queryWrapper.eq("template_id", templateId)
                .isNull("delete_time")
                .orderByAsc("order_index");
        return this.list(queryWrapper);
    }

    @Override
    public List<Node> nodesOfTemplate(String templateId, Integer orderIndex) {
        List<Node> fNodes = nodesOfTemplate(templateId);
        return fNodes.stream().filter(c -> c.getOrderIndex().equals(orderIndex)).collect(Collectors.toList());
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
        return this.list(queryWrapper);
    }
}
