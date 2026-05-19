package com.taskcollab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_operation_log")
public class TaskOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private Long operatorId;

    private String operatorName;

    private String operationType;

    private String operationContent;

    private String rejectReason;

    private String oldStatus;

    private String newStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
