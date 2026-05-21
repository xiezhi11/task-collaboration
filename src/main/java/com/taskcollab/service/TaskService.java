package com.taskcollab.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taskcollab.common.Result;
import com.taskcollab.common.TaskStateMachine;
import com.taskcollab.common.TaskStatus;
import com.taskcollab.common.UserContext;
import com.taskcollab.dto.TaskAcceptDTO;
import com.taskcollab.dto.TaskCreateDTO;
import com.taskcollab.dto.TaskDragDTO;
import com.taskcollab.dto.TaskProgressDTO;
import com.taskcollab.dto.TaskQueryDTO;
import com.taskcollab.dto.TaskUpdateDTO;
import com.taskcollab.entity.SysUser;
import com.taskcollab.entity.Task;
import com.taskcollab.entity.TaskOperationLog;
import com.taskcollab.entity.TaskRejectRecord;
import com.taskcollab.mapper.TaskMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TaskService extends ServiceImpl<TaskMapper, Task> {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private TaskOperationLogService operationLogService;

    @Autowired
    private TaskRejectRecordService rejectRecordService;

    @Autowired
    private TaskStateMachine stateMachine;

    @Transactional
    public Result<Task> createTask(TaskCreateDTO dto) {
        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getStartDate().isAfter(dto.getEndDate())) {
                return Result.error("开始日期不能晚于截止日期");
            }
        }

        Task task = new Task();
        task.setProjectName(dto.getProjectName());
        task.setTaskTitle(dto.getTaskTitle());
        task.setTaskDescription(dto.getTaskDescription());
        task.setCreatorId(UserContext.getUserId());
        task.setCreatorName(UserContext.getUserName());
        task.setProgress(0);
        task.setPriority(StringUtils.hasText(dto.getPriority()) ? dto.getPriority() : "MEDIUM");
        task.setIsOverdue(0);
        task.setWasOverdue(0);
        task.setTaskStatus(TaskStatus.DRAFT);

        if (dto.getStartDate() != null) {
            task.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            task.setEndDate(dto.getEndDate());
        }

        if (dto.getExecutorId() != null) {
            SysUser executor = sysUserService.getById(dto.getExecutorId());
            if (executor == null) {
                return Result.error("执行人不存在");
            }
            task.setExecutorId(executor.getId());
            task.setExecutorName(executor.getName());
        }

        save(task);

        operationLogService.logOperation(task.getId(), "CREATE", "创建任务草稿", null, task.getTaskStatus());

        return Result.success(task);
    }

    @Transactional
    public Result<Task> publishTask(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!isTaskCreatorOrLeader(task)) {
            return Result.error("只有负责人或管理员可以发布任务");
        }

        if (!stateMachine.canPublish(task)) {
            return Result.error("只有草稿状态的任务可以发布，当前状态：" + stateMachine.getStatusText(task.getTaskStatus()));
        }

        String oldStatus = task.getTaskStatus();
        String newStatus = stateMachine.getNextStatusAfterPublish(task);

        stateMachine.transitionStatus(task, "PUBLISH", "发布任务", oldStatus, newStatus);
        updateById(task);

        return Result.success(task);
    }

    @Transactional
    public Result<Task> assignTask(Long taskId, Long executorId) {
        Task task = getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!isTaskCreatorOrLeader(task)) {
            return Result.error("只有负责人或管理员可以分配任务");
        }

        if (!stateMachine.canAssign(task)) {
            return Result.error("当前状态不允许分配任务，当前状态：" + stateMachine.getStatusText(task.getTaskStatus()));
        }

        SysUser executor = sysUserService.getById(executorId);
        if (executor == null) {
            return Result.error("执行人不存在");
        }

        String oldStatus = task.getTaskStatus();
        String oldExecutor = task.getExecutorName();

        task.setExecutorId(executor.getId());
        task.setExecutorName(executor.getName());

        String newStatus = stateMachine.getNextStatusAfterAssign(task);
        String content = oldExecutor != null
                ? "重新分配任务：从 " + oldExecutor + " 改为 " + executor.getName()
                : "分配任务给 " + executor.getName();

        stateMachine.transitionStatus(task, "ASSIGN", content, oldStatus, newStatus);
        updateById(task);

        return Result.success(task);
    }

    @Transactional
    public Result<Task> updateProgress(TaskProgressDTO dto) {
        Task task = getById(dto.getTaskId());
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!isTaskExecutor(task)) {
            return Result.error("只有任务执行人可以更新进度");
        }

        if (!stateMachine.canUpdateProgress(task)) {
            String statusText = stateMachine.getStatusText(task.getTaskStatus());
            if (TaskStatus.COMPLETED.equals(task.getTaskStatus())) {
                return Result.error("已完成的任务不能修改进度");
            }
            if (TaskStatus.PENDING_ACCEPT.equals(task.getTaskStatus())) {
                return Result.error("待验收状态的任务不能修改进度，请等待验收结果或被驳回后再修改");
            }
            return Result.error("当前状态不允许修改进度，当前状态：" + statusText);
        }

        if (dto.getProgress() < 0 || dto.getProgress() > 100) {
            return Result.error("进度必须在0到100之间");
        }

        Integer oldProgress = task.getProgress();
        task.setProgress(dto.getProgress());

        String oldStatus = task.getTaskStatus();
        String newStatus = stateMachine.getNextStatusAfterProgressUpdate(task, dto.getProgress());

        if (!oldStatus.equals(newStatus)) {
            stateMachine.transitionStatus(task, "UPDATE_PROGRESS",
                    "更新进度：从 " + oldProgress + "% 到 " + dto.getProgress() + "%",
                    oldStatus, newStatus);
        } else {
            operationLogService.logOperation(task.getId(), "UPDATE_PROGRESS",
                    "更新进度：从 " + oldProgress + "% 到 " + dto.getProgress() + "%",
                    null, null);
        }

        updateById(task);
        return Result.success(task);
    }

    @Transactional
    public Result<Task> submitAccept(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!isTaskExecutor(task)) {
            return Result.error("只有任务执行人可以提交验收");
        }

        if (TaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            return Result.error("已完成的任务不能重复提交验收");
        }

        if (TaskStatus.PENDING_ACCEPT.equals(task.getTaskStatus())) {
            return Result.error("任务已在待验收状态，请勿重复提交");
        }

        if (task.getProgress() == null || task.getProgress() < 100) {
            return Result.error("任务进度必须达到100%才能提交验收，当前进度：" + (task.getProgress() != null ? task.getProgress() : 0) + "%");
        }

        if (!stateMachine.canSubmitAccept(task)) {
            return Result.error("当前状态不允许提交验收，当前状态：" + stateMachine.getStatusText(task.getTaskStatus()));
        }

        String oldStatus = task.getTaskStatus();
        String newStatus = stateMachine.getNextStatusAfterSubmitAccept();

        stateMachine.transitionStatus(task, "SUBMIT_ACCEPT", "提交验收", oldStatus, newStatus);
        updateById(task);

        return Result.success(task);
    }

    @Transactional
    public Result<Task> acceptPass(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!isTaskCreatorOrLeader(task)) {
            return Result.error("只有负责人或管理员可以验收任务");
        }

        if (!stateMachine.canAcceptPass(task)) {
            return Result.error("只有待验收状态的任务可以验收通过，当前状态：" + stateMachine.getStatusText(task.getTaskStatus()));
        }

        String oldStatus = task.getTaskStatus();
        String newStatus = stateMachine.getNextStatusAfterAcceptPass();

        task.setAcceptResult("PASS");
        stateMachine.transitionStatus(task, "ACCEPT_PASS", "验收通过", oldStatus, newStatus);
        updateById(task);

        return Result.success(task);
    }

    @Transactional
    public Result<Task> acceptReject(TaskAcceptDTO dto) {
        Task task = getById(dto.getTaskId());
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!isTaskCreatorOrLeader(task)) {
            return Result.error("只有负责人或管理员可以驳回任务");
        }

        if (!stateMachine.canAcceptReject(task)) {
            return Result.error("只有待验收状态的任务可以驳回，当前状态：" + stateMachine.getStatusText(task.getTaskStatus()));
        }

        if (!StringUtils.hasText(dto.getRejectReason())) {
            return Result.error("驳回原因不能为空");
        }

        String oldStatus = task.getTaskStatus();
        String newStatus = stateMachine.getNextStatusAfterAcceptReject();

        task.setAcceptResult("REJECT");
        stateMachine.transitionStatus(task, "ACCEPT_REJECT", "验收驳回", oldStatus, newStatus);
        updateById(task);

        stateMachine.logRejectRecord(task.getId(), dto.getRejectReason());

        return Result.success(task);
    }

    @Transactional
    public Result<Task> updateTask(Long taskId, TaskUpdateDTO dto) {
        Task task = getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!isTaskCreatorOrLeader(task)) {
            return Result.error("只有负责人或管理员可以修改任务信息");
        }

        if (TaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            return Result.error("已完成的任务不能修改基础信息");
        }

        if (dto.getStartDate() != null && dto.getEndDate() != null) {
            if (dto.getStartDate().isAfter(dto.getEndDate())) {
                return Result.error("开始日期不能晚于截止日期");
            }
        }

        LocalDate startDate = dto.getStartDate() != null ? dto.getStartDate() : task.getStartDate();
        LocalDate endDate = dto.getEndDate() != null ? dto.getEndDate() : task.getEndDate();
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return Result.error("开始日期不能晚于截止日期");
        }

        if (dto.getProjectName() != null) task.setProjectName(dto.getProjectName());
        if (dto.getTaskTitle() != null) task.setTaskTitle(dto.getTaskTitle());
        if (dto.getTaskDescription() != null) task.setTaskDescription(dto.getTaskDescription());
        if (dto.getStartDate() != null) task.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) task.setEndDate(dto.getEndDate());
        if (dto.getPriority() != null) task.setPriority(dto.getPriority());

        updateById(task);

        operationLogService.logOperation(taskId, "UPDATE", "修改任务基础信息", null, null);

        return Result.success(task);
    }

    public Result<Page<Task>> queryTasks(TaskQueryDTO dto) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(dto.getProjectName())) {
            wrapper.like(Task::getProjectName, dto.getProjectName());
        }
        if (dto.getCreatorId() != null) {
            wrapper.eq(Task::getCreatorId, dto.getCreatorId());
        }
        if (dto.getExecutorId() != null) {
            wrapper.eq(Task::getExecutorId, dto.getExecutorId());
        }
        if (StringUtils.hasText(dto.getPriority())) {
            wrapper.eq(Task::getPriority, dto.getPriority());
        }
        if (StringUtils.hasText(dto.getTaskStatus())) {
            wrapper.eq(Task::getTaskStatus, dto.getTaskStatus());
        }
        if (dto.getStartDateBegin() != null) {
            wrapper.ge(Task::getStartDate, dto.getStartDateBegin());
        }
        if (dto.getStartDateEnd() != null) {
            wrapper.le(Task::getStartDate, dto.getStartDateEnd());
        }
        if (dto.getEndDateBegin() != null) {
            wrapper.ge(Task::getEndDate, dto.getEndDateBegin());
        }
        if (dto.getEndDateEnd() != null) {
            wrapper.le(Task::getEndDate, dto.getEndDateEnd());
        }

        if (dto.getIsOverdue() != null) {
            LocalDate today = LocalDate.now();
            if (dto.getIsOverdue() == 1) {
                wrapper.and(w -> w.ne(Task::getTaskStatus, TaskStatus.COMPLETED)
                        .lt(Task::getEndDate, today));
            } else {
                wrapper.and(w -> w.eq(Task::getTaskStatus, TaskStatus.COMPLETED)
                        .or().ge(Task::getEndDate, today)
                        .or().isNull(Task::getEndDate));
            }
        }

        if (!UserContext.isLeader() && !"ADMIN".equals(UserContext.getUserRole())) {
            wrapper.and(w -> w.eq(Task::getCreatorId, UserContext.getUserId())
                    .or().eq(Task::getExecutorId, UserContext.getUserId()));
        }

        wrapper.orderByDesc(Task::getCreateTime);

        Page<Task> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        Page<Task> result = page(page, wrapper);

        result.getRecords().forEach(this::checkOverdue);

        return Result.success(result);
    }

    public Result<Map<String, Object>> getTaskDetail(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (!UserContext.isLeader() && !"ADMIN".equals(UserContext.getUserRole())) {
            if (!task.getCreatorId().equals(UserContext.getUserId())
                    && (task.getExecutorId() == null || !task.getExecutorId().equals(UserContext.getUserId()))) {
                return Result.error("无权查看该任务详情");
            }
        }

        checkOverdue(task);

        List<TaskOperationLog> logs = operationLogService.lambdaQuery()
                .eq(TaskOperationLog::getTaskId, taskId)
                .orderByDesc(TaskOperationLog::getCreateTime)
                .list();

        List<TaskRejectRecord> rejects = rejectRecordService.lambdaQuery()
                .eq(TaskRejectRecord::getTaskId, taskId)
                .orderByDesc(TaskRejectRecord::getCreateTime)
                .list();

        Map<String, Object> result = new HashMap<>();
        result.put("task", task);
        result.put("operationLogs", logs);
        result.put("rejectRecords", rejects);

        return Result.success(result);
    }

    public Result<Map<String, List<Task>>> getKanban(TaskQueryDTO dto) {
        dto.setPageSize(1000);
        Page<Task> pageResult = queryTasks(dto).getData();
        List<Task> tasks = pageResult.getRecords();

        Map<String, List<Task>> kanban = new HashMap<>();
        kanban.put(TaskStatus.PENDING_START, new java.util.ArrayList<>());
        kanban.put(TaskStatus.IN_PROGRESS, new java.util.ArrayList<>());
        kanban.put(TaskStatus.PENDING_ACCEPT, new java.util.ArrayList<>());
        kanban.put(TaskStatus.REJECTED, new java.util.ArrayList<>());
        kanban.put(TaskStatus.COMPLETED, new java.util.ArrayList<>());
        kanban.put(TaskStatus.PENDING_ASSIGN, new java.util.ArrayList<>());

        for (Task task : tasks) {
            checkOverdue(task);
            String status = task.getTaskStatus();
            if (kanban.containsKey(status)) {
                kanban.get(status).add(task);
            }
        }

        return Result.success(kanban);
    }

    private void checkOverdue(Task task) {
        if (TaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            task.setIsOverdue(0);
            return;
        }

        if (task.getEndDate() != null) {
            LocalDate today = LocalDate.now();
            boolean isOverdue = today.isAfter(task.getEndDate());
            task.setIsOverdue(isOverdue ? 1 : 0);
            if (isOverdue && (task.getWasOverdue() == null || task.getWasOverdue() == 0)) {
                task.setWasOverdue(1);
                updateById(task);
            }
        }
    }

    public Result<List<TaskOperationLog>> getOperationLogs(Long taskId) {
        Task task = getById(taskId);
        if (task == null) {
            return Result.error("任务不存在");
        }

        List<TaskOperationLog> logs = operationLogService.lambdaQuery()
                .eq(TaskOperationLog::getTaskId, taskId)
                .orderByDesc(TaskOperationLog::getCreateTime)
                .list();

        return Result.success(logs);
    }

    private boolean isTaskCreatorOrLeader(Task task) {
        return UserContext.isLeader() || task.getCreatorId().equals(UserContext.getUserId());
    }

    private boolean isTaskExecutor(Task task) {
        return task.getExecutorId() != null && task.getExecutorId().equals(UserContext.getUserId());
    }

    @Transactional
    public Result<Task> dragUpdateStatus(TaskDragDTO dto) {
        Task task = getById(dto.getTaskId());
        if (task == null) {
            return Result.error("任务不存在");
        }

        if (TaskStatus.COMPLETED.equals(task.getTaskStatus())) {
            return Result.error("已完成的任务不能通过拖拽变更状态");
        }

        if (!stateMachine.canDragToStatus(task, dto.getTargetStatus())) {
            return Result.error("不允许从 " + stateMachine.getStatusText(task.getTaskStatus())
                    + " 拖拽到 " + stateMachine.getStatusText(dto.getTargetStatus()));
        }

        if (stateMachine.isLeaderOperation(dto.getTargetStatus())) {
            if (!isTaskCreatorOrLeader(task)) {
                return Result.error("只有负责人或管理员可以拖拽到 " + stateMachine.getStatusText(dto.getTargetStatus()) + " 状态");
            }
        }

        if (stateMachine.isExecutorOperation(dto.getTargetStatus())) {
            if (!isTaskExecutor(task)) {
                return Result.error("只有任务执行人可以拖拽到 " + stateMachine.getStatusText(dto.getTargetStatus()) + " 状态");
            }
        }

        if (TaskStatus.REJECTED.equals(dto.getTargetStatus()) && !StringUtils.hasText(dto.getRejectReason())) {
            return Result.error("拖拽到已驳回状态必须填写驳回原因");
        }

        if (TaskStatus.PENDING_ACCEPT.equals(dto.getTargetStatus())) {
            if (task.getProgress() == null || task.getProgress() < 100) {
                return Result.error("任务进度必须达到100%才能拖拽到待验收，当前进度：" + (task.getProgress() != null ? task.getProgress() : 0) + "%");
            }
        }

        String oldStatus = task.getTaskStatus();
        String newStatus = dto.getTargetStatus();
        String operationType = stateMachine.getDragOperationType(newStatus);
        String operationContent = stateMachine.getDragOperationContent(newStatus);

        if (TaskStatus.COMPLETED.equals(newStatus)) {
            task.setAcceptResult("PASS");
        } else if (TaskStatus.REJECTED.equals(newStatus)) {
            task.setAcceptResult("REJECT");
        }

        if (TaskStatus.IN_PROGRESS.equals(newStatus) && (task.getProgress() == null || task.getProgress() == 0)) {
            task.setProgress(1);
        }

        stateMachine.transitionStatus(task, operationType, operationContent, oldStatus, newStatus);
        updateById(task);

        if (TaskStatus.REJECTED.equals(newStatus)) {
            stateMachine.logRejectRecord(task.getId(), dto.getRejectReason());
        }

        checkOverdue(task);

        return Result.success(task);
    }
}
