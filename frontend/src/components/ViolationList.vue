<template>
  <div class="list-container">
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">
            <el-icon><List /></el-icon>
            违章记录列表
          </span>
          <div class="header-actions">
            <el-button 
              type="primary" 
              @click="navigateToAdd"
            >
              <el-icon><Plus /></el-icon>
              添加记录
            </el-button>
            <el-button 
              @click="goBack"
            >
              <el-icon><ArrowLeft /></el-icon>
              返回首页
            </el-button>
          </div>
        </div>
      </template>

      <!-- 搜索和筛选区域 -->
      <div class="filter-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-input
              v-model="searchForm.plateNumber"
              placeholder="搜索车牌号"
              clearable
              @clear="handleSearch"
              @keyup.enter="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :span="6">
            <el-select
              v-model="searchForm.violationType"
              placeholder="筛选违章类型"
              clearable
              @change="handleSearch"
              style="width: 100%"
            >
              <el-option
                v-for="type in violationTypes"
                :key="type"
                :label="type"
                :value="type"
              />
            </el-select>
          </el-col>
          <el-col :span="8">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleSearch"
              style="width: 100%"
            />
          </el-col>
          <el-col :span="4">
            <el-button 
              type="primary" 
              @click="handleSearch"
              style="width: 100%"
            >
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 统计信息 -->
      <div class="summary-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <div class="summary-item">
              <span class="summary-label">总记录数：</span>
              <span class="summary-value">{{ filteredViolations.length }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="summary-item">
              <span class="summary-label">总罚款：</span>
              <span class="summary-value warning">¥{{ totalFines }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="summary-item">
              <span class="summary-label">总扣分：</span>
              <span class="summary-value danger">{{ totalPoints }}</span>
            </div>
          </el-col>
          <el-col :span="6">
            <el-button 
              type="success" 
              @click="exportData"
              :loading="exporting"
            >
              <el-icon><Download /></el-icon>
              导出数据
            </el-button>
          </el-col>
        </el-row>
      </div>

      <!-- 数据表格 -->
      <el-table 
        :data="paginatedViolations" 
        style="width: 100%"
        v-loading="loading"
        stripe
        border
        :default-sort="{ prop: 'violationTime', order: 'descending' }"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column 
          prop="plateNumber" 
          label="车牌号" 
          width="120"
          sortable
        >
          <template #default="{ row }">
            <el-tag type="info">{{ row.plateNumber }}</el-tag>
          </template>
        </el-table-column>
        
        <el-table-column 
          prop="violationType" 
          label="违章类型" 
          width="150"
          sortable
        >
          <template #default="{ row }">
            <el-tag :type="getViolationTypeTag(row.violationType)">
              {{ row.violationType }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column 
          prop="violationTime" 
          label="违章时间" 
          width="180"
          sortable
        >
          <template #default="{ row }">
            {{ formatDateTime(row.violationTime) }}
          </template>
        </el-table-column>
        
        <el-table-column 
          prop="fineAmount" 
          label="罚款金额" 
          width="120"
          sortable
          align="right"
        >
          <template #default="{ row }">
            <span class="fine-amount">¥{{ row.fineAmount?.toFixed(2) || '0.00' }}</span>
          </template>
        </el-table-column>
        
        <el-table-column 
          prop="pointsDeducted" 
          label="扣分" 
          width="100"
          sortable
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="getPointsTag(row.pointsDeducted)">
              {{ row.pointsDeducted || 0 }}分
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column
          prop="violationLocation"
          label="位置"
          width="200"
        >
          <template #default="{ row }">
            <span>{{ row.violationLocation || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column
          prop="warningLevel"
          label="预警等级"
          width="120"
          align="center"
        >
          <template #default="{ row }">
            <el-tag :type="row.warningLevel === 'RED' ? 'danger' : (row.warningLevel === 'ORANGE' ? 'warning' : 'info')">
              {{ row.warningLevel || '-' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click="editViolation(row)"
              link
            >
              <el-icon><Edit /></el-icon>
              编辑
            </el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="deleteViolation(row)"
              link
            >
              <el-icon><Delete /></el-icon>
              删除
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
          :total="filteredViolations.length"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script>
  import { List, Plus, ArrowLeft, Search, Download, Edit, Delete } from '@element-plus/icons-vue'
  import { getViolations, deleteViolation } from '@/api/violation'
  import * as XLSX from 'xlsx'
  
  export default {
    name: 'ViolationList',
    components: {
      List,
      Plus,
      ArrowLeft,
      Search,
      Download,
      Edit,
      Delete,
    },
    data() {
      return {
        loading: false,
        exporting: false,
        violations: [],
        searchForm: {
          plateNumber: '',
          violationType: '',
        },
        dateRange: null,
        violationTypes: [
          '超速行驶',
          '闯红灯',
          '违法停车',
          '未系安全带',
          '酒驾',
          '无证驾驶',
          '逆行',
          '违法变道',
          '占用应急车道',
          '其他',
        ],
        currentPage: 1,
        pageSize: 10,
      }
    },
    computed: {
      filteredViolations() {
        let result = [...this.violations]
  
        if (this.searchForm.plateNumber) {
          result = result.filter(v =>
            v.plateNumber?.toLowerCase().includes(this.searchForm.plateNumber.toLowerCase())
          )
        }
  
        if (this.searchForm.violationType) {
          result = result.filter(v => v.violationType === this.searchForm.violationType)
        }
  
        if (this.dateRange && this.dateRange.length === 2) {
          const [startDate, endDate] = this.dateRange
          result = result.filter(v => {
            if (!v.violationTime) return false
            const d = v.violationTime.split('T')[0]
            return d >= startDate && d <= endDate
          })
        }
  
        // 如果是从智能预警页跳转过来，并且携带了行政区参数，则按行政区过滤
        const district = this.$route.query?.district
        if (district) {
          result = result.filter(v =>
            (v.violationLocation || '').includes(district)
          )
        }

        // 固定按违章时间倒序，确保最新新增/更新记录优先可见
        result.sort((a, b) => {
          const ta = a?.violationTime ? new Date(a.violationTime).getTime() : 0
          const tb = b?.violationTime ? new Date(b.violationTime).getTime() : 0
          return tb - ta
        })

        return result
      },
  
      paginatedViolations() {
        const start = (this.currentPage - 1) * this.pageSize
        return this.filteredViolations.slice(start, start + this.pageSize)
      },
  
      totalFines() {
        return this.filteredViolations.reduce((s, v) => s + (v.fineAmount || 0), 0).toFixed(2)
      },
  
      totalPoints() {
        return this.filteredViolations.reduce((s, v) => s + (v.pointsDeducted || 0), 0)
      },
    },
    mounted() {
      this.applyRouteFilters()
      this.fetchViolations()
    },
    watch: {
      '$route.query': {
        handler() {
          this.applyRouteFilters()
        },
        deep: true,
      },
    },
    methods: {
      applyRouteFilters() {
        const type = this.$route.query?.type
        const month = this.$route.query?.month
        if (type) {
          this.searchForm.violationType = String(type)
        } else if (!this.searchForm.violationType) {
          this.searchForm.violationType = ''
        }

        if (month && /^\d{4}-\d{2}$/.test(String(month))) {
          const m = String(month)
          const start = `${m}-01`
          const endDate = new Date(Number(m.slice(0, 4)), Number(m.slice(5, 7)), 0)
          const end = `${m}-${String(endDate.getDate()).padStart(2, '0')}`
          this.dateRange = [start, end]
        } else if (!this.$route.query?.month) {
          this.dateRange = null
        }
        this.currentPage = 1
      },

      /** ✅ 正确获取数据 */
      async fetchViolations() {
        this.loading = true
        try {
          const res = await getViolations()
          this.violations = res.data || []
        } catch (error) {
          this.$message.error('获取记录失败')
          console.error(error)
        } finally {
          this.loading = false
        }
      },
  
      handleSearch() {
        this.currentPage = 1
      },
  
      editViolation(row) {
        this.$router.push(`/add-violation/${row.violationId}`)
      },
  
      /** ✅ 正确删除 */
      async deleteViolation(row) {
        try {
          await this.$confirm(
            `确定要删除车牌号为 ${row.plateNumber} 的违章记录吗？`,
            '确认删除',
            { type: 'warning' }
          )
  
          await deleteViolation(row.violationId)
          this.$message.success('删除成功')
          this.fetchViolations()
        } catch (e) {
          if (e !== 'cancel') {
            this.$message.error('删除失败')
            console.error(e)
          }
        }
      },
  
      formatDateTime(dateString) {
        if (!dateString) return '-'
        return dateString.replace('T', ' ').substring(0, 19)
      },
  
      getViolationTypeTag(type) {
        const map = {
          超速行驶: 'danger',
          闯红灯: 'danger',
          酒驾: 'danger',
          无证驾驶: 'danger',
          违法停车: 'warning',
          未系安全带: 'warning',
          逆行: 'warning',
          违法变道: 'info',
          占用应急车道: 'info',
        }
        return map[type] || ''
      },
  
      getPointsTag(points) {
        if (points >= 12) return 'danger'
        if (points >= 6) return 'warning'
        return 'info'
      },
  
      handleSizeChange(val) {
        this.pageSize = val
        this.currentPage = 1
      },
  
      handleCurrentChange(val) {
        this.currentPage = val
      },
  
      navigateToAdd() {
        this.$router.push('/add-violation')
      },
  
      goBack() {
        this.$router.push('/')
      },
  
      async exportData() {
        this.exporting = true
        try {
          const data = this.filteredViolations.map(v => ({
            车牌号: v.plateNumber,
            违章类型: v.violationType,
            违章时间: this.formatDateTime(v.violationTime),
            罚款金额: v.fineAmount,
            扣分: v.pointsDeducted,
          }))

          const worksheet = XLSX.utils.json_to_sheet(data)
          const workbook = XLSX.utils.book_new()
          XLSX.utils.book_append_sheet(workbook, worksheet, '违章记录')

          const wbout = XLSX.write(workbook, {
            bookType: 'xlsx',
            type: 'array',
          })

          const blob = new Blob([wbout], {
            type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
          })

          const url = URL.createObjectURL(blob)
          const a = document.createElement('a')
          a.href = url
          a.download = '违章记录.xlsx'
          a.click()
          URL.revokeObjectURL(url)

          this.$message.success('导出成功')
        } catch (e) {
          console.error(e)
          this.$message.error('导出失败')
        } finally {
          this.exporting = false
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
  margin-bottom: 20px;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.summary-section {
  margin-bottom: 20px;
  padding: 15px 20px;
  background: #f0f9ff;
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

.summary-value.warning {
  color: #e6a23c;
}

.summary-value.danger {
  color: #f56c6c;
}

.fine-amount {
  font-weight: bold;
  color: #e6a23c;
}

.pagination-section {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

:deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}

:deep(.el-button) {
  border-radius: 6px;
}
</style>
