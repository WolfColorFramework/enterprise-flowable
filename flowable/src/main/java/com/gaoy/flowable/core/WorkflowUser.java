package com.gaoy.flowable.core;

import java.util.List;

public interface WorkflowUser {

    String getUserId();

    String getUserName();

    List<WorkflowUser> UsersByIds(List<String> userIds);
}
