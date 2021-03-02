package com.gaoy.flowable.chat;

import com.gaoy.flowable.domain.Node;
import com.gaoy.flowable.domain.WorkflowStep;

import java.util.List;

/**
 * 画图类
 */
public interface ChartCore {

    /**
     * 画流程图
     * @param nodes 流程节点
     * @param steps 流程步骤（有效的流程步骤）
     * @return
     */
    DefaultViewNode drawWorkflow(List<Node> nodes, List<WorkflowStep> steps);
}
