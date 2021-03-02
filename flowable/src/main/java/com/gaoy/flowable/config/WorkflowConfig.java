package com.gaoy.flowable.config;

import com.gaoy.flowable.chat.ChartCore;
import com.gaoy.flowable.chat.DefaultChart;
import com.gaoy.flowable.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfig {

    @Bean
    @ConditionalOnMissingBean(NodeCore.class)
    public NodeCore nodeCore() {
        return new DefaultNode();
    }

    @Bean
    @ConditionalOnMissingBean(StepCore.class)
    public StepCore stepCore() {
        return new DefaultStep();
    }

    @Bean
    @ConditionalOnMissingBean(WorkflowCore.class)
    public WorkflowCore workflowCore() {
        return new DefaultWorkflow();
    }

    @Bean
    @ConditionalOnMissingBean(ChartCore.class)
    public ChartCore chartCore() {
        return new DefaultChart();
    }

}
