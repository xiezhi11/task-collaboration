package com.taskcollab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taskcollab.common.UserContext;
import com.taskcollab.entity.TaskOperationLog;
import com.taskcollab.mapper.TaskOperationLogMapper;
import org.springframework.stereotype.Service;

@Service
public class TaskOperationLogService extends ServiceImpl<TaskOperationLogMapper, TaskOperationLog> {

    public void logOperation(Long taskId, String operationType, String operationContent,
                             String oldStatus, String newStatus) {
        TaskOperationLog log = new TaskOperationLog();
        log.setTaskId(taskId);
        log.setOperatorId(UserContext.getUserId());
        log.setOperatorName(UserContext.getUserName());
        log.setOperationType(operationType);
        log.setOperationContent(operationContent);
        log.setOldStatus(oldStatus);
        log.setNewStatus(newStatus);
        save(log);
    }

    public void logRejectReason(Long taskId, String rejectReason) {
        TaskOperationLog log = new TaskOperationLog();
        log.setTaskId(taskId);
        log.setOperatorId(UserContext.getUserId());
        log.setOperatorName(UserContext.getUserName());
        log.setOperationType("REJECT_REASON");
        log.setRejectReason(rejectReason);
        save(log);
    }
}
