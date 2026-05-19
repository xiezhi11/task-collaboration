package com.taskcollab.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class TaskUpdateDTO {
    private String projectName;
    private String taskTitle;
    private String taskDescription;
    private Long executorId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String priority;
}
