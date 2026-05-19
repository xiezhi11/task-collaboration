<template>
  <div class="task-list-container">
    <div class="page-header">
      <h2>任务列表</h2>
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
        <el-form-item label="任务状态">
          <el-select v-model="queryForm.taskStatus" placeholder="全部" clearable>
            <el-option v-for="(text, status) in STATUS_TEXT" :key="status" :label="text" :value="status" />
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="queryForm.priority" placeholder="全部" clearable>
            <el-option v-for="(text, p) in PRIORITY_TEXT" :key="p" :label="text" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="执行人">
          <el-select v-model="queryForm.executorId" placeholder="全部" clearable>
            <el-option v-for="user in users" :key="user.id" :label="user.name" :value="user.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="逾期">
          <el-select v-model="queryForm.isOverdue" placeholder="全部" clearable>
            <el-option label="已逾期" :value="1" />
            <el-option label="未逾期" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchTasks">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="projectName" label="项目名称" width="120" />
        <el-table-column prop="taskTitle" label="任务标题" min-width="150">
          <template #default="{ row }">
            <el-link type="primary" @click="goDetail(row.id)">{{ row.taskTitle }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="creatorName" label="负责人" width="80" />
        <el-table-column prop="executorName" label="执行人" width="80" />
        <el-table-column prop="startDate" label="开始日期" width="110" />
        <el-table-column prop="endDate" label="截止日期" width="110">
          <template #default="{ row }">
            <span :class="{ 'overdue-text': row.isOverdue === 1 && row.taskStatus !== 'COMPLETED' }">
              {{ row.endDate }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <el-progress :percentage="row.progress" :status="row.progress === 100 ? 'success' : ''" />
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80">
          <template #default="{ row }">
            <el-tag :type="getPriorityTagType(row.priority)">{{ PRIORITY_TEXT[row.priority] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :color="STATUS_COLOR[row.taskStatus]" effect="dark">{{ STATUS_TEXT[row.taskStatus] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="逾期" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.isOverdue === 1 && row.taskStatus !== 'COMPLETED'" type="danger">已逾期</el-tag>
            <el-tag v-else-if="row.wasOverdue === 1" type="warning">曾逾期</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="goDetail(row.id)">详情</el-button>
            <el-button
              size="small"
              type="primary"
              link
              @click="openAssignDialog(row)"
              v-if="isLeader && row.taskStatus !== 'COMPLETED'"
            >分配</el-button>
            <el-button
              size="small"
              type="primary"
              link
              @click="openProgressDialog(row)"
              v-if="canUpdateProgress(row)"
            >更新进度</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pagination"
        v-model:current-page="queryForm.pageNum"
        v-model:page-size="queryForm.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchTasks"
        @current-change="fetchTasks"
      />
    </el-card>

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
          <el-date-picker v-model="createForm.startDate" type="date" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="截止日期">
          <el-date-picker v-model="createForm.endDate" type="date" placeholder="选择日期" />
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

    <el-dialog v-model="assignDialogVisible" title="分配任务" width="400px">
      <el-form label-width="80px">
        <el-form-item label="任务">
          <span>{{ currentTask?.taskTitle }}</span>
        </el-form-item>
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
        <el-form-item label="任务">
          <span>{{ currentTask?.taskTitle }}</span>
        </el-form-item>
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
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { STATUS_TEXT, STATUS_COLOR, PRIORITY_TEXT } from '../utils/constants'
import { queryTasks, createTask, assignTask, updateProgress, publishTask } from '../api/task'
import { getUserList } from '../api/auth'

const router = useRouter()
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const users = ref([])

const currentUser = computed(() => JSON.parse(localStorage.getItem('currentUser') || '{}'))
const isLeader = computed(() => currentUser.value.role === 'LEADER' || currentUser.value.role === 'ADMIN')

const queryForm = reactive({
  projectName: '',
  taskStatus: '',
  priority: '',
  executorId: null,
  isOverdue: null,
  pageNum: 1,
  pageSize: 10
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

const assignDialogVisible = ref(false)
const currentTask = ref(null)
const assignExecutorId = ref(null)

const progressDialogVisible = ref(false)
const progressValue = ref(0)

onMounted(() => {
  fetchUsers()
  fetchTasks()
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

const fetchTasks = async () => {
  loading.value = true
  try {
    const res = await queryTasks(queryForm)
    if (res.code === 200) {
      tableData.value = res.data.records
      total.value = res.data.total
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
    taskStatus: '',
    priority: '',
    executorId: null,
    isOverdue: null,
    pageNum: 1
  })
  fetchTasks()
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
      fetchTasks()
    }
  } catch (e) {
    console.error(e)
  }
}

const openAssignDialog = (row) => {
  currentTask.value = row
  assignExecutorId.value = row.executorId
  assignDialogVisible.value = true
}

const handleAssign = async () => {
  if (!assignExecutorId.value) {
    ElMessage.warning('请选择执行人')
    return
  }
  try {
    const res = await assignTask(currentTask.value.id, assignExecutorId.value)
    if (res.code === 200) {
      ElMessage.success('分配成功')
      assignDialogVisible.value = false
      fetchTasks()
    }
  } catch (e) {
    console.error(e)
  }
}

const canUpdateProgress = (row) => {
  if (row.taskStatus === 'COMPLETED' || row.taskStatus === 'PENDING_ACCEPT') {
    return false
  }
  if (isLeader.value) {
    return true
  }
  return row.executorId === currentUser.value.userId
}

const openProgressDialog = (row) => {
  currentTask.value = row
  progressValue.value = row.progress
  progressDialogVisible.value = true
}

const handleUpdateProgress = async () => {
  try {
    const res = await updateProgress({
      taskId: currentTask.value.id,
      progress: progressValue.value
    })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      progressDialogVisible.value = false
      fetchTasks()
    }
  } catch (e) {
    console.error(e)
  }
}

const getPriorityTagType = (priority) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }
  return map[priority] || 'info'
}
</script>

<style scoped>
.task-list-container {
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

.table-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.overdue-text {
  color: #f56c6c;
  font-weight: bold;
}
</style>
