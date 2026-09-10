import { createRouter, createWebHistory } from 'vue-router'
import { authState, isLoggedIn } from '@/stores/auth'
import { ROLE } from '@/types/api'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', redirect: '/course/0' },
    {
      path: '/course/:type(0|1|2)',
      name: 'CourseList',
      component: () => import('@/views/CourseListView.vue'),
      meta: { title: '课程中心' },
    },
    {
      path: '/course/detail/:id',
      name: 'CourseDetail',
      component: () => import('@/views/CourseDetailView.vue'),
      meta: { title: '课程详情' },
    },
    {
      path: '/job',
      name: 'JobList',
      component: () => import('@/views/JobListView.vue'),
      meta: { title: '实习就业' },
    },
    {
      path: '/job/detail/:id',
      name: 'JobDetail',
      component: () => import('@/views/JobDetailView.vue'),
      meta: { title: '职位详情' },
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { title: '个人中心', requiresAuth: true },
    },
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/views/AdminView.vue'),
      meta: { title: '后台管理', requiresAuth: true, adminOnly: true },
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', guest: true, plain: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '注册', guest: true, plain: true },
    },
    {
      path: '/reset-password',
      name: 'ResetPassword',
      component: () => import('@/views/ResetPasswordView.vue'),
      meta: { title: '重置密码', guest: true, plain: true },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  // 已登录用户不可进入登录/注册/重置密码页
  if (to.meta.guest && isLoggedIn()) {
    return { path: '/' }
  }
  if (to.meta.requiresAuth && !isLoggedIn()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.adminOnly && authState.userInfo?.role !== ROLE.ADMIN) {
    return { path: '/' }
  }
  if (to.meta.title) {
    document.title = `${String(to.meta.title)} - 智慧教育平台`
  }
  return true
})

export default router
