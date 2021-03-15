package com.gaoy.flowable.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gaoy.flowable.chat.DefaultViewNode;
import com.gaoy.flowable.core.WorkflowUser;
import com.gaoy.flowable.domain.Workflow;
import com.gaoy.flowable.domain.WorkflowArgs;
import com.gaoy.flowable.domain.WorkflowStep;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WorkflowService extends IService<Workflow> {

    List<WorkflowStep> start(WorkflowArgs args, String loginUserId);

    Boolean pass(Workflow workflow, WorkflowArgs args, String loginUserId);

    Boolean cancel(Workflow workflow);

    Boolean back(Workflow workflow, WorkflowArgs args);

    Boolean abort(Workflow workflow, WorkflowUser firstUser);

    Boolean obsolete(Workflow workflow);
}
