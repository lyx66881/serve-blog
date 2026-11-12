import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/http'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '数据看板', icon: 'Odometer' } },
        { path: 'blogs', name: 'blogs', component: () => import('../views/Blogs.vue'), meta: { title: '博客管理', icon: 'Document' } },
        { path: 'users', name: 'users', component: () => import('../views/Users.vue'), meta: { title: '用户管理', icon: 'User' } },
        { path: 'categories', name: 'categories', component: () => import('../views/Categories.vue'), meta: { title: '分类管理', icon: 'FolderOpened' } },
        { path: 'tags', name: 'tags', component: () => import('../views/Tags.vue'), meta: { title: '标签管理', icon: 'CollectionTag' } },
        { path: 'feedbacks', name: 'feedbacks', component: () => import('../views/Feedbacks.vue'), meta: { title: '意见反馈', icon: 'ChatDotRound' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(to => {
  document.title = to.meta.title ? `${to.meta.title} - 博客后台管理` : '博客后台管理'
  if (to.name !== 'login' && !getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && getToken()) {
    return { name: 'dashboard' }
  }
  return true
})

export default router
