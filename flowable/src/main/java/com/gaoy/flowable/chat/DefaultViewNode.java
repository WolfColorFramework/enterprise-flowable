package com.gaoy.flowable.chat;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class DefaultViewNode {
    /**
     * 节点Id
     */
    private String nodeId;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 节点类型
     */
    private Integer nodeType = ViewNodeType.审核节点.ordinal();
    /**
     * 节点状态
     */
    private Integer nodeStatus = ViewNodeStatus.未进行.ordinal();
    /**
     * 下一个节点
     */
    private DefaultViewNode childrenNode;
    /**
     * 处理人信息
     */
    private List<String> actionUsers = Lists.newArrayList();
    /**
     * 条件
     */
    private List<DefaultViewNode> conditionList;
    /**
     * 是否是跳跃节点
     */
    private Boolean jump;

    /**
     * 节点类型
     */
    public enum ViewNodeType {
        条件节点,
        审核节点,
        路由节点
    }

    /**
     * 节点状态
     */
    public enum ViewNodeStatus{
        未进行,
        已完成,
        进行中
    }
}
