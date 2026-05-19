package com.taskcollab.dto;

import lombok.Data;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
public class TaskProgressDTO {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotNull(message = "进度不能为空")
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private Integer progress;
}
