<template>
  <div class="kanban-container">
    <div class="page-header">
      <h2>任务看板 <small class="tip-text">（支持拖拽变更状态，受权限和状态机控制）</small></h2>
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
        <div class="column-header" :style="{ borderColor: column.color }" :data-status="column.status">
          <span class="column-title">{{ column.title }}</span>
          <el-tag :color="column.color" effect="dark" size="small">
            {{ (kanbanData[column.status] || []).length }}
          </el-tag>
        </div>

        <div class="column-content"
             :class="{ 'drag-over-allowed': dragOverAllowed, 'drag-over-denied': dragOverDenied }">
          <draggable
            v-model="kanbanData[column.status]"
            group="tasks"
            item-key="id"
            :clone="cloneTask"
            :move="onMove"
            @start="onDragStart"
            @end="onDragEnd"
            class="drag-list"
            :disabled="!canDragAny"
            ghost-class="ghost-card"
            chosen-class="chosen-card"
            drag-class="drag-card"
          >
            <template #item="{ element }">
              <div
                class="task-card"
                :class="{
                  'overdue': element.isOverdue === 1,
                  'can-drag': canDragTask(element),
                  'cannot-drag': !canDragTask(element)
                }"
                @click="goDetail(element.id)"
              >
                <div class="drag-handle" v-if="canDragTask(element)">
                  <el-icon><Rank /></el-icon>
                </div>
                <div class="task-header">
                  <el-tag :type="getPriorityTagType(element.priority)" size="small">
                    {{ PRIORITY_TEXT[element.priority] }}
                  </el-tag>
                  <div class="header-right">
                    <el-tag v-if="element.isOverdue === 1" type="danger" size="small">逾期</el-tag>
                    <el-tooltip v-if="!canDragTask(element)" content="该任务当前状态不允许拖拽">
                      <el-icon class="lock-icon"><Lock /></el-icon>
                    </el-tooltip>
                  </div>
                </div>
                <div class="task-title">{{ element.taskTitle }}</div>
                <div class="task-project">{{ element.projectName }}</div>
                <div class="task-info">
                  <span><el-icon><User /></el-icon> {{ element.executorName || '未分配' }}</span>
                  <span><el-icon><UserFilled /></el-icon> {{ element.creatorName }}</span>
                </div>
                <div class="task-dates">
                  <span><el-icon><Calendar /></el-icon> {{ formatDate(element.endDate) }}</span>
                </div>
                <el-progress
                  class="task-progress"
                  :percentage="element.progress || 0"
                  :stroke-width="6"
                  :status="element.progress === 100 ? 'success' : ''"
                />
              </div>
            </template>
          </draggable>

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

    <el-dialog v-model="rejectDialogVisible" title="填写驳回原因" width="500px" :close-on-click-modal="false">
      <el-form :model="rejectForm" label-width="100px">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="rejectForm.rejectReason"
            type="textarea"
            :rows="4"
            placeholder="请输入驳回原因，便于执行人了解需要改进的内容"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelReject">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, User, UserFilled, Calendar, Rank, Lock } from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import { STATUS_TEXT, STATUS_COLOR, PRIORITY_TEXT, TASK_STATUS } from '../utils/constants'
import { getKanban, createTask, dragUpdateStatus } from '../api/task'
import { getUserList } from '../api/auth'
import { getCurrentUser } from '../utils/storage'

const router = useRouter()
const loading = ref(false)
const kanbanData = ref({})
const users = ref([])
const dateRange = ref([])

const currentUser = computed(() => getCurrentUser() || {})
const isLeader = computed(() => currentUser.value.role === 'LEADER' || currentUser.value.role === 'ADMIN')

const dragOverAllowed = ref(false)
const dragOverDenied = ref(false)
const draggedTask = ref(null)
const originalColumn = ref(null)
const isDragging = ref(false)

const rejectDialogVisible = ref(false)
const pendingDragTarget = ref(null)
const rejectForm = reactive({
  rejectReason: ''
})

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

const canDragAny = computed(() => true)

const canDragTask = (task) => {
  if (!task) return false
  if (task.taskStatus === TASK_STATUS.COMPLETED) return false

  const user = currentUser.value
  if (!user) return false

  const isTaskCreator = task.creatorId === user.id
  const isTaskExecutor = task.executorId === user.id
  const isAdminOrLeader = user.role === 'LEADER' || user.role === 'ADMIN'

  if (task.taskStatus === TASK_STATUS.DRAFT) return false
  if (task.taskStatus === TASK_STATUS.PENDING_ASSIGN) {
    return isAdminOrLeader || isTaskCreator
  }
  if (task.taskStatus === TASK_STATUS.PENDING_START || task.taskStatus === TASK_STATUS.REJECTED) {
    return (isAdminOrLeader || isTaskCreator) || isTaskExecutor
  }
  if (task.taskStatus === TASK_STATUS.IN_PROGRESS) {
    return isTaskExecutor || (isAdminOrLeader || isTaskCreator)
  }
  if (task.taskStatus === TASK_STATUS.PENDING_ACCEPT) {
    return isAdminOrLeader || isTaskCreator
  }

  return false
}

const getAllowedTargetStatuses = (task) => {
  if (!task) return []
  const status = task.taskStatus
  const allowed = []

  if (status === TASK_STATUS.PENDING_ASSIGN && task.executorId) {
    allowed.push(TASK_STATUS.PENDING_START)
  }
  if (status === TASK_STATUS.PENDING_START) {
    allowed.push(TASK_STATUS.IN_PROGRESS)
    if (task.progress === 100) allowed.push(TASK_STATUS.PENDING_ACCEPT)
  }
  if (status === TASK_STATUS.IN_PROGRESS) {
    if (task.progress === 100) allowed.push(TASK_STATUS.PENDING_ACCEPT)
  }
  if (status === TASK_STATUS.REJECTED) {
    allowed.push(TASK_STATUS.IN_PROGRESS)
    if (task.progress === 100) allowed.push(TASK_STATUS.PENDING_ACCEPT)
  }
  if (status === TASK_STATUS.PENDING_ACCEPT) {
    allowed.push(TASK_STATUS.COMPLETED)
    allowed.push(TASK_STATUS.REJECTED)
  }

  return allowed
}

const cloneTask = (task) => {
  return { ...task }
}

const onDragStart = (evt) => {
  draggedTask.value = evt.item
  originalColumn.value = evt.from.dataset.status
  isDragging.value = true
}

const onMove = (evt) => {
  const task = evt.draggedContext.element
  const targetStatus = evt.to.dataset.status

  if (!canDragTask(task)) {
    dragOverAllowed.value = false
    dragOverDenied.value = true
    return false
  }

  const allowedTargets = getAllowedTargetStatuses(task)
  const allowed = allowedTargets.includes(targetStatus)

  dragOverAllowed.value = allowed
  dragOverDenied.value = !allowed

  return allowed
}

const onDragEnd = async (evt) => {
  isDragging.value = false
  dragOverAllowed.value = false
  dragOverDenied.value = false

  const task = evt.item.__vueParentComponent.ctx.element
  const fromStatus = evt.from.dataset.status
  const toStatus = evt.to.dataset.status

  if (fromStatus === toStatus) {
    return
  }

  if (!task || !toStatus) {
    fetchKanban()
    return
  }

  const allowedTargets = getAllowedTargetStatuses(task)
  if (!allowedTargets.includes(toStatus)) {
    ElMessage.warning('不允许从 ' + STATUS_TEXT[fromStatus] + ' 拖拽到 ' + STATUS_TEXT[toStatus])
    await nextTick()
    fetchKanban()
    return
  }

  const user = currentUser.value
  const isAdminOrLeader = user.role === 'LEADER' || user.role === 'ADMIN'
  const isTaskCreator = task.creatorId === user.id
  const isTaskExecutor = task.executorId === user.id

  if (toStatus === TASK_STATUS.PENDING_START || toStatus === TASK_STATUS.COMPLETED || toStatus === TASK_STATUS.REJECTED) {
    if (!isAdminOrLeader && !isTaskCreator) {
      ElMessage.warning('只有负责人或管理员可以拖拽到 ' + STATUS_TEXT[toStatus] + ' 状态')
      await nextTick()
      fetchKanban()
      return
    }
  }

  if (toStatus === TASK_STATUS.IN_PROGRESS || toStatus === TASK_STATUS.PENDING_ACCEPT) {
    if (!isTaskExecutor) {
      ElMessage.warning('只有任务执行人可以拖拽到 ' + STATUS_TEXT[toStatus] + ' 状态')
      await nextTick()
      fetchKanban()
      return
    }
  }

  if (toStatus === TASK_STATUS.PENDING_ACCEPT && (!task.progress || task.progress < 100)) {
    ElMessage.warning('任务进度必须达到100%才能拖拽到待验收')
    await nextTick()
    fetchKanban()
    return
  }

  if (toStatus === TASK_STATUS.REJECTED) {
    pendingDragTarget.value = { taskId: task.id, targetStatus: toStatus }
    rejectForm.rejectReason = ''
    rejectDialogVisible.value = true
    return
  }

  try {
    const res = await dragUpdateStatus({
      taskId: task.id,
      targetStatus: toStatus
    })
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
    } else {
      ElMessage.error(res.message || '状态更新失败')
      await nextTick()
      fetchKanban()
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('状态更新失败')
    await nextTick()
    fetchKanban()
  }
}

const cancelReject = () => {
  rejectDialogVisible.value = false
  pendingDragTarget.value = null
  fetchKanban()
}

const confirmReject = async () => {
  if (!rejectForm.rejectReason || !rejectForm.rejectReason.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }
  try {
    const res = await dragUpdateStatus({
      taskId: pendingDragTarget.value.taskId,
      targetStatus: pendingDragTarget.value.targetStatus,
      rejectReason: rejectForm.rejectReason.trim()
    })
    if (res.code === 200) {
      ElMessage.success('已驳回')
      rejectDialogVisible.value = false
      pendingDragTarget.value = null
    } else {
      ElMessage.error(res.message || '驳回失败')
      rejectDialogVisible.value = false
      pendingDragTarget.value = null
      fetchKanban()
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('驳回失败')
    rejectDialogVisible.value = false
    pendingDragTarget.value = null
    fetchKanban()
  }
}

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
      await nextTick()
      document.querySelectorAll('.column-content').forEach(el => {
        const status = el.parentElement.querySelector('.column-header')?.dataset?.status
        if (status) {
          el.dataset.status = status
        }
      })
      columns.forEach(col => {
        const colEl = document.querySelector(`.column-header[data-status="${col.status}"]`)
        if (colEl) {
          colEl.parentElement.querySelector('.column-content').dataset.status = col.status
        }
      })
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
  if (isDragging.value) return
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

.tip-text {
  font-size: 13px;
  color: #909399;
  font-weight: normal;
  margin-left: 8px;
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
  transition: background-color 0.2s;
}

.column-content.drag-over-allowed {
  background-color: #e1f3d8;
  border: 2px dashed #67c23a;
}

.column-content.drag-over-denied {
  background-color: #fef0f0;
  border: 2px dashed #f56c6c;
}

.drag-list {
  min-height: 100px;
}

.task-card {
  background-color: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.3s;
  border-left: 3px solid #e4e7ed;
  position: relative;
}

.task-card.can-drag {
  cursor: grab;
}

.task-card.cannot-drag {
  opacity: 0.85;
  cursor: not-allowed;
}

.task-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.task-card.overdue {
  border-left-color: #f56c6c;
  background-color: #fef0f0;
}

.task-card.ghost-card {
  opacity: 0.5;
  background-color: #c0c4cc;
}

.task-card.chosen-card {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
  transform: rotate(2deg);
}

.task-card.drag-card {
  opacity: 0.9;
  cursor: grabbing;
}

.drag-handle {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #c0c4cc;
  cursor: grab;
  opacity: 0.6;
}

.drag-handle:hover {
  opacity: 1;
  color: #409eff;
}

.lock-icon {
  color: #c0c4cc;
  margin-left: 4px;
}

.task-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
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
