<template>
  <div class="warning-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">⚠️ 违章预警列表（四级预警 · 分级推送）</h2>
        <p class="page-desc">蓝→路段网格员；黄→网格员+片区；橙→交警中队；红→交警支队+消防</p>
        <el-alert
          class="page-help"
          type="info"
          :closable="false"
          show-icon
          
          description="用于把违章高发/预测结果按蓝黄橙红四级归类展示，并给出对应推送对象，帮助交管人员快速判断优先级。"
        />
      </div>
      <div class="page-actions">
        <el-button type="warning" @click="generateDemo" :loading="generating">
          生成四级演示数据
        </el-button>
        <el-button type="primary" @click="loadWarnings" :loading="loading">刷新</el-button>
      </div>
    </div>

    <!-- 等级概览卡片 -->
    <div class="level-cards">
      <div class="level-card blue" @click="activeLevel = 'BLUE'">
        <div class="lv-title">蓝色预警</div>
        <div class="lv-count">{{ counts.BLUE }}</div>
        <div class="lv-sub">路段网格员</div>
      </div>
      <div class="level-card yellow" @click="activeLevel = 'YELLOW'">
        <div class="lv-title">黄色预警</div>
        <div class="lv-count">{{ counts.YELLOW }}</div>
        <div class="lv-sub">网格员 + 片区</div>
      </div>
      <div class="level-card orange" @click="activeLevel = 'ORANGE'">
        <div class="lv-title">橙色预警</div>
        <div class="lv-count">{{ counts.ORANGE }}</div>
        <div class="lv-sub">交警中队</div>
      </div>
      <div class="level-card red" @click="activeLevel = 'RED'">
        <div class="lv-title">红色预警</div>
        <div class="lv-count">{{ counts.RED }}</div>
        <div class="lv-sub">支队 + 消防</div>
      </div>
    </div>

    <!-- 按等级归类 -->
    <el-tabs v-model="activeLevel" type="card" class="level-tabs">
      <el-tab-pane :label="`蓝色（${counts.BLUE}）`" name="BLUE">
        <el-table v-if="grouped.BLUE.length" :data="paginated.BLUE" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="level" label="预警级别" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="levelTagType(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="预警信息" min-width="280" show-overflow-tooltip />
          <el-table-column prop="pushTarget" label="推送对象" width="170" show-overflow-tooltip />
          <el-table-column prop="location" label="地点" width="160" show-overflow-tooltip />
          <el-table-column prop="warningTime" label="预警时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.warningTime) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="蓝色预警暂无数据，可点击上方「生成四级演示数据」" />
        <div v-if="grouped.BLUE.length" class="pagination-section">
          <el-pagination
            v-model:current-page="pages.BLUE"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="grouped.BLUE.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane :label="`黄色（${counts.YELLOW}）`" name="YELLOW">
        <el-table v-if="grouped.YELLOW.length" :data="paginated.YELLOW" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="level" label="预警级别" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="levelTagType(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="预警信息" min-width="280" show-overflow-tooltip />
          <el-table-column prop="pushTarget" label="推送对象" width="170" show-overflow-tooltip />
          <el-table-column prop="location" label="地点" width="160" show-overflow-tooltip />
          <el-table-column prop="warningTime" label="预警时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.warningTime) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="黄色预警暂无数据，可点击上方「生成四级演示数据」" />
        <div v-if="grouped.YELLOW.length" class="pagination-section">
          <el-pagination
            v-model:current-page="pages.YELLOW"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="grouped.YELLOW.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane :label="`橙色（${counts.ORANGE}）`" name="ORANGE">
        <el-table v-if="grouped.ORANGE.length" :data="paginated.ORANGE" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="level" label="预警级别" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="levelTagType(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="预警信息" min-width="280" show-overflow-tooltip />
          <el-table-column prop="pushTarget" label="推送对象" width="170" show-overflow-tooltip />
          <el-table-column prop="location" label="地点" width="160" show-overflow-tooltip />
          <el-table-column prop="warningTime" label="预警时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.warningTime) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="橙色预警暂无数据，可点击上方「生成四级演示数据」" />
        <div v-if="grouped.ORANGE.length" class="pagination-section">
          <el-pagination
            v-model:current-page="pages.ORANGE"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="grouped.ORANGE.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
          />
        </div>
      </el-tab-pane>
      <el-tab-pane :label="`红色（${counts.RED}）`" name="RED">
        <el-table v-if="grouped.RED.length" :data="paginated.RED" border stripe style="width: 100%">
          <el-table-column prop="id" label="ID" width="70" align="center" />
          <el-table-column prop="level" label="预警级别" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="levelTagType(row.level)" size="small">{{ levelText(row.level) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="预警信息" min-width="280" show-overflow-tooltip />
          <el-table-column prop="pushTarget" label="推送对象" width="170" show-overflow-tooltip />
          <el-table-column prop="location" label="地点" width="160" show-overflow-tooltip />
          <el-table-column prop="warningTime" label="预警时间" width="180">
            <template #default="{ row }">
              {{ formatTime(row.warningTime) }}
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="红色预警暂无数据，可点击上方「生成四级演示数据」" />
        <div v-if="grouped.RED.length" class="pagination-section">
          <el-pagination
            v-model:current-page="pages.RED"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            :total="grouped.RED.length"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handlePageSizeChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getWarnings } from '@/api/warning'
import { generateDemoWarnings } from '@/api/simulate'

export default {
  name: 'WarningPage',
  data() {
    return {
      warnings: [],
      activeLevel: 'RED',
      loading: false,
      generating: false,
      pageSize: 20,
      pages: {
        BLUE: 1,
        YELLOW: 1,
        ORANGE: 1,
        RED: 1,
      },
    }
  },
  computed: {
    grouped() {
      const base = { BLUE: [], YELLOW: [], ORANGE: [], RED: [] }
      ;(this.warnings || []).forEach(w => {
        const lv = w?.level || ''
        if (base[lv]) base[lv].push(w)
      })
      return base
    },
    counts() {
      return {
        BLUE: this.grouped.BLUE.length,
        YELLOW: this.grouped.YELLOW.length,
        ORANGE: this.grouped.ORANGE.length,
        RED: this.grouped.RED.length,
      }
    },
    paginated() {
      const sliceBy = (rows, page) => {
        const p = Math.max(1, Number(page) || 1)
        const start = (p - 1) * this.pageSize
        return rows.slice(start, start + this.pageSize)
      }
      return {
        BLUE: sliceBy(this.grouped.BLUE, this.pages.BLUE),
        YELLOW: sliceBy(this.grouped.YELLOW, this.pages.YELLOW),
        ORANGE: sliceBy(this.grouped.ORANGE, this.pages.ORANGE),
        RED: sliceBy(this.grouped.RED, this.pages.RED),
      }
    },
  },
  mounted() {
    this.loadWarnings()
  },
  methods: {
    clampPages() {
      const clamp = (total, pageSize, current) => {
        const maxPage = Math.max(1, Math.ceil((total || 0) / (pageSize || 1)))
        return Math.min(Math.max(1, current || 1), maxPage)
      }
      this.pages.BLUE = clamp(this.grouped.BLUE.length, this.pageSize, this.pages.BLUE)
      this.pages.YELLOW = clamp(this.grouped.YELLOW.length, this.pageSize, this.pages.YELLOW)
      this.pages.ORANGE = clamp(this.grouped.ORANGE.length, this.pageSize, this.pages.ORANGE)
      this.pages.RED = clamp(this.grouped.RED.length, this.pageSize, this.pages.RED)
    },
    handlePageSizeChange(val) {
      this.pageSize = val
      this.pages.BLUE = 1
      this.pages.YELLOW = 1
      this.pages.ORANGE = 1
      this.pages.RED = 1
    },
    async loadWarnings() {
      this.loading = true
      try {
        const res = await getWarnings()
        this.warnings = res.data || []

        // 默认切到“有数据”的等级，更直观
        const order = ['RED', 'ORANGE', 'YELLOW', 'BLUE']
        const first = order.find(lv => (this.grouped[lv] || []).length > 0)
        if (first) this.activeLevel = first
        this.clampPages()
      } catch (err) {
        console.error('获取预警失败', err)
        this.$message?.error?.('获取预警数据失败')
      } finally {
        this.loading = false
      }
    },
    async generateDemo() {
      this.generating = true
      try {
        await generateDemoWarnings(5)
        this.$message.success('已生成四级演示预警（每级 5 条）')
        await this.loadWarnings()
      } catch (e) {
        this.$message.error('生成演示预警失败')
        console.error(e)
      } finally {
        this.generating = false
      }
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
  },
}
</script>

<style scoped>
.warning-page {
  padding: 20px;
}
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}
.page-title {
  margin: 0;
}
.page-desc {
  color: #666;
  font-size: 13px;
  margin-bottom: 16px;
}
.page-actions {
  display: flex;
  gap: 10px;
}
.page-help {
  margin-top: 10px;
  max-width: 920px;
}

.level-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin: 10px 0 14px;
}
.level-card {
  cursor: pointer;
  border-radius: 12px;
  padding: 14px 14px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  background: #fff;
  transition: transform 0.12s ease, box-shadow 0.12s ease;
}
.level-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
}
.lv-title {
  font-size: 14px;
  font-weight: 700;
  color: #2c3e50;
}
.lv-count {
  font-size: 26px;
  font-weight: 800;
  margin-top: 6px;
}
.lv-sub {
  font-size: 12px;
  color: #606266;
  margin-top: 6px;
}
.level-card.blue .lv-count { color: #409eff; }
.level-card.yellow .lv-count { color: #e6a23c; }
.level-card.orange .lv-count { color: #f59e0b; }
.level-card.red .lv-count { color: #f56c6c; }

.level-tabs {
  background: #fff;
  border-radius: 12px;
  padding: 10px 10px 4px;
}

.pagination-section {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}
</style>
