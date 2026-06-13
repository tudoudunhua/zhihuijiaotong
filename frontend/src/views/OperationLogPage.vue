<template>
  <div class="log-page">
    <el-card>
      <template #header>
        <div class="head">
          <span>系统操作日志</span>
          <el-button size="small" @click="loadLogs">刷新</el-button>
        </div>
      </template>

      <el-form :inline="true" class="filter-form" @submit.prevent>
        <el-form-item label="用户名">
          <el-input v-model="filters.username" clearable placeholder="操作人" style="width: 140px" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="filters.module" clearable placeholder="全部" style="width: 140px">
            <el-option v-for="m in moduleOptions" :key="m.value" :label="m.label" :value="m.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAIL" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作说明">
          <el-input v-model="filters.action" clearable placeholder="关键词" style="width: 160px" />
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filters.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="logs" v-loading="loading" border stripe>
        <el-table-column prop="createdTime" label="时间" width="170" />
        <el-table-column label="操作人" width="140">
          <template #default="{ row }">
            <div>{{ row.realName || row.username || '-' }}</div>
            <div v-if="row.username" class="sub">{{ row.username }}</div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            {{ roleLabel(row.role) }}
          </template>
        </el-table-column>
        <el-table-column label="模块" width="100">
          <template #default="{ row }">
            {{ moduleLabel(row.module) }}
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作" min-width="180" show-overflow-tooltip />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">
              {{ row.status === 'SUCCESS' ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="clientIp" label="IP" width="130" />
        <el-table-column prop="requestUri" label="请求路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="detail" label="备注" min-width="120" show-overflow-tooltip />
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :current-page="page + 1"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
import { getOperationLogs } from '@/api/operationLog'

const MODULE_OPTIONS = [
  { value: 'AUTH', label: '认证' },
  { value: 'USER', label: '用户' },
  { value: 'VIOLATION', label: '违章' },
  { value: 'CAMERA', label: '摄像头' },
  { value: 'WARNING', label: '预警' },
  { value: 'SIMULATE', label: '模拟' },
  { value: 'OWNER', label: '车主' },
  { value: 'SYSTEM', label: '系统' },
  { value: 'OTHER', label: '其他' },
]

export default {
  name: 'OperationLogPage',
  data() {
    return {
      loading: false,
      logs: [],
      total: 0,
      page: 0,
      size: 20,
      moduleOptions: MODULE_OPTIONS,
      filters: {
        username: '',
        module: '',
        action: '',
        status: '',
        timeRange: null,
      },
    }
  },
  mounted() {
    this.loadLogs()
  },
  methods: {
    async loadLogs() {
      this.loading = true
      try {
        const params = {
          page: this.page,
          size: this.size,
          username: this.filters.username || undefined,
          module: this.filters.module || undefined,
          action: this.filters.action || undefined,
          status: this.filters.status || undefined,
        }
        if (Array.isArray(this.filters.timeRange) && this.filters.timeRange.length === 2) {
          params.startTime = this.filters.timeRange[0]
          params.endTime = this.filters.timeRange[1]
        }
        const res = await getOperationLogs(params)
        const data = res.data || {}
        this.logs = data.items || []
        this.total = data.total || 0
      } catch (e) {
        const msg = e?.response?.data || e?.message || '加载操作日志失败'
        this.$message.error(typeof msg === 'string' ? msg : '加载操作日志失败')
      } finally {
        this.loading = false
      }
    },
    search() {
      this.page = 0
      this.loadLogs()
    },
    resetFilters() {
      this.filters = {
        username: '',
        module: '',
        action: '',
        status: '',
        timeRange: null,
      }
      this.page = 0
      this.loadLogs()
    },
    onPageChange(p) {
      this.page = Math.max(p - 1, 0)
      this.loadLogs()
    },
    onSizeChange(s) {
      this.size = s
      this.page = 0
      this.loadLogs()
    },
    moduleLabel(module) {
      const hit = MODULE_OPTIONS.find(m => m.value === module)
      return hit ? hit.label : (module || '-')
    },
    roleLabel(role) {
      const map = {
        ADMIN: '管理员',
        OFFICER: '交警处置员',
        ANALYST: '数据分析员',
        USER: '普通用户',
      }
      return map[role] || role || '-'
    },
  },
}
</script>

<style scoped>
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.filter-form {
  margin-bottom: 12px;
}
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.sub {
  font-size: 12px;
  color: #909399;
}
</style>
