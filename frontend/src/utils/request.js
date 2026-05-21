import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getCurrentUser } from './storage'

const service = axios.create({
  baseURL: '/api',
  timeout: 10000
})

service.interceptors.request.use(
  (config) => {
    const user = getCurrentUser() || {}
    if (user.userId) {
      config.headers['X-User-Id'] = user.userId
      config.headers['X-Username'] = user.username
      config.headers['X-User-Role'] = user.role
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
