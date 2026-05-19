package com.taskcollab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskcollab.entity.TaskOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskOperationLogMapper extends BaseMapper<TaskOperationLog> {
}
