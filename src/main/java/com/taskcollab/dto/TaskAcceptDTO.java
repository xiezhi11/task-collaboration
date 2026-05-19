package com.taskcollab.dto;

import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class TaskAcceptDTO {
    @NotNull(message = "任务ID不能为空")
    private Long taskId;

    @NotBlank(message = "驳回原因不能为空")
    private String rejectReason;
}
