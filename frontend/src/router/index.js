import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/tasks'
  },
  {
    path: '/tasks',
    name: 'TaskList',
    component: () => import('../views/TaskList.vue')
  },
  {
    path: '/tasks/:id',
    name: 'TaskDetail',
    component: () => import('../views/TaskDetail.vue')
  },
  {
    path: '/kanban',
    name: 'Kanban',
    component: () => import('../views/Kanban.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
