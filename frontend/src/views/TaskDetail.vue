<template>
  <div class="task-detail-container">
    <div class="page-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2>任务详情</h2>
      <div class="action-buttons">
        <el-button
          v-if="canPublish"
          type="primary"
          @click="handlePublish"
        >发布任务</el-button>
        <el-button
          v-if="canAssign"
          type="primary"
          @click="openAssignDialog"
        >分配任务</el-button>
        <el-button
          v-if="canUpdateProgress"
          type="success"
          @click="openProgressDialog"
        >更新进度</el-button>
        <el-button
          v-if="canSubmitAccept"
          type="warning"
          @click="handleSubmitAccept"
        >提交验收</el-button>
        <el-button
          v-if="canAcceptPass"
          type="success"
          @click="handleAcceptPass"
        >验收通过</el-button>
        <el-button
          v-if="canAcceptReject"
          type="danger"
          @click="openRejectDialog"
        >验收驳回</el-button>
      </div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>基本信息</span>
              <el-tag :color="STATUS_COLOR[task.taskStatus]" effect="dark" size="large">
                {{ STATUS_TEXT[task.taskStatus] }}
              </el-tag>
            </div>
          </template>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="项目名称">{{ task.projectName }}</el-descriptions-item>
            <el-descriptions-item label="任务标题">{{ task.taskTitle }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ task.creatorName }}</el-descriptions-item>
            <el-descriptions-item label="执行人">{{ task.executorName || '未分配' }}</el-descriptions-item>
            <el-descriptions-item label="开始日期">{{ task.startDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="截止日期">
              <span :class="{ 'overdue-text': task.isOverdue === 1 && task.taskStatus !== 'COMPLETED' }">
                {{ task.endDate || '-' }}
              </span>
              <el-tag v-if="task.isOverdue === 1 && task.taskStatus !== 'COMPLETED'" type="danger" size="small" style="margin-left: 8px;">已逾期</el-tag>
              <el-tag v-else-if="task.wasOverdue === 1" type="warning" size="small" style="margin-left: 8px;">曾逾期</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="进度">
              <el-progress :percentage="task.progress || 0" :status="task.progress === 100 ? 'success' : ''" />
            </el-descriptions-item>
            <el-descriptions-item label="优先级">
              <el-tag :type="getPriorityTagType(task.priority)">{{ PRIORITY_TEXT[task.priority] }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="任务描述" :span="2">
              {{ task.taskDescription || '-' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card class="logs-card">
          <template #header>操作记录</template>
          <el-timeline>
            <el-timeline-item
              v-for="log in operationLogs"
              :key="log.id"
              :timestamp="formatTime(log.createTime)"
              :color="getLogColor(log.operationType)"
            >
              <el-card shadow="hover">
                <h4>{{ log.operatorName }} - {{ getOperationText(log.operationType) }}</h4>
                <p v-if="log.operationContent">{{ log.operationContent }}</p>
                <p v-if="log.rejectReason" style="color: #f56c6c;">驳回原因: {{ log.rejectReason }}</p>
                <p v-if="log.oldStatus && log.newStatus" style="color: #909399;">
                  状态: {{ STATUS_TEXT[log.oldStatus] }} → {{ STATUS_TEXT[log.newStatus] }}
                </p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-if="operationLogs.length === 0" description="暂无操作记录" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="reject-card">
          <template #header>驳回记录</template>
          <div v-if="rejectRecords.length > 0" class="reject-list">
            <div v-for="record in rejectRecords" :key="record.id" class="reject-item">
              <div class="reject-header">
                <span class="reject-by">{{ record.rejectByName }}</span>
                <span class="reject-time">{{ formatTime(record.createTime) }}</span>
              </div>
              <p class="reject-reason">{{ record.rejectReason }}</p>
            </div>
          </div>
          <el-empty v-else description="暂无驳回记录" />
        </el-card>

        <el-card class="info-card" style="margin-top: 16px;">
          <template #header>验收结果</template>
          <div v-if="task.acceptResult" class="accept-result">
            <el-tag :type="task.acceptResult === 'PASS' ? 'success' : 'danger'" size="large">
              {{ task.acceptResult === 'PASS' ? '验收通过' : '验收驳回' }}
            </el-tag>
          </div>
          <div v-else>
            <el-tag type="info" size="large">未验收</el-tag>
          </div>
        </el-card>

        <el-card class="info-card" style="margin-top: 16px;">
          <template #header>时间信息</template>
          <el-descriptions :column="1">
            <el-descriptions-item label="创建时间">{{ formatTime(task.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatTime(task.updateTime) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="assignDialogVisible" title="分配任务" width="400px">
      <el-form label-width="80px">
        <el-form-item label="执行人" required>
          <el-select v-model="assignExecutorId" placeholder="请选择执行人">
            <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assignDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAssign">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="progressDialogVisible" title="更新进度" width="400px">
      <el-form label-width="80px">
        <el-form-item label="进度" required>
          <el-slider v-model="progressValue" :min="0" :max="100" show-stops />
          <div style="text-align: center; font-size: 18px; font-weight: bold;">{{ progressValue }}%</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="progressDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleUpdateProgress">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectDialogVisible" title="验收驳回" width="400px">
      <el-form label-width="80px">
        <el-form-item label="驳回原因" required>
          <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写驳回原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleAcceptReject">确定驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { STATUS_TEXT, STATUS_COLOR, PRIORITY_TEXT } from '../utils/constants'
import {
  getTaskDetail,
  publishTask,
  assignTask,
  updateProgress,
  submitAccept,
  acceptPass,
  acceptReject
} from '../api/task'
import { getUserList } from '../api/auth'
import { getCurrentUser } from '../utils/storage'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const taskId = route.params.id

const task = ref({})
const operationLogs = ref([])
const rejectRecords = ref([])
const users = ref([])

const currentUser = computed(() => getCurrentUser() || {})
const isLeader = computed(() => currentUser.value.role === 'LEADER' || currentUser.value.role === 'ADMIN')

const canPublish = computed(() => {
  return isLeader.value &&
         task.value.taskStatus === 'DRAFT' &&
         (task.value.creatorId === currentUser.value.userId || isLeader.value)
})

const canAssign = computed(() => {
  return isLeader.value &&
         task.value.taskStatus !== 'COMPLETED' &&
         (task.value.creatorId === currentUser.value.userId || isLeader.value)
})

const canUpdateProgress = computed(() => {
  if (task.value.taskStatus === 'COMPLETED' || task.value.taskStatus === 'PENDING_ACCEPT') {
    return false
  }
  if (isLeader.value) {
    return true
  }
  return task.value.executorId === currentUser.value.userId
})

const canSubmitAccept = computed(() => {
  return task.value.executorId === currentUser.value.userId &&
         task.value.progress === 100 &&
         (task.value.taskStatus === 'IN_PROGRESS' ||
          task.value.taskStatus === 'PENDING_START' ||
          task.value.taskStatus === 'REJECTED')
})

const canAcceptPass = computed(() => {
  return isLeader.value &&
         task.value.taskStatus === 'PENDING_ACCEPT' &&
         (task.value.creatorId === currentUser.value.userId || isLeader.value)
})

const canAcceptReject = computed(() => {
  return isLeader.value &&
         task.value.taskStatus === 'PENDING_ACCEPT' &&
         (task.value.creatorId === currentUser.value.userId || isLeader.value)
})

const assignDialogVisible = ref(false)
const assignExecutorId = ref(null)

const progressDialogVisible = ref(false)
const progressValue = ref(0)

const rejectDialogVisible = ref(false)
const rejectReason = ref('')

onMounted(() => {
  fetchUsers()
  fetchDetail()
})

const fetchUsers = async () => {
  try {
    const res = await getUserList()
    if (res.code === 200) {
      users.value = res.data
    }
  } catch (e) {
    console.error(e)
  }
}

const fetchDetail = async () => {
  try {
    const res = await getTaskDetail(taskId)
    if (res.code === 200) {
      task.value = res.data.task
      operationLogs.value = res.data.operationLogs || []
      rejectRecords.value = res.data.rejectRecords || []
    }
  } catch (e) {
    console.error(e)
  }
}

const goBack = () => {
  router.back()
}

const handlePublish = async () => {
  try {
    await ElMessageBox.confirm('确定要发布该任务吗？', '确认发布', {
      type: 'warning'
    })
    const res = await publishTask(taskId)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      fetchDetail()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const openAssignDialog = () => {
  assignExecutorId.value = task.value.executorId
  assignDialogVisible.value = true
}

const handleAssign = async () => {
  if (!assignExecutorId.value) {
    ElMessage.warning('请选择执行人')
    return
  }
  try {
    const res = await assignTask(taskId, assignExecutorId.value)
    if (res.code === 200) {
      ElMessage.success('分配成功')
      assignDialogVisible.value = false
      fetchDetail()
    }
  } catch (e) {
    console.error(e)
  }
}

const openProgressDialog = () => {
  progressValue.value = task.value.progress || 0
  progressDialogVisible.value = true
}

const handleUpdateProgress = async () => {
  try {
    const res = await updateProgress({
      taskId: taskId,
      progress: progressValue.value
    })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      progressDialogVisible.value = false
      fetchDetail()
    }
  } catch (e) {
    console.error(e)
  }
}

const handleSubmitAccept = async () => {
  try {
    await ElMessageBox.confirm('确定要提交验收吗？', '确认提交', {
      type: 'warning'
    })
    const res = await submitAccept(taskId)
    if (res.code === 200) {
      ElMessage.success('提交成功')
      fetchDetail()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const handleAcceptPass = async () => {
  try {
    await ElMessageBox.confirm('确定要验收通过吗？', '确认通过', {
      type: 'success'
    })
    const res = await acceptPass(taskId)
    if (res.code === 200) {
      ElMessage.success('验收通过')
      fetchDetail()
    }
  } catch (e) {
    if (e !== 'cancel') {
      console.error(e)
    }
  }
}

const openRejectDialog = () => {
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const handleAcceptReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    const res = await acceptReject({
      taskId: taskId,
      rejectReason: rejectReason.value
    })
    if (res.code === 200) {
      ElMessage.success('驳回成功')
      rejectDialogVisible.value = false
      fetchDetail()
    }
  } catch (e) {
    console.error(e)
  }
}

const formatTime = (time) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

const getPriorityTagType = (priority) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }
  return map[priority] || 'info'
}

const getLogColor = (operationType) => {
  const colorMap = {
    CREATE: '#909399',
    PUBLISH: '#409EFF',
    ASSIGN: '#E6A23C',
    UPDATE_PROGRESS: '#67C23A',
    SUBMIT_ACCEPT: '#409EFF',
    ACCEPT_PASS: '#67C23A',
    ACCEPT_REJECT: '#F56C6C',
    UPDATE: '#909399',
    REJECT_REASON: '#F56C6C'
  }
  return colorMap[operationType] || '#909399'
}

const getOperationText = (operationType) => {
  const textMap = {
    CREATE: '创建任务',
    PUBLISH: '发布任务',
    ASSIGN: '分配任务',
    UPDATE_PROGRESS: '更新进度',
    SUBMIT_ACCEPT: '提交验收',
    ACCEPT_PASS: '验收通过',
    ACCEPT_REJECT: '验收驳回',
    UPDATE: '修改任务',
    REJECT_REASON: '驳回记录'
  }
  return textMap[operationType] || operationType
}
</script>

<style scoped>
.task-detail-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.page-header h2 {
  margin: 0;
  flex: 1;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.info-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logs-card {
  margin-top: 16px;
}

.reject-card .reject-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reject-item {
  padding: 12px;
  background-color: #fef0f0;
  border-radius: 4px;
  border-left: 3px solid #f56c6c;
}

.reject-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.reject-by {
  font-weight: bold;
  color: #f56c6c;
}

.reject-time {
  color: #909399;
  font-size: 12px;
}

.reject-reason {
  margin: 0;
  color: #606266;
  line-height: 1.5;
}

.overdue-text {
  color: #f56c6c;
  font-weight: bold;
}

.accept-result {
  text-align: center;
  padding: 16px 0;
}
</style>
