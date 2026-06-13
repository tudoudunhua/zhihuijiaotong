<template>
  <div class="list-container">
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><VideoCamera /></el-icon>
            摄像头设备列表
          </span>
          <div class="header-actions">
            <el-button type="primary" @click="navigateToAdd">
              <el-icon><Plus /></el-icon>
              添加摄像头
            </el-button>
            <el-button @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
              返回首页
            </el-button>
          </div>
        </div>
      </template>

      <div class="stats-row">
        <div class="stat-item">
          <span class="stat-label">设备总数</span>
          <span class="stat-value">{{ cameras.length }}</span>
        </div>
        <div class="stat-item">
          <span class="stat-label">在线</span>
          <span class="stat-value success">{{ onlineCount }}</span>
          <span class="stat-dot success"></span>
        </div>
        <div class="stat-item">
          <span class="stat-label">离线</span>
          <span class="stat-value info">{{ offlineCount }}</span>
          <span class="stat-dot info"></span>
        </div>
        <div class="stat-item">
          <span class="stat-label">故障</span>
          <span class="stat-value danger">{{ faultCount }}</span>
          <span class="stat-dot danger"></span>
        </div>
      </div>

      <el-table
        :data="cameras"
        v-loading="loading"
        border
        stripe
        style="width: 100%"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column prop="cameraName" label="摄像头名称" min-width="160" />

        <el-table-column prop="location" label="安装位置" min-width="200" show-overflow-tooltip />

        <el-table-column prop="ipAddress" label="IP 地址" min-width="150" />

        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              size="small"
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="installTime" label="安装时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.installTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="lastHeartbeat" label="最后心跳" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.lastHeartbeat) || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="success" size="small" link @click="openMonitor(row)">
              <el-icon><VideoCamera /></el-icon>
              查看监控
            </el-button>
            <el-button type="primary" size="small" link @click="editCamera(row)">
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button type="danger" size="small" link @click="deleteCameraRow(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <CameraMonitor v-model="monitorVisible" :camera="currentCamera" />
  </div>
</template>

<script>
import { VideoCamera, Plus, ArrowLeft, Edit, Delete } from '@element-plus/icons-vue'
import { getCameras, deleteCamera } from '@/api/camera'
import CameraMonitor from './CameraMonitor.vue'

export default {
  name: 'CameraList',
  components: {
    VideoCamera,
    Plus,
    ArrowLeft,
    Edit,
    Delete,
    CameraMonitor,
  },
  data() {
    return {
      loading: false,
      cameras: [],
      monitorVisible: false,
      currentCamera: null,
    }
  },
  computed: {
    onlineCount() {
      return this.cameras.filter(c => this.statusText(c.status) === '在线').length
    },
    offlineCount() {
      return this.cameras.filter(c => this.statusText(c.status) === '离线').length
    },
    faultCount() {
      return this.cameras.filter(c => this.statusText(c.status) === '故障').length
    },
  },
  mounted() {
    this.fetchCameras()
  },
  methods: {
    async fetchCameras() {
      this.loading = true
      try {
        const res = await getCameras()
        this.cameras = res.data || []
      } catch (e) {
        this.$message.error('获取摄像头列表失败')
        console.error(e)
      } finally {
        this.loading = false
      }
    },

    navigateToAdd() {
      this.$router.push('/cameras/add')
    },

    goBack() {
      this.$router.push('/')
    },

    openMonitor(row) {
      this.currentCamera = row
      this.monitorVisible = true
    },

    editCamera(row) {
      this.$router.push(`/cameras/${row.cameraId}`)
    },

    async deleteCameraRow(row) {
      try {
        await this.$confirm(
          `确定要删除摄像头【${row.cameraName}】吗？`,
          '确认删除',
          { type: 'warning' },
        )
        await deleteCamera(row.cameraId)
        this.$message.success('删除成功')
        this.fetchCameras()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('删除失败')
          console.error(e)
        }
      }
    },

    formatDateTime(val) {
      if (!val) return ''
      try {
        const d = typeof val === 'string' ? new Date(val) : val
        const pad = n => String(n).padStart(2, '0')
        return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
          d.getHours(),
        )}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
      } catch (e) {
        return val
      }
    },

    statusText(status) {
      if (!status) return '未知'
      const s = String(status).toUpperCase()
      if (s === 'ONLINE' || s === '在线') return '在线'
      if (s === 'OFFLINE' || s === '离线') return '离线'
      if (s === 'FAULT' || s === 'MAINTENANCE' || s === '维护' || s === '故障') return '故障'
      if (s.includes('ONLINE') || s.includes('ONINE')) return '在线'
      if (s.includes('OFFLINE')) return '离线'
      if (s.includes('FAULT') || s.includes('MAINTENANCE') || s.includes('MAINT')) return '故障'
      return status
    },

    statusTagType(status) {
      if (!status) return 'info'
      const s = String(status).toUpperCase()
      if (s === 'ONLINE' || s.includes('ONLINE') || s.includes('ONINE')) return 'success'
      if (s === 'FAULT' || s === 'MAINTENANCE' || s.includes('FAULT') || s.includes('MAINT')) return 'danger'
      return 'info'
    },
  },
}
</script>

<style scoped>
.list-container {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 30px 20px;
}

.list-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.stats-row {
  display: flex;
  gap: 32px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #f8fafc 0%, #f1f5f9 100%);
  border-radius: 10px;
}

.stats-row .stat-item { display: flex; align-items: center; gap: 6px; }
.stats-row .stat-label { font-size: 13px; color: #64748b; }
.stats-row .stat-value { font-size: 22px; font-weight: 600; color: #1e293b; }
.stats-row .stat-value.success { color: #67c23a; }
.stats-row .stat-value.info { color: #909399; }
.stats-row .stat-value.danger { color: #f56c6c; }
.stat-dot { display: inline-block; width: 8px; height: 8px; border-radius: 50%; }
.stat-dot.success { background: #67c23a; }
.stat-dot.info { background: #909399; }
.stat-dot.danger { background: #f56c6c; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 20px;
  font-weight: bold;
  color: #2c3e50;
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-actions {
  display: flex;
  gap: 10px;
}
</style>


