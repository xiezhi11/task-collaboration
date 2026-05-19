package com.taskcollab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("task_reject_record")
public class TaskRejectRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String rejectReason;

    private Long rejectById;

    private String rejectByName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
