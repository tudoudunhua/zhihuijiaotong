import { createRouter, createWebHistory } from 'vue-router'

// 页面
import HomePage from '@/views/HomePage.vue'
import StatisticsPage from '@/views/StatisticsPage.vue'
import LoginPage from '@/views/Login.vue'
import UserManagement from '@/views/UserManagement.vue'
import OperationLogPage from '@/views/OperationLogPage.vue'


// 功能组件
import ViolationList from '@/components/ViolationList.vue'
import ViolationForm from '@/components/ViolationForm.vue'
import CameraList from '@/components/CameraList.vue'
import CameraForm from '@/components/CameraForm.vue'
import PendingViolationList from '@/components/PendingViolationList.vue'

const routes = [
  /* ================= 登录 ================= */
  {
    path: '/login',
    name: 'Login',
    component: LoginPage,
  },

  /* ================= 首页 ================= */
  {
    path: '/',
    name: 'HomePage',
    component: HomePage,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER', 'ANALYST', 'ROAD_GRID', 'GRID_MEMBER', 'TRAFFIC_SQUAD', 'FIRE', 'USER'], permission: 'view_home' },
  },

  /* ================= 违章管理 ================= */
  {
    path: '/violations',
    name: 'ViolationList',
    component: ViolationList,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER', 'USER'], permission: 'view_violations' },
  },
  {
    path: '/violations/pending',
    name: 'PendingViolationList',
    component: PendingViolationList,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER'], permission: 'view_pending' },
  },
  {
    path: '/add-violation',
    name: 'ViolationForm',
    component: ViolationForm,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER'], permission: 'view_add_violation' },
  },
  {
    path: '/add-violation/:id',
    name: 'ViolationFormEdit',
    component: ViolationForm,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER'], permission: 'view_add_violation' },
  },

  /* ================= 统计分析 ================= */
  {
    path: '/statistics',
    name: 'StatisticsPage',
    component: StatisticsPage,
    meta: { requiresAuth: true, roles: ['ADMIN', 'ANALYST'], permission: 'view_statistics' },
  },

  /* ================= 摄像头管理 ================= */
  {
    path: '/cameras',
    name: 'CameraList',
    component: CameraList,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER'], permission: 'view_cameras' },
  },
  {
    path: '/cameras/add',
    name: 'CameraAdd',
    component: CameraForm,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER'], permission: 'view_cameras' },
  },
  {
    path: '/cameras/:id',
    name: 'CameraEdit',
    component: CameraForm,
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER'], permission: 'view_cameras' },
  },

  /* ================= ⚠️ 违章预警（新增模块） ================= */
  {
    path: '/warnings',
    name: 'WarningPage',
    component: () => import('@/views/WarningPage.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN', 'OFFICER', 'ANALYST', 'ROAD_GRID', 'GRID_MEMBER', 'TRAFFIC_SQUAD', 'FIRE'], permission: 'view_warnings' },
  },
  {
    path: '/intelligent',
    name: 'IntelligentWarning',
    component: () => import('@/views/IntelligentWarning.vue'),
    meta: { requiresAuth: true, roles: ['ADMIN', 'ANALYST'], permission: 'view_intelligent' },
  },
  {
    path: '/users',
    name: 'UserManagement',
    component: UserManagement,
    meta: { requiresAuth: true, roles: ['ADMIN'], permission: 'view_user_manage' },
  },
  {
    path: '/operation-logs',
    name: 'OperationLogPage',
    component: OperationLogPage,
    meta: { requiresAuth: true, roles: ['ADMIN'], permission: 'view_operation_logs' },
  },

  /* ================= 404 兜底（必须最后） ================= */
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

function defaultPermissionsByRole(role) {
  const r = String(role || 'USER').toUpperCase()
  if (r === 'ADMIN') {
    return ['view_home', 'view_violations', 'view_pending', 'view_add_violation', 'view_statistics', 'view_intelligent', 'view_warnings', 'view_cameras', 'view_user_manage', 'view_operation_logs']
  }
  if (r === 'OFFICER') {
    return ['view_home', 'view_violations', 'view_pending', 'view_add_violation', 'view_warnings', 'view_cameras']
  }
  if (r === 'ANALYST') {
    return ['view_home', 'view_statistics', 'view_intelligent', 'view_warnings']
  }
  if (['ROAD_GRID', 'GRID_MEMBER', 'TRAFFIC_SQUAD', 'FIRE'].includes(r)) {
    return ['view_home', 'view_warnings']
  }
  return ['view_home', 'view_violations']
}

function resolveFallbackPath(perms = []) {
  const has = (p) => perms.includes(p)
  if (has('view_home')) return '/'
  if (has('view_warnings')) return '/warnings'
  if (has('view_intelligent')) return '/intelligent'
  if (has('view_statistics')) return '/statistics'
  if (has('view_violations')) return '/violations'
  if (has('view_pending')) return '/violations/pending'
  if (has('view_add_violation')) return '/add-violation'
  if (has('view_cameras')) return '/cameras'
  if (has('view_user_manage')) return '/users'
  if (has('view_operation_logs')) return '/operation-logs'
  return null
}

/* ================= 路由守卫 ================= */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('jwt_token')

  // 未登录访问受保护页面
  if (to.meta.requiresAuth && (!token || token === '')) {
    next('/login')
    return
  }

  // 已登录还访问登录页
  if (to.path === '/login' && token) {
    next('/')
    return
  }

  // 角色权限控制：不同用户只看到/只访问自己的功能
  const role = (() => {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      return String(user.role || 'USER').toUpperCase()
    } catch (e) {
      return 'USER'
    }
  })()
  const perms = (() => {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      const permsRaw = user.menuPermissions
      const list = Array.isArray(permsRaw)
        ? permsRaw
        : String(permsRaw || '').split(',').map(s => s.trim()).filter(Boolean)
      return list.length ? list : defaultPermissionsByRole(role)
    } catch (e) {
      return defaultPermissionsByRole(role)
    }
  })()
  const hasExplicitPermissions = (() => {
    try {
      const user = JSON.parse(localStorage.getItem('user') || '{}')
      const permsRaw = user.menuPermissions
      const list = Array.isArray(permsRaw)
        ? permsRaw
        : String(permsRaw || '').split(',').map(s => s.trim()).filter(Boolean)
      return list.length > 0
    } catch (e) {
      return false
    }
  })()

  const fallback = resolveFallbackPath(perms)
  const allowRoles = to.meta?.roles
  if (!hasExplicitPermissions && Array.isArray(allowRoles) && allowRoles.length && !allowRoles.includes(role)) {
    if (fallback && fallback !== to.path) {
      next(fallback)
      return
    }
    localStorage.removeItem('jwt_token')
    localStorage.removeItem('user')
    next('/login')
    return
  }

  const permission = to.meta?.permission
  if (permission) {
    if (!perms.includes(permission)) {
      if (fallback && fallback !== to.path) {
        next(fallback)
        return
      }
      localStorage.removeItem('jwt_token')
      localStorage.removeItem('user')
      next('/login')
      return
    }
  }

  next()
})

export default router
