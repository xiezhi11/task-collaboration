package com.taskcollab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.taskcollab.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
