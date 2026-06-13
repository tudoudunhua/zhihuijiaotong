<template>
  <div class="dashboard">
    <div class="dashboard-inner">
      <div class="hero">
        <div class="dash-head">
          <div>
            <div class="dash-title">智慧交通违章分析与预警系统</div>
            <div class="dash-sub">
              <!-- 首页总览 · 违章处理闭环（抓拍待处理→通知车主→标记处理）· 四级预警（蓝/黄/橙/红） -->
            </div>
          </div>
          <div class="dash-actions">
            <el-button type="warning" plain @click="generateDemoAll" :loading="generatingDemo">
              生成演示数据
            </el-button>
            <el-button type="primary" @click="loadAll" :loading="loadingAll">刷新</el-button>
          </div>
        </div>
      </div>

    <!-- 快捷入口 -->
    <el-card class="panel">
      <div class="panel-head">
        <div class="panel-title">快捷入口</div>
        <div class="panel-desc">根据你的角色自动显示可用功能入口</div>
      </div>
      <div class="quick-grid">
        <el-button v-if="isAdmin" type="primary" class="quick-btn" @click="navigateToAddViolation">
          添加违章记录
        </el-button>
        <el-button class="quick-btn" @click="navigateToViolationList">查看违章记录</el-button>
        <el-button class="quick-btn" @click="navigateToPending">待处理违章</el-button>
        <el-button class="quick-btn" @click="navigateToWarnings">预警列表</el-button>
        <el-button class="quick-btn" @click="navigateToStatistics">统计分析</el-button>
        <el-button v-if="isAdmin" class="quick-btn" @click="navigateToCameras">摄像头管理</el-button>
      </div>
    </el-card>

    <!-- 总览数据卡片 -->
    <el-row :gutter="14" class="cards-row">
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric">
            <div class="metric-icon primary"><el-icon><Document /></el-icon></div>
            <div class="metric-main">
              <div class="metric-value">{{ overview.totalViolations }}</div>
              <div class="metric-label">违章总数</div>
            </div>
          </div>
          <div class="metric-sub">平均罚款：¥{{ avgFine }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric">
            <div class="metric-icon warning"><el-icon><Money /></el-icon></div>
            <div class="metric-main">
              <div class="metric-value">¥{{ totalFinesText }}</div>
              <div class="metric-label">罚款总额</div>
            </div>
          </div>
          <div class="metric-sub">扣分合计：{{ overview.totalPoints }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric">
            <div class="metric-icon danger"><el-icon><BellFilled /></el-icon></div>
            <div class="metric-main">
              <div class="metric-value">{{ pendingCount }}</div>
              <div class="metric-label">抓拍待处理</div>
            </div>
          </div>
          <div class="metric-sub">
            <el-link type="primary" @click="navigateToPending">去处理 →</el-link>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="metric-card" shadow="hover">
          <div class="metric">
            <div class="metric-icon red"><el-icon><Warning /></el-icon></div>
            <div class="metric-main">
              <div class="metric-value">{{ warningCounts.RED }}</div>
              <div class="metric-label">红色预警</div>
            </div>
          </div>
          <div class="metric-sub">
            四级预警：蓝 {{ warningCounts.BLUE }} / 黄 {{ warningCounts.YELLOW }} / 橙 {{ warningCounts.ORANGE }} / 红
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 列表区 -->
    <el-row :gutter="14">
      <el-col :span="12">
        <el-card class="panel" shadow="never">
          <div class="panel-head">
            <div class="panel-title">最新待处理违章（摄像头抓拍）</div>
            <div class="panel-desc">未处理记录优先展示，支持一键进入处理页</div>
          </div>
          <el-table :data="pendingPreview" size="small" border stripe v-loading="loadingPending">
            <el-table-column prop="plateNumber" label="车牌号" width="110" />
            <el-table-column prop="violationType" label="违章类型" width="120" />
            <el-table-column prop="violationLocation" label="地点" min-width="160" show-overflow-tooltip />
            <el-table-column prop="violationTime" label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.violationTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default>
                <el-button type="primary" link @click="navigateToPending">去处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingPending && pendingCount === 0" description="暂无待处理抓拍记录，可点击右上角生成演示数据" />
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="panel" shadow="never">
          <div class="panel-head">
            <div class="panel-title">最新预警（四级）</div>
            <div class="panel-desc">按等级分级推送，帮助快速判断处置优先级</div>
          </div>
          <el-table :data="warningPreview" size="small" border stripe v-loading="loadingWarnings">
            <el-table-column prop="level" label="级别" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="levelTagType(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="预警信息" min-width="240" show-overflow-tooltip />
            <el-table-column prop="pushTarget" label="推送对象" width="150" show-overflow-tooltip />
            <el-table-column prop="warningTime" label="时间" width="170">
              <template #default="{ row }">{{ formatTime(row.warningTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="110" align="center">
              <template #default>
                <el-button type="primary" link @click="navigateToWarnings">查看全部</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loadingWarnings && warnings.length === 0" description="暂无预警数据，可点击右上角生成演示数据" />
        </el-card>
      </el-col>
    </el-row>
    </div>
  </div>
</template>

<script>
  import { Document, Money, Warning, BellFilled } from '@element-plus/icons-vue'
  import { getOverview } from '@/api/statistics'
  import { getPendingViolations } from '@/api/violation'
  import { getWarnings } from '@/api/warning'
  import { generateCapturedViolations, generateDemoWarnings } from '@/api/simulate'
  
  export default {
    name: 'HomePage',
    components: {
      Document,
      Money,
      Warning,
      BellFilled,
    },
    data() {
      return {
        loadingAll: false,
        generatingDemo: false,

        overview: {
          totalViolations: 0,
          totalFines: 0,
          totalPoints: 0,
          averageFine: 0,
        },

        pending: [],
        loadingPending: false,

        warnings: [],
        loadingWarnings: false,
      }
    },
    mounted() {
      this.loadAll()
    },
    computed: {
      currentUser() {
        try {
          const s = localStorage.getItem('user')
          return s ? JSON.parse(s) : null
        } catch (e) {
          return null
        }
      },
      isAdmin() {
        const r = this.currentUser?.role || ''
        return r === 'ADMIN' || r === 'admin' || r === '管理员'
      },
      avgFine() {
        const n = Number(this.overview.averageFine || 0)
        return n.toFixed(2)
      },
      totalFinesText() {
        const n = Number(this.overview.totalFines || 0)
        return n.toFixed(2)
      },
      pendingCount() {
        return (this.pending || []).length
      },
      pendingPreview() {
        return (this.pending || []).slice(0, 8)
      },
      warningCounts() {
        const base = { BLUE: 0, YELLOW: 0, ORANGE: 0, RED: 0 }
        ;(this.warnings || []).forEach(w => {
          const lv = w?.level
          if (base[lv] !== undefined) base[lv] += 1
        })
        return base
      },
      warningPreview() {
        return (this.warnings || []).slice(0, 8)
      },
    },
    methods: {
      // 跳转到添加违章记录页面
      navigateToAddViolation() {
        this.$router.push('/add-violation')
      },
  
      // 跳转到违章记录列表页面
      navigateToViolationList() {
        this.$router.push('/violations')
      },
  
      // 跳转到统计分析页面
      navigateToStatistics() {
        this.$router.push('/statistics')
      },

      navigateToPending() {
        this.$router.push('/violations/pending')
      },
      navigateToWarnings() {
        this.$router.push('/warnings')
      },
      navigateToCameras() {
        this.$router.push('/cameras')
      },

      levelText(level) {
        const map = { BLUE: '蓝', YELLOW: '黄', ORANGE: '橙', RED: '红' }
        return map[level] || level || '-'
      },
      levelTagType(level) {
        const map = { BLUE: 'primary', YELLOW: 'warning', ORANGE: 'warning', RED: 'danger' }
        return map[level] || 'info'
      },
      formatTime(t) {
        if (!t) return '-'
        try {
          const d = typeof t === 'string' ? new Date(t) : new Date(Number(t))
          return isNaN(d.getTime()) ? t : d.toLocaleString()
        } catch (e) {
          return t
        }
      },

      async loadAll() {
        this.loadingAll = true
        try {
          await Promise.all([this.loadOverview(), this.loadPending(), this.loadWarnings()])
        } finally {
          this.loadingAll = false
        }
      },
      async loadOverview() {
        try {
          const res = await getOverview()
          this.overview = res.data || this.overview
        } catch (e) {
          console.error('获取总览失败', e)
        }
      },
      async loadPending() {
        this.loadingPending = true
        try {
          const res = await getPendingViolations()
          this.pending = res.data || []
        } catch (e) {
          console.error('获取待处理失败', e)
          this.pending = []
        } finally {
          this.loadingPending = false
        }
      },
      async loadWarnings() {
        this.loadingWarnings = true
        try {
          const res = await getWarnings()
          this.warnings = res.data || []
        } catch (e) {
          console.error('获取预警失败', e)
          this.warnings = []
        } finally {
          this.loadingWarnings = false
        }
      },

      async generateDemoAll() {
        this.generatingDemo = true
        try {
          await Promise.all([
            generateCapturedViolations(20),
            generateDemoWarnings(5),
          ])
          this.$message.success('已生成演示数据（抓拍待处理 + 四级预警）')
          await this.loadAll()
        } catch (e) {
          this.$message.error('生成演示数据失败')
          console.error(e)
        } finally {
          this.generatingDemo = false
        }
      },
    },
  }
  </script>
  


<style scoped>
.dashboard {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 18px;
}

.dashboard-inner {
  max-width: 1400px;
  margin: 0 auto;
}

.hero {
  border-radius: 16px;
  padding: 18px 18px 14px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  box-shadow: 0 10px 26px rgba(102, 126, 234, 0.28);
  margin-bottom: 14px;
}

.dash-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.dash-title {
  font-size: 22px;
  font-weight: 800;
  color: rgba(255, 255, 255, 0.98);
}

.dash-sub {
  margin-top: 6px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.9);
}

.dash-actions {
  display: flex;
  gap: 10px;
}

.panel {
  border-radius: 14px;
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 10px;
}

.panel-title {
  font-size: 16px;
  font-weight: 800;
  color: #2c3e50;
}

.panel-desc {
  font-size: 12px;
  color: #909399;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
}

.quick-btn {
  border-radius: 10px;
  height: 40px;
}

.cards-row {
  margin: 14px 0;
}

.metric-card {
  border-radius: 14px;
}

.metric {
  display: flex;
  align-items: center;
  gap: 12px;
}

.metric-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
}

.metric-icon.primary {
  background: linear-gradient(135deg, #5b8ff9, #5ad8a6);
}
.metric-icon.warning {
  background: linear-gradient(135deg, #f6bd16, #f08c00);
}
.metric-icon.danger {
  background: linear-gradient(135deg, #1890ff, #69c0ff);
}
.metric-icon.red {
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
}

.metric-main {
  flex: 1;
}

.metric-value {
  font-size: 24px;
  font-weight: 900;
  color: #2c3e50;
  line-height: 1.2;
}

.metric-label {
  font-size: 12px;
  color: #606266;
  margin-top: 2px;
}

.metric-sub {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}

@media (max-width: 1200px) {
  .quick-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}
</style>
