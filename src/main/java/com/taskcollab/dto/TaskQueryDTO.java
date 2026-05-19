package com.taskcollab.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskQueryDTO {
    private String projectName;
    private Long creatorId;
    private Long executorId;
    private String priority;
    private String taskStatus;
    private Integer isOverdue;
    private LocalDate startDateBegin;
    private LocalDate startDateEnd;
    private LocalDate endDateBegin;
    private LocalDate endDateEnd;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
