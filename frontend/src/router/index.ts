import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { getAccessToken } from '../api/token'
import { useAuthStore } from '../stores/auth'
// 路由级懒加载：登录页不携带工作台与客户页的组件，减小首屏包体。
const DefaultLayout = () => import('../layouts/DefaultLayout.vue')
const LoginView = () => import('../views/LoginView.vue')
const DashboardView = () => import('../views/DashboardView.vue')
const CustomerListView = () => import('../views/CustomerListView.vue')
const CustomerDetailView = () => import('../views/CustomerDetailView.vue')
const NotFoundView = () => import('../views/NotFoundView.vue')

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    title?: string
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true, title: '登录' }
  },
  {
    path: '/',
    component: DefaultLayout,
    children: [
      { path: '', redirect: '/dashboard' },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: DashboardView,
        meta: { title: '工作台' }
      },
      {
        path: 'customers',
        name: 'customers',
        component: CustomerListView,
        meta: { title: '客户列表' }
      },
      {
        path: 'customers/:id',
        name: 'customer-detail',
        component: CustomerDetailView,
        meta: { title: '客户详情' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: NotFoundView,
    meta: { public: true, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const token = getAccessToken()

  if (to.meta.public === true) {
    if (to.name === 'login' && token) {
      return { name: 'dashboard' }
    }
    return true
  }

  if (!token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  // 刷新页面后内存里没有用户资料，用 /auth/me 恢复
  const auth = useAuthStore()
  if (!auth.user) {
    try {
      await auth.loadMe()
    } catch {
      auth.reset()
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : ''
  document.title = title
    ? `${title} · 硬质合金刀具出口管理系统`
    : '硬质合金刀具出口管理系统'
})

export default router