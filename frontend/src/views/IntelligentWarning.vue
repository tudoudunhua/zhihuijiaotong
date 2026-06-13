<template>
  <div class="intelligent-page">
    <div class="page-header">
      <h1>🧠 智能违章预警中心</h1>
      <p class="subtitle">基于 AI 机器学习算法的违章趋势预测与风险评估系统</p>
    </div>

    <el-card class="control-panel" shadow="hover">
      <div class="control-group">
        <!-- 左侧筛选条件 -->
        <div class="control-left">
          <div class="control-item">
            <span class="label">时间</span>
            <el-select v-model="selectedHour" placeholder="选择小时" size="small" style="width:120px">
              <el-option v-for="h in 24" :key="h" :label="`${h - 1}:00`" :value="h - 1" />
            </el-select>
          </div>

          <div class="control-item">
            <span class="label">行政区</span>
            <el-select
              v-model="selectedDistrict"
              placeholder="全部区域"
              size="small"
              style="width: 180px"
              clearable
            >
              <el-option label="全部区域" value="0" />
              <el-option v-for="(d, idx) in districtOptions" :key="idx" :label="d" :value="d" />
            </el-select>
          </div>

          <div class="control-item">
            <span class="label">违章类型</span>
            <el-select
              v-model="selectedType"
              placeholder="全部类型"
              size="small"
              style="width: 200px"
              filterable
              clearable
            >
              <el-option label="全部类型" value="0" />
              <el-option v-for="(t, idx) in typeOptions" :key="idx" :label="t" :value="t" />
            </el-select>
          </div>
        </div>

        <!-- 右侧操作按钮 -->
        <div class="control-right">
          <el-button
            type="primary"
            icon="el-icon-data-analysis"
            size="small"
            @click="onPredict"
            :loading="loading"
          >
            立即分析
          </el-button>
          <el-button
            type="warning"
            size="small"
            plain
            @click="onGenerateWeatherWarnings"
          >
            生成天气预警
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="metrics-row" v-if="summary">
       <el-col :span="6">
          <el-card shadow="hover" class="metric-card risk-card">
             <div class="metric-icon"><i class="el-icon-warning"></i></div>
             <div class="metric-info">
               <div class="metric-title">风险等级</div>
               <div class="metric-value" :style="{color: summary.risk_color}">{{ summary.risk_level }}</div>
               <div class="metric-sub-text">
                 高 {{ levelStats.high }} / 中 {{ levelStats.medium }} / 低 {{ levelStats.low }}
               </div>
             </div>
          </el-card>
       </el-col>
       <el-col :span="6">
          <el-card shadow="hover" class="metric-card">
             <div class="metric-icon" style="background:#e1f3d8;color:#67c23a"><i class="el-icon-s-data"></i></div>
             <div class="metric-info">
               <div class="metric-title">预测峰值 ({{ summary.primary_location }})</div>
               <div class="metric-value">{{ summary.total_predicted }} <span class="unit">起</span></div>
             </div>
          </el-card>
       </el-col>
       <el-col :span="12">
          <el-card shadow="hover" class="metric-card suggestion-card prominent-suggestion">
             <div class="suggestion-header">
               <span class="suggestion-badge">核心预警建议</span>
               <div class="suggestion-title">
                 <i class="el-icon-cpu"></i> AI 智能决策建议
               </div>
             </div>
             <div class="suggestion-content">
               {{ summary.suggestion }}
             </div>
             <div class="suggestion-footer">
               <span class="footer-item">当前时段：{{ selectedHour }}:00</span>
               <span class="footer-item">峰值时段：{{ summary.peak_hour || '-' }}</span>
               <span class="footer-item">峰值数量：{{ summary.total_predicted || 0 }} 起</span>
             </div>
          </el-card>
       </el-col>
    </el-row>

    <el-row :gutter="20" class="charts-row">
      <!-- 高风险地点 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>📍 区域违章热点预测 Top 6</span>
          </template>
          <div ref="locationChart" class="chart-container"></div>
        </el-card>
      </el-col>

      <!-- 高风险时间 -->
      <el-col :span="12">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <span>⏰ 24小时违章趋势预测</span>
          </template>
          <div ref="timeChart" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 模型解释 + 最新预警记录 -->
    <el-row :gutter="20" class="detail-row">
      <el-col :span="12">
        <el-card shadow="hover" class="detail-card">
          <template #header>
            <span>🧾 模型解释与风险说明</span>
          </template>
          <div v-if="reasons && reasons.length" class="reason-list">
            <div
              v-for="(item, index) in reasons"
              :key="index"
              class="reason-item"
            >
              <div class="reason-title">
                <span class="dot"></span>
                {{ item.title }}
              </div>
              <div class="reason-desc">
                {{ item.desc }}
              </div>
            </div>
          </div>
          <div v-else class="reason-empty">
            当前暂无模型解释信息。
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="hover" class="detail-card">
          <template #header>
            <div class="detail-header">
              <span>⚠️ 最新智能预警记录</span>
              <div class="detail-header-actions">
                <el-tag
                  v-if="warningFilterLocation"
                  type="info"
                  size="small"
                  closable
                  @close="clearWarningFilter"
                >
                  过滤：{{ warningFilterLocation }}
                </el-tag>
                <el-button
                  size="small"
                  type="primary"
                  @click="exportWarnings"
                  :disabled="!filteredLatestWarnings.length"
                >
                  导出
                </el-button>
              </div>
            </div>
          </template>
          <el-table
            v-if="filteredLatestWarnings && filteredLatestWarnings.length"
            :data="filteredLatestWarnings"
            size="small"
            height="260"
            border
          >
            <el-table-column
              prop="warningTime"
              label="时间"
              width="170"
              :formatter="formatWarningTime"
            />
            <el-table-column
              prop="location"
              label="地点"
              min-width="120"
              show-overflow-tooltip
            >
              <template #default="{ row }">
                {{ formatDistrict(row.location) }}
              </template>
            </el-table-column>
            <el-table-column
              prop="violationType"
              label="违章类型"
              min-width="110"
              show-overflow-tooltip
            />
            <el-table-column
              prop="weatherFactor"
              label="天气因素"
              min-width="90"
            >
              <template #default="{ row }">
                <span v-if="row.warningType === 'Weather'">
                  {{ row.weatherFactor || '恶劣天气' }}
                </span>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column
              prop="warningType"
              label="预警来源"
              width="90"
            >
              <template #default="{ row }">
                <el-tag
                  size="small"
                  :type="row.warningType === 'Weather' ? 'warning' : (row.warningType === 'ML' ? 'success' : 'info')"
                >
                  {{ row.warningType === 'Weather' ? '天气' : (row.warningType === 'ML' ? '模型' : (row.warningType || '规则')) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="severity"
              label="级别"
              width="70"
            >
              <template #default="scope">
                <el-tag
                  :type="scope.row.severity === 'High' ? 'danger' : (scope.row.severity === 'Medium' ? 'warning' : 'success')"
                  size="small"
                >
                  {{ formatSeverity(scope.row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column
              prop="count"
              label="预计数量"
              width="90"
            />
          </el-table>
          <div v-else class="reason-empty">
            暂无智能预警记录，可先在后台触发生成或等待定时任务。
          </div>
        </el-card>
      </el-col>

      <el-col :span="24" style="margin-top: 16px;">
        <el-card shadow="hover" class="detail-card">
          <template #header>
            <span>🎯 预警处置闭环建议</span>
          </template>
          <el-timeline v-if="actions && actions.length">
            <el-timeline-item
              v-for="(item, idx) in actions"
              :key="idx"
              :type="item.priority && item.priority.includes('高') ? 'danger' : 'primary'"
              :hollow="false"
              :timestamp="item.priority || '中优先级'"
            >
              <div class="action-row">
                <div class="action-title">{{ item.title }}</div>
                <el-tag size="mini" :type="getStatusTagType(item.status)">
                  {{ item.status || '待执行' }}
                </el-tag>
              </div>
              <div class="action-detail">{{ item.detail }}</div>
              <div class="action-ops">
                <el-button size="mini" @click="updateActionStatus(idx, '待执行')">待执行</el-button>
                <el-button size="mini" type="warning" plain @click="updateActionStatus(idx, '执行中')">执行中</el-button>
                <el-button size="mini" type="success" plain @click="updateActionStatus(idx, '已完成')">已完成</el-button>
              </div>
            </el-timeline-item>
          </el-timeline>
          <div v-else class="reason-empty">
            暂无处置建议，请先执行一次智能分析。
          </div>
        </el-card>
      </el-col>

      <el-col :span="24" style="margin-top: 16px;">
        <el-card shadow="hover" class="detail-card">
          <template #header>
            <span>🚗 复犯车辆预警（近30天）</span>
          </template>
          <el-table
            v-if="repeatOffenders && repeatOffenders.length"
            :data="repeatOffenders"
            size="small"
            border
            max-height="260"
          >
            <el-table-column prop="plateNumber" label="车牌号" min-width="120" />
            <el-table-column prop="totalCount" label="复犯次数" width="100" />
            <el-table-column prop="typeDiversity" label="违章类型数" width="110" />
            <el-table-column prop="lastViolationTime" label="最近违章时间" min-width="170">
              <template #default="{ row }">
                {{ formatWarningTime(row) }}
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险级别" width="100">
              <template #default="{ row }">
                <el-tag :type="formatRepeatRiskTag(row.riskLevel)" size="small">
                  {{ row.riskLevel || '低' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div v-else class="reason-empty">
            当前筛选条件下暂无复犯车辆。
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts/core'
import { BarChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  VisualMapComponent,
  MarkPointComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([
  BarChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  VisualMapComponent,
  MarkPointComponent,
  CanvasRenderer
])

import * as XLSX from 'xlsx'
import { getIntelligentWarning, getTypes, getDistricts, getLatestIntelligentWarnings, triggerWeatherWarnings, getRepeatOffenders } from '@/api/intelligent'

export default {
  name: 'IntelligentWarning',

  data() {
    return {
      locationChart: null,
      timeChart: null,
      chartResizeTimer: null,
      loading: false,
      selectedHour: new Date().getHours(), // 默认当前小时
      selectedType: '0',
      selectedLocation: '',
      selectedDistrict: '0',
      typeOptions: [],
      districtOptions: [],
      summary: null,
      reasons: [],
      actions: [],
      latestWarnings: [],
      warningFilterLocation: '',
      repeatOffenders: [],
    }
  },

  computed: {
    filteredLatestWarnings() {
      if (!this.warningFilterLocation) {
        return this.latestWarnings || []
      }
      return (this.latestWarnings || []).filter(
        w => this.formatDistrict(w.location) === this.warningFilterLocation
      )
    },

    levelStats() {
      if (this.summary && this.summary.risk_distribution) {
        return {
          high: Number(this.summary.risk_distribution.high || 0),
          medium: Number(this.summary.risk_distribution.medium || 0),
          low: Number(this.summary.risk_distribution.low || 0)
        }
      }
      const stats = { high: 0, medium: 0, low: 0 }
      ;(this.latestWarnings || []).forEach(w => {
        const s = (w.severity || '').toLowerCase()
        if (s === 'high') stats.high += 1
        else if (s === 'medium') stats.medium += 1
        else if (s === 'low') stats.low += 1
      })
      return stats
    },
  },

  mounted() {
    this.loadTypes()
    this.loadDistricts()
    this.loadLatestWarnings()
    // 等待 DOM 和布局完成后再初始化图表，避免 cartesian2d 报错
    this.$nextTick(() => {
      let retries = 0
      const tryInit = () => {
        if (this.initCharts()) {
          this.onPredict()
        } else if (retries++ < 30) {
          setTimeout(tryInit, 50)
        } else {
          this.onPredict()
        }
      }
      setTimeout(tryInit, 150)
    })
    window.addEventListener('resize', this.handleResize)
  },

  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize)
    if (this.chartResizeTimer) clearTimeout(this.chartResizeTimer)
    if (this.locationChart) this.locationChart.dispose()
    if (this.timeChart) this.timeChart.dispose()
  },

  methods: {
    handleResize() {
      if (this.chartResizeTimer) clearTimeout(this.chartResizeTimer)
      this.chartResizeTimer = setTimeout(() => {
        this.chartResizeTimer = null
        if (this.locationChart && !this.locationChart.isDisposed()) this.locationChart.resize()
        if (this.timeChart && !this.timeChart.isDisposed()) this.timeChart.resize()
      }, 100)
    },

    initCharts() {
      if (!this.$refs.locationChart || !this.$refs.timeChart) return
      const locEl = this.$refs.locationChart
      const timeEl = this.$refs.timeChart
      if (locEl.clientWidth === 0 || timeEl.clientWidth === 0) return false
      if (this.locationChart) this.locationChart.dispose()
      if (this.timeChart) this.timeChart.dispose()
      this.locationChart = echarts.init(locEl, null, { renderer: 'canvas' })
      this.timeChart = echarts.init(timeEl, null, { renderer: 'canvas' })
      return true
    },

    async loadTypes() {
      try {
        const res = await getTypes()
        this.typeOptions = res.data || []
      } catch (e) {
        console.error('加载类型失败', e)
      }
    },

    async loadDistricts() {
      try {
        const res = await getDistricts()
        this.districtOptions = res.data || []
      } catch (e) {
        console.error('加载行政区失败', e)
      }
    },

    async loadLatestWarnings() {
      try {
        const res = await getLatestIntelligentWarnings()
        this.latestWarnings = res.data || []
      } catch (e) {
        console.error('加载最新预警失败', e)
      }
    },

    async onPredict() {
      this.loading = true
      try {
        const params = {
          hour: Number(this.selectedHour),
          type: this.selectedType || '0',
          location: this.selectedDistrict || this.selectedLocation || '0'
        }
        const res = await getIntelligentWarning(params)
        const d = res.data
        
        // 兼容旧接口数据，如果后端没有返回 summary，自己构造一个默认的
        if (!d.summary) {
           d.summary = {
             risk_level: '未知',
             risk_color: '#909399',
             total_predicted: '-',
             suggestion: '后端未返回分析建议',
             primary_location: params.location === '0' ? '全局' : params.location
           }
        }
        d.summary = this.alignSummaryWithTimes(d.summary, d.times || [], Number(this.selectedHour))

        this.summary = d.summary
        this.reasons = d.reasons || []
        this.actions = this.normalizeActions(d.actions)
        await this.loadRepeatOffenders(params)
        // 先更新布局（summary 出现），再用 nextTick 等 DOM 稳定后再渲染图表，避免 resize 与 setOption 冲突
        this.$nextTick(() => {
          this.renderLocationChart(d.locations || [])
          this.renderTimeChart(d.times || [])
        })
      } catch (e) {
        this.$message.error('预测分析失败，请稍后重试')
        console.error(e)
      } finally {
        this.loading = false
      }
    },

    normalizeActions(rawActions) {
      const fallback = [
        { title: '警力布控', detail: '建议在高风险时段前 30 分钟完成重点路段警力前置。', priority: '中优先级', status: '待执行' },
        { title: '执法策略', detail: '建议对热点区域开展电子抓拍与现场巡逻联合治理。', priority: '中优先级', status: '待执行' },
        { title: '公众提醒', detail: '建议通过诱导屏/公众号发布高风险时段提醒。', priority: '中优先级', status: '待执行' }
      ]
      const list = Array.isArray(rawActions) && rawActions.length ? rawActions : fallback
      return list.map(a => ({
        title: a.title || '处置建议',
        detail: a.detail || '请结合现场情况安排处置。',
        priority: a.priority || '中优先级',
        status: a.status || '待执行'
      }))
    },

    alignSummaryWithTimes(summary, times, selectedHour) {
      const safeSummary = { ...(summary || {}) }
      const list = Array.isArray(times) ? times : []
      if (!list.length) return safeSummary

      let peakCount = -1
      let peakHour = '0:00'
      let selectedCount = 0
      list.forEach(item => {
        const c = Number(item.count || 0)
        const t = String(item.time || '0:00')
        const h = Number(t.split(':')[0])
        if (c > peakCount) {
          peakCount = c
          peakHour = t
        }
        if (h === Number(selectedHour)) {
          selectedCount = c
        }
      })

      // 统一峰值口径：卡片指标、建议文案与趋势图一致
      safeSummary.peak_hour = peakHour
      safeSummary.peak_count = peakCount < 0 ? 0 : peakCount
      safeSummary.selected_hour_count = selectedCount
      safeSummary.total_predicted = safeSummary.peak_count

      const primary = safeSummary.primary_location || '当前区域'
      safeSummary.suggestion = `当前选择的 ${selectedHour}:00 时段，${primary} 预计违章 ${selectedCount} 起；全天峰值出现在 ${peakHour}（约 ${safeSummary.peak_count} 起），建议在峰值前 30 分钟前置巡逻警力。`

      return safeSummary
    },

    async loadRepeatOffenders(params) {
      try {
        const req = {
          location: params.location || '0',
          minCount: 2
        }
        const res = await getRepeatOffenders(req)
        const list = res.data || []
        if (list.length) {
          this.repeatOffenders = list
          return
        }

        // 若当前区域筛选无结果，则回退全局复犯车辆，避免页面空白
        if (req.location !== '0') {
          const fallbackRes = await getRepeatOffenders({
            location: '0',
            minCount: 2
          })
          this.repeatOffenders = fallbackRes.data || []
          return
        }

        this.repeatOffenders = []
      } catch (e) {
        console.error('加载复犯车辆预警失败', e)
        this.repeatOffenders = []
      }
    },

    getStatusTagType(status) {
      if (status === '已完成') return 'success'
      if (status === '执行中') return 'warning'
      return 'info'
    },

    updateActionStatus(index, status) {
      if (!this.actions[index]) return
      this.actions.splice(index, 1, {
        ...this.actions[index],
        status
      })
    },

    renderLocationChart(data) {
      if (!this.locationChart || this.locationChart.isDisposed()) return
      const el = this.$refs.locationChart
      if (!el || el.clientWidth === 0 || el.clientHeight === 0) return
      const list = Array.isArray(data) ? data : []
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: list.length ? list.map(i => i.name) : ['暂无数据'],
          axisLabel: {
            interval: 0,
            rotate: 15 // 稍微倾斜防止重叠
          }
        },
        yAxis: {
          type: 'value',
          splitLine: {
            lineStyle: { type: 'dashed' }
          }
        },
        series: [
          {
            name: '预测违章数',
            type: 'bar',
            barWidth: '40%',
            data: list.length ? list.map(i => i.count) : [0],
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#83bff6' },
                { offset: 0.5, color: '#188df0' },
                { offset: 1, color: '#188df0' }
              ]),
              borderRadius: [4, 4, 0, 0]
            },
            label: {
              show: true,
              position: 'top'
            }
          }
        ]
      }
      this.locationChart.setOption(option, { notMerge: true })
      this.locationChart.off('click')
      this.locationChart.on('click', params => {
        const name = params.name
        if (name && name !== '暂无数据') {
          // 先在本页做过滤统计
          this.handleLocationFilter(name)
          // 跳转到违章记录列表，查看对应区域的详细记录
          this.$router.push({
            path: '/violations',
            query: {
              district: name,
              fromIntelligent: '1',
            },
          })
        }
      })
    },

    renderTimeChart(data) {
      if (!this.timeChart || this.timeChart.isDisposed()) return
      const el = this.$refs.timeChart
      if (!el || el.clientWidth === 0 || el.clientHeight === 0) return
      const list = Array.isArray(data) ? data : []
      const option = {
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: list.length ? list.map(i => i.time) : ['0:00']
        },
        yAxis: {
          type: 'value',
          splitLine: {
            lineStyle: { type: 'dashed' }
          }
        },
        visualMap: {
          show: false,
          dimension: 0,
          pieces: [
            { lte: 6, color: '#67C23A' },
            { gt: 6, lte: 8, color: '#E6A23C' },
            { gt: 8, lte: 17, color: '#409EFF' },
            { gt: 17, lte: 20, color: '#E6A23C' },
            { gt: 20, color: '#67C23A' }
          ]
        },
        series: [
          {
            name: '违章趋势',
            type: 'line',
            smooth: true,
            data: list.length ? list.map(i => i.count) : [0],
            areaStyle: {
               color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(64,158,255,0.3)' },
                { offset: 1, color: 'rgba(64,158,255,0.01)' }
              ])
            },
            lineStyle: {
              width: 3,
              color: '#409EFF'
            },
            markPoint: {
              data: [
                { type: 'max', name: 'Max' },
                { type: 'min', name: 'Min' }
              ]
            },
            markLine: {
              symbol: 'none',
              lineStyle: {
                type: 'dashed',
                color: '#f56c6c'
              },
              data: [
                {
                  xAxis: `${this.selectedHour}:00`,
                  name: '当前查询'
                },
                ...(this.summary && this.summary.peak_hour ? [{ xAxis: this.summary.peak_hour, name: '峰值时段' }] : [])
              ]
            }
          }
        ]
      }
      this.timeChart.setOption(option, { notMerge: true })
    },

    formatWarningTime(row) {
      if (!row) return '-'
      const value = row.warningTime || row.lastViolationTime || row
      if (!value) return '-'
      try {
        return new Date(value).toLocaleString()
      } catch (e) {
        return value
      }
    },

    formatSeverity(row) {
      const s = (row.severity || '').toLowerCase()
      if (s === 'high') return '高'
      if (s === 'medium') return '中'
      if (s === 'low') return '低'
      return row.severity || '-'
    },

    formatRepeatRiskTag(level) {
      if (level === '高') return 'danger'
      if (level === '中') return 'warning'
      return 'success'
    },

    formatDistrict(location) {
      if (!location) return '-'
      const districts = [
        '郑东新区',
        '金水区',
        '二七区',
        '中原区',
        '管城回族区',
        '惠济区',
      ]

      // 道路关键字到行政区映射（可按需补充）
      const roadMap = {
        滨河大道: '惠济区',
        金水东路: '郑东新区',
        商务内环路: '郑东新区',
        花园路: '金水区',
        中州大道: '金水区',
        大学路: '二七区',
        京广路: '二七区',
        建设路: '中原区',
        嵩山路: '中原区',
        航海东路: '管城回族区',
        文化北路: '惠济区',
      }

      for (const [road, dist] of Object.entries(roadMap)) {
        if (location.includes(road)) return dist
      }

      for (const d of districts) {
        if (location.includes(d)) return d
      }

      if (location.includes('-')) {
        return location.split('-')[0]
      }

      return location
    },

    handleLocationFilter(name) {
      this.warningFilterLocation = name
    },

    clearWarningFilter() {
      this.warningFilterLocation = ''
    },

    exportWarnings() {
      const list = this.filteredLatestWarnings
      if (!list || !list.length) {
        this.$message.warning('暂无可导出的预警数据')
        return
      }
      try {
        const data = list.map(w => ({
          时间: this.formatWarningTime(w),
          地点: this.formatDistrict(w.location),
          违章类型: w.violationType,
          风险级别: this.formatSeverity(w),
          预计数量: w.count,
        }))
        const worksheet = XLSX.utils.json_to_sheet(data)
        const workbook = XLSX.utils.book_new()
        XLSX.utils.book_append_sheet(workbook, worksheet, '智能预警')
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
        a.download = '智能预警记录.xlsx'
        a.click()
        URL.revokeObjectURL(url)
        this.$message.success('导出成功')
      } catch (e) {
        console.error(e)
        this.$message.error('导出失败')
      }
    },

    async onGenerateWeatherWarnings() {
      try {
        // 尝试调用后端接口（如果已经部署了天气预警规则）
        await triggerWeatherWarnings()
        await this.loadLatestWarnings()
        this.$message.success('已根据后台天气规则生成预警')
      } catch (e) {
        // 如果后端暂时没有对应接口（404 等情况），就在前端本地随机标注天气因素，保证演示效果
        console.warn('触发后台天气预警失败，使用前端随机天气模式', e)
        this.applyRandomWeatherFactors()
        this.$message.success('已根据示例随机天气生成预警效果')
      }
    },

    applyRandomWeatherFactors() {
      const weathers = ['大雨', '暴雨', '大雾', '雨夹雪', '冰雪路面']
      const updated = (this.latestWarnings || []).map(w => {
        // 只给部分记录加上天气因素，避免全部变成天气预警
        if (Math.random() < 0.5) return w
        const clone = { ...w }
        clone.warningType = 'Weather'
        clone.weatherFactor = weathers[Math.floor(Math.random() * weathers.length)]
        return clone
      })
      this.latestWarnings = updated
    },
  }
}
</script>

<style scoped>
.intelligent-page {
  padding: 24px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}
.page-header h1 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}
.subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.control-panel {
  margin-bottom: 24px;
  border-radius: 8px;
}
.control-group {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.control-left {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
  align-items: center;
}
.control-right {
  display: flex;
  align-items: center;
}
.control-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label {
  font-weight: 500;
  color: #606266;
}

.metrics-row {
  margin-bottom: 24px;
}
.metric-card {
  min-height: 120px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  transition: all 0.3s;
}
.metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0,0,0,0.1);
}
.metric-card .el-card__body {
  width: 100%;
  display: flex;
  align-items: center;
  padding: 20px !important;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  background: #fef0f0;
  color: #f56c6c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-right: 16px;
  flex-shrink: 0;
}
.metric-info {
  flex-grow: 1;
}
.metric-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 4px;
}
.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}
.metric-sub-text {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}
.unit {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}

.suggestion-card .el-card__body {
  display: block !important;
}
.suggestion-header {
  font-size: 16px;
  font-weight: bold;
  color: #409EFF;
  margin-bottom: 12px;
}
.suggestion-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.prominent-suggestion {
  border: 2px solid rgba(245, 108, 108, 0.25);
  background: linear-gradient(135deg, #fff7f7 0%, #fff 48%, #f5f9ff 100%);
  position: relative;
  overflow: hidden;
}

.prominent-suggestion::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 6px;
  background: linear-gradient(180deg, #f56c6c, #e6a23c);
}

.prominent-suggestion .el-card__body {
  padding-left: 22px !important;
}

.suggestion-badge {
  display: inline-block;
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  background: linear-gradient(135deg, #f56c6c, #e6a23c);
  padding: 3px 10px;
  border-radius: 10px;
  margin-bottom: 8px;
}

.suggestion-title {
  font-size: 18px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 6px;
}

.prominent-suggestion .suggestion-content {
  margin-top: 6px;
  font-size: 16px;
  line-height: 1.8;
  color: #303133;
  font-weight: 500;
}

.suggestion-footer {
  margin-top: 10px;
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.footer-item {
  font-size: 12px;
  color: #606266;
  background: rgba(64, 158, 255, 0.08);
  border: 1px solid rgba(64, 158, 255, 0.2);
  border-radius: 12px;
  padding: 3px 10px;
}

.charts-row {
  margin-bottom: 24px;
}
.chart-card {
  border-radius: 8px;
}
.chart-header {
  font-weight: bold;
  font-size: 16px;
}
.chart-container {
  height: 350px;
  width: 100%;
  min-width: 300px;
}

.detail-row {
  margin-top: 8px;
}

.detail-card {
  border-radius: 8px;
}

.reason-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reason-item {
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}

.reason-item:last-child {
  border-bottom: none;
}

.reason-title {
  display: flex;
  align-items: center;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.reason-title .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #409eff;
  margin-right: 8px;
}

.reason-desc {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.reason-empty {
  text-align: center;
  color: #c0c4cc;
  padding: 32px 0;
  font-size: 13px;
}

.action-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.action-detail {
  margin-top: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.action-ops {
  margin-top: 8px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.detail-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>


