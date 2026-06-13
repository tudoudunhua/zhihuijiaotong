<template>
  <div class="list-container">
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><Bell /></el-icon>
            待处理违章（摄像头抓拍）
          </span>
          <div class="header-actions">
            <el-button type="warning" @click="generateDemo" :loading="generating">
              <el-icon><Plus /></el-icon>
              生成模拟抓拍
            </el-button>
            <el-button type="primary" @click="fetchData">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
            <el-button @click="goBack">
              <el-icon><ArrowLeft /></el-icon>
              返回首页
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选栏 -->
      <div class="filter-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-input
              v-model="filters.plateNumber"
              placeholder="搜索车牌号"
              clearable
              @clear="handleFilter"
              @keyup.enter="handleFilter"
            />
          </el-col>
          <el-col :span="6">
            <el-select
              v-model="filters.violationType"
              placeholder="筛选违章类型"
              clearable
              @change="handleFilter"
              style="width: 100%"
            >
              <el-option
                v-for="t in availableTypes"
                :key="t"
                :label="t"
                :value="t"
              />
            </el-select>
          </el-col>
          <el-col :span="10">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleFilter"
              style="width: 100%"
            />
          </el-col>
          <el-col :span="2">
            <div class="filter-tip">
              <!-- 占位，让布局更紧凑 -->
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="summary-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="summary-item">
              <span class="summary-label">待处理数量：</span>
              <span class="summary-value danger">{{ filteredRows.length }}</span>
            </div>
          </el-col>
          <el-col :span="18">
            <div class="summary-tip">
              系统将按车主信息自动选择「短信 / 电话 / 留言」通知方式；若缺少车主联系方式，可在通知弹窗中补录。
            </div>
          </el-col>
        </el-row>
      </div>

      <el-table
        :data="paginatedRows"
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        :default-sort="{ prop: 'violationTime', order: 'descending' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />

        <el-table-column prop="plateNumber" label="车牌号" width="120" sortable>
          <template #default="{ row }">
            <el-tag type="info">{{ row.plateNumber }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="violationType" label="违章类型" width="150" sortable />

        <el-table-column prop="violationTime" label="违章时间" width="180" sortable>
          <template #default="{ row }">
            {{ formatDateTime(row.violationTime) }}
          </template>
        </el-table-column>

        <el-table-column prop="violationLocation" label="位置" min-width="220">
          <template #default="{ row }">
            {{ row.violationLocation || '-' }}
          </template>
        </el-table-column>

        <el-table-column prop="fineAmount" label="罚款" width="110" align="right">
          <template #default="{ row }">
            ¥{{ row.fineAmount?.toFixed(2) || '0.00' }}
          </template>
        </el-table-column>

        <el-table-column prop="pointsDeducted" label="扣分" width="90" align="center">
          <template #default="{ row }">
            {{ row.pointsDeducted || 0 }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" size="small" link @click="openNotify(row)">
              <el-icon><ChatDotRound /></el-icon>
              通知车主
            </el-button>
            <el-button type="success" size="small" link @click="markProcessed(row)">
              <el-icon><CircleCheck /></el-icon>
              标记已处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-section">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="filteredRows.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePageSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 通知弹窗 -->
    <el-dialog v-model="notifyVisible" title="通知车主" width="520px" @close="resetNotify">
      <div v-if="currentRow" class="notify-meta">
        <div class="meta-line">
          <span class="meta-k">车牌：</span>
          <span class="meta-v">{{ currentRow.plateNumber }}</span>
        </div>
        <div class="meta-line">
          <span class="meta-k">违章：</span>
          <span class="meta-v">{{ currentRow.violationType }}</span>
        </div>
        <div class="meta-line">
          <span class="meta-k">时间：</span>
          <span class="meta-v">{{ formatDateTime(currentRow.violationTime) }}</span>
        </div>
      </div>

      <el-form label-width="90px">
        <el-form-item label="通知方式">
          <el-radio-group v-model="notifyForm.channel">
            <el-radio label="AUTO">自动选择</el-radio>
            <el-radio label="SMS">短信</el-radio>
            <el-radio label="PHONE">电话</el-radio>
            <el-radio label="MESSAGE">留言</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="留言内容">
          <el-input
            v-model="notifyForm.message"
            type="textarea"
            :rows="4"
            placeholder="可不填，系统会自动生成通知内容"
          />
        </el-form-item>
      </el-form>

      <el-divider />

      <div class="owner-box">
        <div class="owner-title">车主信息（缺少时可补录）</div>
        <el-form label-width="90px">
          <el-form-item label="车主姓名">
            <el-input v-model="ownerForm.ownerName" placeholder="例如：张三" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="ownerForm.phoneNumber" placeholder="用于短信/电话通知" />
          </el-form-item>
          <el-form-item label="允许渠道">
            <el-checkbox v-model="ownerForm.allowSms">短信</el-checkbox>
            <el-checkbox v-model="ownerForm.allowPhone">电话</el-checkbox>
            <el-checkbox v-model="ownerForm.allowMessage">留言</el-checkbox>
          </el-form-item>
        </el-form>
        <div class="owner-actions">
          <el-button @click="loadOwner" :loading="ownerLoading">读取已录入</el-button>
          <el-button type="primary" @click="saveOwner" :loading="ownerSaving">保存车主信息</el-button>
        </div>
      </div>

      <template #footer>
        <el-button @click="notifyVisible = false">取消</el-button>
        <el-button type="primary" :loading="notifying" @click="doNotify">发送通知</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ArrowLeft, Bell, Refresh, ChatDotRound, CircleCheck, Plus } from '@element-plus/icons-vue'
import { getPendingViolations, processViolation, notifyViolationOwner } from '@/api/violation'
import { getOwnerByPlate, upsertOwner } from '@/api/owner'
import { generateCapturedViolations } from '@/api/simulate'

export default {
  name: 'PendingViolationList',
  components: { ArrowLeft, Bell, Refresh, ChatDotRound, CircleCheck, Plus },
  data() {
    return {
      loading: false,
      generating: false,
      rows: [],
      filters: {
        plateNumber: '',
        violationType: '',
      },
      dateRange: null,
      currentPage: 1,
      pageSize: 20,

      notifyVisible: false,
      notifying: false,
      currentRow: null,
      notifyForm: {
        channel: 'AUTO',
        message: '',
      },

      ownerLoading: false,
      ownerSaving: false,
      ownerForm: {
        ownerName: '',
        phoneNumber: '',
        allowSms: true,
        allowPhone: true,
        allowMessage: true,
      },
    }
  },
  computed: {
    availableTypes() {
      const s = new Set()
      ;(this.rows || []).forEach(r => {
        if (r?.violationType) s.add(r.violationType)
      })
      return Array.from(s).sort()
    },
    filteredRows() {
      let result = [...(this.rows || [])]

      if (this.filters.plateNumber) {
        const q = this.filters.plateNumber.toLowerCase()
        result = result.filter(v => String(v?.plateNumber || '').toLowerCase().includes(q))
      }

      if (this.filters.violationType) {
        result = result.filter(v => v.violationType === this.filters.violationType)
      }

      if (this.dateRange && this.dateRange.length === 2) {
        const [startDate, endDate] = this.dateRange
        result = result.filter(v => {
          const d = this.getDatePart(v?.violationTime)
          if (!d) return false
          return d >= startDate && d <= endDate
        })
      }

      return result
    },
    paginatedRows() {
      const start = (this.currentPage - 1) * this.pageSize
      return this.filteredRows.slice(start, start + this.pageSize)
    },
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    async generateDemo() {
      this.generating = true
      try {
        await generateCapturedViolations(10)
        this.$message.success('已生成 10 条模拟抓拍违章')
        await this.fetchData()
      } catch (e) {
        this.$message.error('生成失败')
        console.error(e)
      } finally {
        this.generating = false
      }
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await getPendingViolations()
        this.rows = res.data || []
      } catch (e) {
        this.$message.error('获取待处理列表失败')
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    handleFilter() {
      // 使用 computed 本地过滤即可，这里保留钩子便于后续扩展为后端筛选
      this.currentPage = 1
    },
    handlePageSizeChange(val) {
      this.pageSize = val
      this.currentPage = 1
    },
    handlePageChange(val) {
      this.currentPage = val
    },
    goBack() {
      this.$router.push('/')
    },
    formatDateTime(dateString) {
      if (!dateString) return '-'
      return String(dateString).replace('T', ' ').substring(0, 19)
    },
    getDatePart(dateString) {
      if (!dateString) return null
      const s = String(dateString)
      // 兼容 "2026-03-06T01:56:42" / "2026-03-06 01:56:42"
      if (s.length >= 10) return s.substring(0, 10)
      return null
    },

    openNotify(row) {
      this.currentRow = row
      this.notifyVisible = true
      this.loadOwner()
    },
    resetNotify() {
      this.notifying = false
      this.currentRow = null
      this.notifyForm = { channel: 'AUTO', message: '' }
      this.ownerForm = { ownerName: '', phoneNumber: '', allowSms: true, allowPhone: true, allowMessage: true }
    },

    async loadOwner() {
      if (!this.currentRow?.plateNumber) return
      this.ownerLoading = true
      try {
        const res = await getOwnerByPlate(this.currentRow.plateNumber)
        const o = res.data
        this.ownerForm.ownerName = o?.ownerName || ''
        this.ownerForm.phoneNumber = o?.phoneNumber || ''
        this.ownerForm.allowSms = o?.allowSms !== false
        this.ownerForm.allowPhone = o?.allowPhone !== false
        this.ownerForm.allowMessage = o?.allowMessage !== false
      } catch (e) {
        // 404 正常：说明没录入过
      } finally {
        this.ownerLoading = false
      }
    },

    async saveOwner() {
      if (!this.currentRow?.plateNumber) return
      this.ownerSaving = true
      try {
        await upsertOwner({
          plateNumber: this.currentRow.plateNumber,
          ownerName: this.ownerForm.ownerName,
          phoneNumber: this.ownerForm.phoneNumber,
          allowSms: this.ownerForm.allowSms,
          allowPhone: this.ownerForm.allowPhone,
          allowMessage: this.ownerForm.allowMessage,
          preferredChannel: 'AUTO',
        })
        this.$message.success('车主信息已保存')
      } catch (e) {
        this.$message.error('保存车主信息失败')
        console.error(e)
      } finally {
        this.ownerSaving = false
      }
    },

    async doNotify() {
      if (!this.currentRow?.violationId) return
      this.notifying = true
      try {
        const res = await notifyViolationOwner(this.currentRow.violationId, {
          channel: this.notifyForm.channel,
          message: this.notifyForm.message,
        })
        const r = res.data || {}
        if (r.status === 'SENT') {
          this.$message.success(`通知成功（${r.channel || 'AUTO'}）`)
          this.notifyVisible = false
        } else {
          this.$message.warning(r.content || '通知失败，请先补录车主信息')
        }
      } catch (e) {
        const msg = e?.response?.data || '通知失败'
        this.$message.error(typeof msg === 'string' ? msg : '通知失败')
        console.error(e)
      } finally {
        this.notifying = false
      }
    },

    async markProcessed(row) {
      if (!row?.violationId) return
      try {
        await this.$confirm('确认标记为已处理吗？', '提示', { type: 'warning' })
        await processViolation(row.violationId)
        this.$message.success('已标记处理')
        this.fetchData()
      } catch (e) {
        if (e !== 'cancel') {
          this.$message.error('标记失败')
          console.error(e)
        }
      }
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

.filter-section {
  margin-bottom: 12px;
  padding: 14px 18px;
  background: #fafafa;
  border-radius: 8px;
}

.filter-tip {
  color: #606266;
  font-size: 13px;
  line-height: 32px;
}

.pagination-section {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.summary-section {
  margin-bottom: 16px;
  padding: 14px 18px;
  background: #fff7ed;
  border-radius: 8px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.summary-label {
  color: #606266;
  font-size: 14px;
}

.summary-value {
  font-size: 18px;
  font-weight: bold;
  color: #2c3e50;
}

.summary-value.danger {
  color: #f56c6c;
}

.summary-tip {
  color: #8a6d3b;
  font-size: 13px;
  line-height: 22px;
}

.notify-meta {
  margin-bottom: 10px;
  padding: 10px 12px;
  background: #f5f7fa;
  border-radius: 8px;
}
.meta-line {
  display: flex;
  gap: 10px;
  line-height: 22px;
}
.meta-k {
  width: 56px;
  color: #606266;
}
.meta-v {
  color: #2c3e50;
  font-weight: 600;
}

.owner-box {
  padding: 10px 12px;
  background: #f0f9ff;
  border-radius: 8px;
}
.owner-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #2c3e50;
}
.owner-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>

