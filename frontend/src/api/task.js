import request from '../utils/request'

export function createTask(data) {
  return request({
    url: '/tasks',
    method: 'post',
    data
  })
}

export function publishTask(taskId) {
  return request({
    url: `/tasks/${taskId}/publish`,
    method: 'post'
  })
}

export function assignTask(taskId, executorId) {
  return request({
    url: `/tasks/${taskId}/assign`,
    method: 'post',
    params: { executorId }
  })
}

export function updateProgress(data) {
  return request({
    url: '/tasks/progress',
    method: 'post',
    data
  })
}

export function submitAccept(taskId) {
  return request({
    url: `/tasks/${taskId}/submit-accept`,
    method: 'post'
  })
}

export function acceptPass(taskId) {
  return request({
    url: `/tasks/${taskId}/accept-pass`,
    method: 'post'
  })
}

export function acceptReject(data) {
  return request({
    url: '/tasks/accept-reject',
    method: 'post',
    data
  })
}

export function updateTask(taskId, data) {
  return request({
    url: `/tasks/${taskId}`,
    method: 'put',
    data
  })
}

export function queryTasks(params) {
  return request({
    url: '/tasks',
    method: 'get',
    params
  })
}

export function getTaskDetail(taskId) {
  return request({
    url: `/tasks/${taskId}`,
    method: 'get'
  })
}

export function getKanban(params) {
  return request({
    url: '/tasks/kanban',
    method: 'get',
    params
  })
}

export function getOperationLogs(taskId) {
  return request({
    url: `/tasks/${taskId}/logs`,
    method: 'get'
  })
}

export function dragUpdateStatus(data) {
  return request({
    url: '/tasks/drag',
    method: 'post',
    data
  })
}
