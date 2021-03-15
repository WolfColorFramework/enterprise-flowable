package com.gaoy.flowable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gaoy.flowable.domain.Node;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface NodeService extends IService<Node> {

    /**
     * 模板下的所有节点
     *
     * @param templateId 模板Id
     * @return
     */
    List<Node> nodesOfTemplate(String templateId);

    /**
     * 模板下的对应节点
     *
     * @param templateId 模板Id
     * @param orderIndex 索引
     * @return
     */
    List<Node> nodesOfTemplate(String templateId, Integer orderIndex);

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

}
