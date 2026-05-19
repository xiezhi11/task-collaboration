<template>
  <div class="kanban-container">
    <div class="page-header">
      <h2>任务看板</h2>
      <el-button type="primary" @click="openCreateDialog" v-if="isLeader">
        <el-icon><Plus /></el-icon>
        新建任务
      </el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="queryForm" inline>
        <el-form-item label="项目名称">
          <el-input v-model="queryForm.projectName" placeholder="请输入项目名称" clearable />
        </el-form-item>
        <el-form-item label="负责人">
          <el-select v-model="queryForm.creatorId" placeholder="全部" clearable>
            <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行人">
          <el-select v-model="queryForm.executorId" placeholder="全部" clearable>
            <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="queryForm.priority" placeholder="全部" clearable>
            <el-option v-for="(text, p) in PRIORITY_TEXT" :key="p" :label="text" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="逾期">
          <el-select v-model="queryForm.isOverdue" placeholder="全部" clearable>
            <el-option label="已逾期" :value="1" />
            <el-option label="未逾期" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchKanban">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="kanban-board" v-loading="loading">
      <div v-for="column in columns" :key="column.status" class="kanban-column">
        <div class="column-header" :style="{ borderColor: column.color }">
          <span class="column-title">{{ column.title }}</span>
          <el-tag :color="column.color" effect="dark" size="small">
            {{ (kanbanData[column.status] || []).length }}
          </el-tag>
        </div>

        <div class="column-content">
          <div
            v-for="task in kanbanData[column.status]"
            :key="task.id"
            class="task-card"
            :class="{ 'overdue': task.isOverdue === 1 }"
            @click="goDetail(task.id)"
          >
            <div class="task-header">
              <el-tag :type="getPriorityTagType(task.priority)" size="small">
                {{ PRIORITY_TEXT[task.priority] }}
              </el-tag>
              <el-tag v-if="task.isOverdue === 1" type="danger" size="small">逾期</el-tag>
            </div>
            <div class="task-title">{{ task.taskTitle }}</div>
            <div class="task-project">{{ task.projectName }}</div>
            <div class="task-info">
              <span><el-icon><User /></el-icon> {{ task.executorName || '未分配' }}</span>
              <span><el-icon><UserFilled /></el-icon> {{ task.creatorName }}</span>
            </div>
            <div class="task-dates">
              <span><el-icon><Calendar /></el-icon> {{ formatDate(task.endDate) }}</span>
            </div>
            <el-progress
              class="task-progress"
              :percentage="task.progress || 0"
              :stroke-width="6"
              :status="task.progress === 100 ? 'success' : ''"
            />
          </div>

          <el-empty
            v-if="!kanbanData[column.status] || kanbanData[column.status].length === 0"
            description="暂无任务"
            :image-size="60"
          />
        </div>
      </div>
    </div>

    <el-dialog v-model="createDialogVisible" title="新建任务" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="项目名称" required>
          <el-input v-model="createForm.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="任务标题" required>
          <el-input v-model="createForm.taskTitle" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="createForm.taskDescription" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-form-item label="执行人">
          <el-select v-model="createForm.executorId" placeholder="请选择执行人" clearable>
            <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期">
          <el-date-picker v-model="createForm.startDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="createForm.endDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="createForm.priority" placeholder="请选择优先级">
            <el-option v-for="(text, p) in PRIORITY_TEXT" :key="p" :label="text" :value="p" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { STATUS_TEXT, STATUS_COLOR, PRIORITY_TEXT, TASK_STATUS } from '../utils/constants'
import { getKanban, createTask } from '../api/task'
import { getUserList } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const kanbanData = ref({})
const users = ref([])
const dateRange = ref([])

const currentUser = computed(() => JSON.parse(localStorage.getItem('currentUser') || '{}'))
const isLeader = computed(() => currentUser.value.role === 'LEADER' || currentUser.value.role === 'ADMIN')

const columns = [
  { status: TASK_STATUS.PENDING_ASSIGN, title: '待分配', color: STATUS_COLOR[TASK_STATUS.PENDING_ASSIGN] },
  { status: TASK_STATUS.PENDING_START, title: '待开始', color: STATUS_COLOR[TASK_STATUS.PENDING_START] },
  { status: TASK_STATUS.IN_PROGRESS, title: '进行中', color: STATUS_COLOR[TASK_STATUS.IN_PROGRESS] },
  { status: TASK_STATUS.PENDING_ACCEPT, title: '待验收', color: STATUS_COLOR[TASK_STATUS.PENDING_ACCEPT] },
  { status: TASK_STATUS.REJECTED, title: '已驳回', color: STATUS_COLOR[TASK_STATUS.REJECTED] },
  { status: TASK_STATUS.COMPLETED, title: '已完成', color: STATUS_COLOR[TASK_STATUS.COMPLETED] }
]

const queryForm = reactive({
  projectName: '',
  creatorId: null,
  executorId: null,
  priority: '',
  isOverdue: null,
  endDateBegin: null,
  endDateEnd: null
})

const createDialogVisible = ref(false)
const createForm = reactive({
  projectName: '',
  taskTitle: '',
  taskDescription: '',
  executorId: null,
  startDate: null,
  endDate: null,
  priority: 'MEDIUM'
})

onMounted(() => {
  fetchUsers()
  fetchKanban()
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

const fetchKanban = async () => {
  loading.value = true
  try {
    const params = { ...queryForm }
    if (dateRange.value && dateRange.value.length === 2) {
      params.endDateBegin = dateRange.value[0]
      params.endDateEnd = dateRange.value[1]
    }
    const res = await getKanban(params)
    if (res.code === 200) {
      kanbanData.value = res.data
    }
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  Object.assign(queryForm, {
    projectName: '',
    creatorId: null,
    executorId: null,
    priority: '',
    isOverdue: null
  })
  dateRange.value = []
  fetchKanban()
}

const goDetail = (id) => {
  router.push(`/tasks/${id}`)
}

const openCreateDialog = () => {
  Object.assign(createForm, {
    projectName: '',
    taskTitle: '',
    taskDescription: '',
    executorId: null,
    startDate: null,
    endDate: null,
    priority: 'MEDIUM'
  })
  createDialogVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.projectName || !createForm.taskTitle) {
    ElMessage.warning('请填写必填项')
    return
  }
  try {
    const res = await createTask(createForm)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      createDialogVisible.value = false
      fetchKanban()
    }
  } catch (e) {
    console.error(e)
  }
}

const formatDate = (date) => {
  if (!date) return '-'
  return date
}

const getPriorityTagType = (priority) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }
  return map[priority] || 'info'
}
</script>

<style scoped>
.kanban-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-card {
  margin-bottom: 16px;
}

.kanban-board {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 16px;
  min-height: 500px;
}

.kanban-column {
  flex: 0 0 280px;
  background-color: #f5f7fa;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.column-header {
  padding: 12px 16px;
  background-color: #fff;
  border-bottom: 3px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px 8px 0 0;
}

.column-title {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.column-content {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  max-height: calc(100vh - 380px);
}

.task-card {
  background-color: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 3px solid #e4e7ed;
}

.task-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.task-card.overdue {
  border-left-color: #f56c6c;
  background-color: #fef0f0;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.task-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  line-height: 1.4;
}

.task-project {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.task-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #606266;
  margin-bottom: 6px;
}

.task-info span {
  display: flex;
  align-items: center;
  gap: 2px;
}

.task-dates {
  font-size: 12px;
  color: #606266;
  margin-bottom: 8px;
}

.task-dates span {
  display: flex;
  align-items: center;
  gap: 2px;
}

.task-progress {
  margin-top: 4px;
}
</style>
