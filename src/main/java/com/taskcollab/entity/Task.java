package com.taskcollab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String projectName;

    private String taskTitle;

    private String taskDescription;

    private Long creatorId;

    private String creatorName;

    private Long executorId;

    private String executorName;

    private LocalDate startDate;

    private LocalDate endDate;

    private Integer progress;

    private String priority;

    private String taskStatus;

    private Integer isOverdue;

    private Integer wasOverdue;

    private String acceptResult;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
