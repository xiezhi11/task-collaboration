<template>
  <div class="app-container">
    <el-container>
      <el-header class="header">
        <div class="header-content">
          <h1 class="title">项目任务协作系统</h1>
          <div class="user-info">
            <el-dropdown @command="switchUser">
              <span class="current-user">
                <el-icon><User /></el-icon>
                {{ currentUser?.name || '请选择用户' }} ({{ currentUser?.role }})
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="user in users"
                    :key="user.username"
                    :command="user"
                  >
                    {{ user.name }} ({{ user.role }})
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
        <el-menu
          :default-active="$route.path"
          mode="horizontal"
          router
          class="nav-menu"
        >
          <el-menu-item index="/tasks">
            <el-icon><List /></el-icon>
            任务列表
          </el-menu-item>
          <el-menu-item index="/kanban">
            <el-icon><Grid /></el-icon>
            任务看板
          </el-menu-item>
        </el-menu>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getUserList, login } from './api/auth'
import { getCurrentUser } from './utils/storage'

const router = useRouter()
const users = ref([])
const currentUser = ref(getCurrentUser())

onMounted(async () => {
  try {
    const res = await getUserList()
    if (res.code === 200) {
      users.value = res.data
      if (!currentUser.value && users.value.length > 0) {
        await switchUser(users.value[0], false)
      }
    }
  } catch (e) {
    console.error('获取用户列表失败', e)
  }
})

const switchUser = async (user, reload = true) => {
  try {
    const res = await login(user.username)
    if (res.code === 200) {
      currentUser.value = res.data
      localStorage.setItem('currentUser', JSON.stringify(res.data))
      ElMessage.success(`已切换到用户: ${res.data.name}`)
      if (reload) {
        router.go(0)
      }
    } else {
      ElMessage.error(res.message)
    }
  } catch (e) {
    console.error('登录失败', e)
  }
}
</script>

<style scoped>
.app-container {
  min-height: 100vh;
  background-color: #f5f7fa;
}

.header {
  background-color: #fff;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 24px;
  height: 60px;
}

.title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.current-user {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
}

.nav-menu {
  border-bottom: none;
  padding: 0 24px;
}

.main-content {
  padding: 24px;
}
</style>
