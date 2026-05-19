package com.taskcollab.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.taskcollab.entity.TaskRejectRecord;
import com.taskcollab.mapper.TaskRejectRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class TaskRejectRecordService extends ServiceImpl<TaskRejectRecordMapper, TaskRejectRecord> {
}
