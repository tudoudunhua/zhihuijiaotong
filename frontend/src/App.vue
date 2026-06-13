<template>
  <div id="app">
    <template v-if="isLoginPage">
      <router-view />
    </template>

    <template v-else>
      <div class="app-layout">
        <aside class="app-sidebar">
          <div class="sidebar-brand">
            <span class="logo-text">♺ 智慧交通违章分析与预警系统</span>
          </div>

          <el-menu
            v-if="isLoggedIn"
            :default-active="activeMenu"
            class="nav-menu"
            @select="handleMenuSelect"
          >
            <el-menu-item v-if="canViewHome" index="/">
              <el-icon><HomeFilled /></el-icon>
              <span>首页</span>
            </el-menu-item>

            <el-menu-item v-if="canViewViolations" index="/violations">
              <el-icon><List /></el-icon>
              <span>违章记录</span>
            </el-menu-item>

            <el-menu-item v-if="canViewPending" index="/violations/pending">
              <el-icon><Warning /></el-icon>
              <span>待处理违章</span>
            </el-menu-item>

            <el-menu-item v-if="canAddViolation" index="/add-violation">
              <el-icon><Plus /></el-icon>
              <span>添加记录</span>
            </el-menu-item>

            <el-menu-item v-if="canViewStatistics" index="/statistics">
              <el-icon><DataAnalysis /></el-icon>
              <span>统计分析</span>
            </el-menu-item>

            <el-menu-item v-if="canViewIntelligent" index="/intelligent">
              <el-icon><DataAnalysis /></el-icon>
              <span>智能预警</span>
            </el-menu-item>

            <el-menu-item v-if="canViewWarnings" index="/warnings">
              <el-icon><Warning /></el-icon>
              <span>预警列表</span>
            </el-menu-item>

            <el-menu-item v-if="canManageCameras" index="/cameras">
              <el-icon><VideoCamera /></el-icon>
              <span>摄像头管理</span>
            </el-menu-item>
            <el-menu-item v-if="canManageUsers" index="/users">
              <el-icon><List /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item v-if="canViewOperationLogs" index="/operation-logs">
              <el-icon><List /></el-icon>
              <span>操作日志</span>
            </el-menu-item>
          </el-menu>
        </aside>

        <div class="app-content">
          <header class="content-topbar">
            <div class="topbar-left">系统导航</div>
            <div class="header-right">
              <template v-if="isLoggedIn">
                <span class="user-name">欢迎，{{ username }}</span>
                <el-tag :type="isAdmin ? 'warning' : 'info'" size="default" class="role-tag">
                  {{ roleDisplay }}
                </el-tag>
                <el-button link class="logout-btn switch-user-btn" @click="switchUser">
                  切换用户
                </el-button>
              </template>
            </div>
          </header>

          <el-main class="app-main">
            <router-view />
          </el-main>
        </div>
      </div>
    </template>
  </div>
</template>
<script>
  import {
    HomeFilled,
    List,
    Plus,
    DataAnalysis,
    VideoCamera,
    Warning,
  } from '@element-plus/icons-vue'
  
  export default {
    name: 'App',
  
    components: {
      HomeFilled,
      List,
      Plus,
      DataAnalysis,
      VideoCamera,
      Warning,
    },
  
    data() {
      return {
        user: null,
        token: null,
      }
    },
  
    mounted() {
      this.syncUser()
      window.addEventListener('auth-changed', this.syncUser)
    },
    watch: {
      $route() {
        this.syncUser()
      },
    },
    beforeUnmount() {
      window.removeEventListener('auth-changed', this.syncUser)
    },
  
    computed: {
      activeMenu() {
        return this.$route.path
      },
      isLoginPage() {
        return this.$route.path === '/login'
      },
      isLoggedIn() {
        return !!this.token
      },
      currentUser() {
        if (this.user) return this.user
        try {
          const s = localStorage.getItem('user')
          return s ? JSON.parse(s) : null
        } catch (e) {
          return null
        }
      },
      username() {
        return this.currentUser?.realName || this.currentUser?.username || ''
      },
      isAdmin() {
        return this.normalizedRole === 'ADMIN'
      },
      normalizedRole() {
        const r = String(this.currentUser?.role || '').toUpperCase()
        if (r === '管理员') return 'ADMIN'
        if (r === 'ANALYSIS' || r === 'ANALYST') return 'ANALYST'
        if (r === 'POLICE' || r === 'OFFICER') return 'OFFICER'
        if (r === '路段网格员'.toUpperCase()) return 'ROAD_GRID'
        if (r === '网格员'.toUpperCase()) return 'GRID_MEMBER'
        if (r === '交警中队'.toUpperCase()) return 'TRAFFIC_SQUAD'
        if (r === '消防'.toUpperCase()) return 'FIRE'
        if (r === 'ADMIN' || r === 'USER') return r
        if (['ROAD_GRID', 'GRID_MEMBER', 'TRAFFIC_SQUAD', 'FIRE'].includes(r)) return r
        return 'USER'
      },
      canViewHome() {
        return this.hasPermission('view_home')
      },
      canViewViolations() {
        return this.hasPermission('view_violations')
      },
      canViewPending() {
        return this.hasPermission('view_pending')
      },
      canAddViolation() {
        return this.hasPermission('view_add_violation')
      },
      canViewStatistics() {
        return this.hasPermission('view_statistics')
      },
      canViewIntelligent() {
        return this.hasPermission('view_intelligent')
      },
      canViewWarnings() {
        return this.hasPermission('view_warnings')
      },
      canManageCameras() {
        return this.hasPermission('view_cameras')
      },
      canManageUsers() {
        return this.hasPermission('view_user_manage')
      },
      canViewOperationLogs() {
        return this.hasPermission('view_operation_logs')
      },
      roleDisplay() {
        if (this.normalizedRole === 'ADMIN') return '管理员'
        if (this.normalizedRole === 'OFFICER') return '交警处置员'
        if (this.normalizedRole === 'ANALYST') return '数据分析员'
        if (this.normalizedRole === 'ROAD_GRID') return '路段网格员'
        if (this.normalizedRole === 'GRID_MEMBER') return '网格员'
        if (this.normalizedRole === 'TRAFFIC_SQUAD') return '交警中队'
        if (this.normalizedRole === 'FIRE') return '消防'
        return '普通用户'
      },
    },
  
    methods: {
      syncUser() {
        try {
          const s = localStorage.getItem('user')
          this.user = s ? JSON.parse(s) : null
        } catch (e) {
          this.user = null
        }
        this.token = localStorage.getItem('jwt_token') || null
      },
      handleMenuSelect(path) {
        this.$router.push(path)
      },

      switchUser() {
        localStorage.removeItem('jwt_token')
        localStorage.removeItem('user')
        this.user = null
        this.token = null
        if (this.$axios?.defaults?.headers?.common) {
          delete this.$axios.defaults.headers.common.Authorization
        }
        window.dispatchEvent(new Event('auth-changed'))
        this.$message.success('请使用新账号登录')
        this.$router.push('/login')
      },
  
      logout() {
        localStorage.removeItem('jwt_token')
        localStorage.removeItem('user')
        this.user = null
        this.token = null
        if (this.$axios?.defaults?.headers?.common) {
          delete this.$axios.defaults.headers.common.Authorization
        }
        window.dispatchEvent(new Event('auth-changed'))
        this.$message.success('已退出登录')
        this.$router.push('/login')
      },
  
      goLogin() {
        this.$router.push('/login')
      },
      hasPermission(permission) {
        const raw = this.currentUser?.menuPermissions
        let perms = Array.isArray(raw)
          ? raw
          : String(raw || '').split(',').map(s => s.trim()).filter(Boolean)
        if (!perms.length) {
          const role = this.normalizedRole
          if (role === 'ADMIN') {
            perms = ['view_home', 'view_violations', 'view_pending', 'view_add_violation', 'view_statistics', 'view_intelligent', 'view_warnings', 'view_cameras', 'view_user_manage', 'view_operation_logs']
          } else if (role === 'OFFICER') {
            perms = ['view_home', 'view_violations', 'view_pending', 'view_add_violation', 'view_warnings', 'view_cameras']
          } else if (role === 'ANALYST') {
            perms = ['view_home', 'view_statistics', 'view_intelligent', 'view_warnings']
          } else if (['ROAD_GRID', 'GRID_MEMBER', 'TRAFFIC_SQUAD', 'FIRE'].includes(role)) {
            perms = ['view_home', 'view_warnings']
          } else {
            perms = ['view_home', 'view_violations']
          }
        }
        return perms.includes(permission)
      },
    },
  }
  </script>
  <style>
/* ================= 基础 ================= */
#app {
  min-height: 100vh;
  font-family: 'PingFang SC', 'Microsoft YaHei', Arial, sans-serif;
}

.app-layout {
  height: 100vh;
  display: flex;
  background: #f5f7fa;
  overflow: hidden;
}

.app-sidebar {
  width: 240px;
  height: 100vh;
  position: sticky;
  top: 0;
  background: linear-gradient(180deg, #667eea, #764ba2);
  color: #fff;
  box-shadow: 2px 0 12px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
}

.sidebar-brand {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.15);
}

.logo-text {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

.app-sidebar .nav-menu {
  border-right: none !important;
  background: transparent !important;
}

.app-sidebar .nav-menu .el-menu-item {
  color: rgba(255, 255, 255, 0.92);
  font-size: 14px;
}

.app-sidebar .nav-menu .el-menu-item:hover {
  background: rgba(255, 255, 255, 0.16);
}

.app-sidebar .nav-menu .el-menu-item.is-active {
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.app-content {
  flex: 1;
  min-width: 0;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.content-topbar {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.topbar-left {
  font-size: 15px;
  color: #303133;
  font-weight: 600;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-size: 14px;
  color: #606266;
}

.role-tag {
  margin-right: 4px;
}

.app-main {
  flex: 1;
  background: #f5f7fa;
  padding: 16px;
  overflow-y: auto;
}
</style>
    