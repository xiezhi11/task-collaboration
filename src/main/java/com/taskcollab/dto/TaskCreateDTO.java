package com.taskcollab.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class TaskCreateDTO {
    @NotBlank(message = "项目名称不能为空")
    private String projectName;

    @NotBlank(message = "任务标题不能为空")
    private String taskTitle;

    private String taskDescription;

    private Long executorId;

    private LocalDate startDate;

    private LocalDate endDate;

    private String priority;
}
