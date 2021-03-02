package com.gaoy.flowable.core;

import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.WorkflowStep;

import java.util.List;

public interface NodeCore {

    /**
     * 步骤的节点
     * @param step
     * @return
     */
    Node stepOfNode(WorkflowStep step);

    /**
     * 下一节点
     * @param node 节点
     * @return
     */
    List<Node> nextNodes(Node node);

    /**
     * 下一节点
     * @param templateId 模板Id
     * @param orderIndex 当前节点Index
     * @return
     */
    List<Node> nextNodes(String templateId, Integer orderIndex);

    /**
     * 最近的退回节点
     * @param nodeId 节点Id
     * @return
     */
    Node nearestBackNode(String nodeId);

    /**
     * 节点是否是会审节点
     * @param node 节点
     * @return
     */
    Boolean validityMultiple(Node node);
}
