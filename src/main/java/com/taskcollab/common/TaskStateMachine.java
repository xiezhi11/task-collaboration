package com.taskcollab.common;

import com.taskcollab.entity.Task;
import com.taskcollab.entity.TaskOperationLog;
import com.taskcollab.entity.TaskRejectRecord;
import com.taskcollab.service.TaskOperationLogService;
import com.taskcollab.service.TaskRejectRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class TaskStateMachine {

    @Autowired
    private TaskOperationLogService operationLogService;

    @Autowired
    private TaskRejectRecordService rejectRecordService;

    private static final Set<String> CAN_PUBLISH_STATES = Set.of(TaskStatus.DRAFT);
    private static final Set<String> CAN_ASSIGN_STATES = Set.of(
            TaskStatus.DRAFT, TaskStatus.PENDING_ASSIGN, TaskStatus.PENDING_START,
            TaskStatus.IN_PROGRESS, TaskStatus.REJECTED
    );
    private static final Set<String> CAN_UPDATE_PROGRESS_STATES = Set.of(
            TaskStatus.PENDING_START, TaskStatus.IN_PROGRESS, TaskStatus.REJECTED
    );
    private static final Set<String> CAN_SUBMIT_ACCEPT_STATES = Set.of(
            TaskStatus.PENDING_START, TaskStatus.IN_PROGRESS, TaskStatus.REJECTED
    );
    private static final Set<String> CAN_ACCEPT_PASS_STATES = Set.of(TaskStatus.PENDING_ACCEPT);
    private static final Set<String> CAN_ACCEPT_REJECT_STATES = Set.of(TaskStatus.PENDING_ACCEPT);

    private static final Set<String> DRAG_TO_IN_PROGRESS_STATES = Set.of(
            TaskStatus.PENDING_START, TaskStatus.REJECTED
    );
    private static final Set<String> DRAG_TO_PENDING_ACCEPT_STATES = Set.of(
            TaskStatus.PENDING_START, TaskStatus.IN_PROGRESS, TaskStatus.REJECTED
    );
    private static final Set<String> DRAG_TO_COMPLETED_STATES = Set.of(TaskStatus.PENDING_ACCEPT);
    private static final Set<String> DRAG_TO_REJECTED_STATES = Set.of(TaskStatus.PENDING_ACCEPT);
    private static final Set<String> DRAG_TO_PENDING_START_STATES = Set.of(TaskStatus.PENDING_ASSIGN);

    public boolean canPublish(Task task) {
        return CAN_PUBLISH_STATES.contains(task.getTaskStatus());
    }

    public boolean canAssign(Task task) {
        return CAN_ASSIGN_STATES.contains(task.getTaskStatus())
                && !TaskStatus.COMPLETED.equals(task.getTaskStatus());
    }

    public boolean canUpdateProgress(Task task) {
        return CAN_UPDATE_PROGRESS_STATES.contains(task.getTaskStatus());
    }

    public boolean canSubmitAccept(Task task) {
        return CAN_SUBMIT_ACCEPT_STATES.contains(task.getTaskStatus())
                && task.getProgress() != null && task.getProgress() == 100;
    }

    public boolean canAcceptPass(Task task) {
        return CAN_ACCEPT_PASS_STATES.contains(task.getTaskStatus());
    }

    public boolean canAcceptReject(Task task) {
        return CAN_ACCEPT_REJECT_STATES.contains(task.getTaskStatus());
    }

    public String getNextStatusAfterPublish(Task task) {
        return task.getExecutorId() != null ? TaskStatus.PENDING_START : TaskStatus.PENDING_ASSIGN;
    }

    public String getNextStatusAfterAssign(Task task) {
        if (TaskStatus.PENDING_ASSIGN.equals(task.getTaskStatus())) {
            return TaskStatus.PENDING_START;
        }
        return task.getTaskStatus();
    }

    public String getNextStatusAfterProgressUpdate(Task task, Integer newProgress) {
        if (TaskStatus.PENDING_START.equals(task.getTaskStatus()) && newProgress > 0) {
            return TaskStatus.IN_PROGRESS;
        }
        if (TaskStatus.REJECTED.equals(task.getTaskStatus())) {
            return TaskStatus.IN_PROGRESS;
        }
        return task.getTaskStatus();
    }

    public String getNextStatusAfterSubmitAccept() {
        return TaskStatus.PENDING_ACCEPT;
    }

    public String getNextStatusAfterAcceptPass() {
        return TaskStatus.COMPLETED;
    }

    public String getNextStatusAfterAcceptReject() {
        return TaskStatus.REJECTED;
    }

    public void transitionStatus(Task task, String operationType, String operationContent,
                                 String oldStatus, String newStatus) {
        task.setTaskStatus(newStatus);
        operationLogService.logOperation(
                task.getId(), operationType, operationContent, oldStatus, newStatus
        );
    }

    public void logRejectRecord(Long taskId, String rejectReason) {
        TaskRejectRecord rejectRecord = new TaskRejectRecord();
        rejectRecord.setTaskId(taskId);
        rejectRecord.setRejectReason(rejectReason);
        rejectRecord.setRejectById(UserContext.getUserId());
        rejectRecord.setRejectByName(UserContext.getUserName());
        rejectRecordService.save(rejectRecord);

        operationLogService.logRejectReason(taskId, rejectReason);
    }

    public String getStatusText(String status) {
        switch (status) {
            case TaskStatus.DRAFT: return "草稿";
            case TaskStatus.PUBLISHED: return "已发布";
            case TaskStatus.PENDING_ASSIGN: return "待分配";
            case TaskStatus.PENDING_START: return "待开始";
            case TaskStatus.IN_PROGRESS: return "进行中";
            case TaskStatus.PENDING_ACCEPT: return "待验收";
            case TaskStatus.REJECTED: return "已驳回";
            case TaskStatus.COMPLETED: return "已完成";
            default: return status;
        }
    }

    public boolean canDragToStatus(Task task, String targetStatus) {
        String currentStatus = task.getTaskStatus();
        if (TaskStatus.COMPLETED.equals(currentStatus)) {
            return false;
        }
        switch (targetStatus) {
            case TaskStatus.PENDING_START:
                return DRAG_TO_PENDING_START_STATES.contains(currentStatus)
                        && task.getExecutorId() != null;
            case TaskStatus.IN_PROGRESS:
                return DRAG_TO_IN_PROGRESS_STATES.contains(currentStatus);
            case TaskStatus.PENDING_ACCEPT:
                return DRAG_TO_PENDING_ACCEPT_STATES.contains(currentStatus)
                        && task.getProgress() != null && task.getProgress() == 100;
            case TaskStatus.COMPLETED:
                return DRAG_TO_COMPLETED_STATES.contains(currentStatus);
            case TaskStatus.REJECTED:
                return DRAG_TO_REJECTED_STATES.contains(currentStatus);
            default:
                return false;
        }
    }

    public String getDragOperationType(String targetStatus) {
        switch (targetStatus) {
            case TaskStatus.PENDING_START: return "DRAG_TO_PENDING_START";
            case TaskStatus.IN_PROGRESS: return "DRAG_TO_IN_PROGRESS";
            case TaskStatus.PENDING_ACCEPT: return "DRAG_TO_PENDING_ACCEPT";
            case TaskStatus.COMPLETED: return "DRAG_TO_COMPLETED";
            case TaskStatus.REJECTED: return "DRAG_TO_REJECTED";
            default: return "DRAG";
        }
    }

    public String getDragOperationContent(String targetStatus) {
        switch (targetStatus) {
            case TaskStatus.PENDING_START: return "拖拽至待开始";
            case TaskStatus.IN_PROGRESS: return "拖拽至进行中";
            case TaskStatus.PENDING_ACCEPT: return "拖拽至待验收";
            case TaskStatus.COMPLETED: return "拖拽至已完成";
            case TaskStatus.REJECTED: return "拖拽至已驳回";
            default: return "拖拽变更状态";
        }
    }

    public boolean isLeaderOperation(String targetStatus) {
        return TaskStatus.PENDING_START.equals(targetStatus)
                || TaskStatus.COMPLETED.equals(targetStatus)
                || TaskStatus.REJECTED.equals(targetStatus);
    }

    public boolean isExecutorOperation(String targetStatus) {
        return TaskStatus.IN_PROGRESS.equals(targetStatus)
                || TaskStatus.PENDING_ACCEPT.equals(targetStatus);
    }
}
