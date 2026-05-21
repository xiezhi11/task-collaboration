package com.taskcollab.dto;

import lombok.Data;

@Data
public class TaskDragDTO {
    private Long taskId;
    private String targetStatus;
    private String rejectReason;
}
