import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/stores/auth'

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
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach((to) => {
  // 已登录用户不可进入登录/注册页
  if (to.meta.guest && isLoggedIn()) {
    return { path: '/' }
  }
  if (to.meta.title) {
    document.title = `${String(to.meta.title)} - 智慧教育平台`
  }
  return true
})

export default router
