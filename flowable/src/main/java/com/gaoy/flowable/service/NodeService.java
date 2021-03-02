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

}
