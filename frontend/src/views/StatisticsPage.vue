<template>
  <div class="statistics-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>统计分析</h1>
      <p>违章数据总体情况与风险预警</p>
    </div>

    <!-- 总览卡片 -->
    <el-row :gutter="20" class="overview-row">
      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-icon total">
            <i class="el-icon-document"></i>
          </div>
          <div class="stat-content">
            <div class="stat-title">违章总数</div>
            <div class="stat-value ">{{ overview.totalViolations }}</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-icon money">
            <i class="el-icon-money"></i>
          </div>
          <div class="stat-content">
            <div class="stat-title">罚款总额（元）</div>
            <div class="stat-value">{{ overview.totalFines }}</div>
            <div class="stat-sub">平均罚款：{{ avgFine }} 元</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="stat-card">
          <div class="stat-icon points">
            <i class="el-icon-warning"></i>
          </div>
          <div class="stat-content">
            <div class="stat-title">扣分合计</div>
            <div class="stat-value">{{ overview.totalPoints }}</div>
            <div class="stat-sub">平均扣分：{{ avgPoints }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 预警系统 -->
    <el-card class="warning-card">
      <div class="warning-header">
        <h2>⚠️ 违章预警分析</h2>
        <span class="warning-desc">
          基于违章数量进行风险等级划分（系统自动判定）
        </span>
      </div>

      <el-row :gutter="20" class="warning-row">
        <el-col :span="8">
          <div class="warning-box red">
            <div class="warning-level">红色预警</div>
            <div class="warning-value">{{ warning.red }}</div>
            <div class="warning-tip">高风险</div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="warning-box orange">
            <div class="warning-level">橙色预警</div>
            <div class="warning-value">{{ warning.orange }}</div>
            <div class="warning-tip">中风险</div>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="warning-box blue">
            <div class="warning-level">蓝色预警</div>
            <div class="warning-value">{{ warning.blue }}</div>
            <div class="warning-tip">低风险</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="charts-row">
        <!-- 违章类型占比 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <div class="chart-header">
              <h2>📊 违章类型占比分析</h2>
              <span class="chart-desc">基于当前违章数据的结构分布</span>
            </div>
            <div ref="typeChart" class="chart-container"></div>
          </el-card>
        </el-col>

        <!-- 月度趋势 -->
        <el-col :span="12">
          <el-card class="chart-card">
            <div class="chart-header">
              <h2>📈 月度违章趋势分析</h2>
              <span class="chart-desc">观察近几个月违章变化情况</span>
            </div>
            <div ref="monthChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="bottom-row">
        <el-col :span="12">
          <div class="risk-summary">
            当前系统违章总数为
            <strong>{{ overview.totalViolations }}</strong>，
            综合风险等级判定为：
            <strong :class="riskClass">{{ riskText }}</strong>
            若连续 3 个月保持上升趋势，建议提高巡查频次并启动专项整治行动。
          </div>
        </el-col>
        <el-col :span="12">
          <div class="top-plates">
            <div class="top-plates-title">🚗 高频违章车牌 Top5</div>
            <div v-if="topPlates.length" class="top-plates-list">
              <div
                v-for="(item, index) in topPlates"
                :key="item.plateNumber"
                class="top-plates-item"
              >
                <span class="rank">NO.{{ index + 1 }}</span>
                <span class="plate">{{ item.plateNumber }}</span>
                <span class="count">{{ item.count }} 起</span>
              </div>
            </div>
            <div v-else class="top-plates-empty">
              当前暂无足够数据统计高频违章车牌。
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
  import * as echarts from 'echarts/core'
  import { PieChart, LineChart } from 'echarts/charts'
  import {
    TitleComponent,
    TooltipComponent,
    GridComponent,
    LegendComponent,
    AxisPointerComponent
  } from 'echarts/components'
  import { CanvasRenderer } from 'echarts/renderers'

  echarts.use([
    PieChart,
    LineChart,
    TitleComponent,
    TooltipComponent,
    GridComponent,
    LegendComponent,
    AxisPointerComponent,
    CanvasRenderer
  ])

  import {
    getOverview,
    getTypeStats,
    getMonthStats,
    getTopPlates
  } from '@/api/statistics'
  
  export default {
    name: 'StatisticsPage',
  
    data() {
      return {
        overview: {
          totalViolations: 0,
          totalFines: 0,
          totalPoints: 0
        },
        warning: {
          red: 0,
          orange: 0,
          blue: 0
        },
        monthDataMap: {},
        typeChart: null,   // 违章类型占比图
        monthChart: null,  // 月度趋势图
        topPlates: []      // 高频违章车牌
      }
    },
  
    mounted() {
      this.$nextTick(() => {
        // ① 先初始化图表
        this.initTypeChart()
        this.initMonthChart()
    
        // ② 再加载数据
        this.loadOverview()
        this.loadTypeStats()
        this.loadMonthStats()
        this.loadTopPlates()
      })
    },
  
    methods: {
      /* ===============================
         1️⃣ 加载统计总览数据
      =============================== */
      async loadOverview() {
        const res = await getOverview()
        const d = res.data
  
        // 总览数据
        this.overview = d
  
        // 互斥预警规则
        this.warning.red = d.totalViolations >= 1500 ? 1 : 0
        this.warning.orange =
          d.totalViolations >= 800 && d.totalViolations < 1500 ? 1 : 0
        this.warning.blue = d.totalViolations < 800 ? 1 : 0
  
      },
  
      /* ===============================
         2️⃣ 初始化违章类型饼图
      =============================== */
      initTypeChart() {
        this.typeChart = echarts.init(this.$refs.typeChart)
  
        this.typeChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} 起 ({d}%)'
          },
          legend: {
            bottom: 0
          },
          series: [
            {
              name: '违章类型',
              type: 'pie',
              radius: ['40%', '65%'],
              avoidLabelOverlap: false,
              label: {
                formatter: '{b}\n{d}%'
              },
              data: []
            }
          ]
        })
  
        // ✅ 关键：点击跳转到违章列表页
        this.typeChart.on('click', params => {
          this.$router.push({
            path: '/violations',
            query: {
              type: params.name
            }
          })
        })
      },
  
      /* ===============================
         3️⃣ 更新饼图数据
      =============================== */
      updateTypeChartFromStats(typeCountMap) {
        if (!this.typeChart) return

        const data = Object.entries(typeCountMap || {}).map(([name, value]) => ({
          name,
          value,
        }))

        this.typeChart.setOption({
          series: [{ data }]
        })
      },

      /* ===============================
         4️⃣ 初始化月度趋势图
      =============================== */
      initMonthChart() {
        this.monthChart = echarts.init(this.$refs.monthChart)
        this.monthChart.setOption({
          tooltip: {
            trigger: 'item',
            triggerOn: 'mousemove|click',
            confine: true,
            backgroundColor: 'rgba(50, 50, 50, 0.88)',
            borderWidth: 0,
            textStyle: {
              color: '#fff',
              fontSize: 12,
            },
            formatter: param => {
              if (!param) return ''
              const month = param.name || '-'
              const value = param.value ?? 0
              const ratio = this.getMonthRatioText(month, Number(value))
              return `${month}<br/>违章数量：${value} 起<br/>环比上月：${ratio}`
            },
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '10%',
            containLabel: true,
          },
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: [],
          },
          yAxis: {
            type: 'value',
            splitLine: {
              lineStyle: { type: 'dashed' },
            },
          },
          series: [
            {
              name: '月度违章数',
              type: 'line',
              smooth: true,
              data: [],
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(91,143,249,0.35)' },
                  { offset: 1, color: 'rgba(91,143,249,0.03)' },
                ]),
              },
              lineStyle: {
                width: 3,
                color: '#5b8ff9',
              },
              showSymbol: true,
              symbolSize: 8,
              emphasis: {
                focus: 'series',
                scale: true,
              },
            },
          ],
        })
      },

      updateMonthChart(monthlyCountMap) {
        if (!this.monthChart) return
        const entries = Object.entries(monthlyCountMap || {}).sort(([a], [b]) =>
          a.localeCompare(b),
        )
        const xData = entries.map(([month]) => month)
        const yData = entries.map(([, value]) => value)

        this.monthChart.setOption({
          xAxis: { data: xData },
          series: [{ data: yData }],
        })

        // 保存当前月度数据用于 tooltip 环比计算
        this.monthDataMap = entries.reduce((acc, [month, value]) => {
          acc[month] = Number(value || 0)
          return acc
        }, {})
      },

      getMonthRatioText(month, current) {
        const months = Object.keys(this.monthDataMap || {}).sort((a, b) => a.localeCompare(b))
        const idx = months.indexOf(month)
        if (idx <= 0) return '无上月数据'
        const prevMonth = months[idx - 1]
        const prev = Number(this.monthDataMap[prevMonth] || 0)
        if (prev === 0) return '无可比基数'
        const delta = ((current - prev) / prev) * 100
        const sign = delta >= 0 ? '+' : ''
        return `${sign}${delta.toFixed(1)}%`
      },

      /* ===============================
         5️⃣ 额外数据：类型分布 / 月度趋势 / 高频车牌
      =============================== */
      async loadTypeStats() {
        try {
          const res = await getTypeStats()
          const d = res.data || {}
          this.updateTypeChartFromStats(d.typeCount || {})
        } catch (e) {
          console.error('获取类型统计失败', e)
        }
      },

      async loadMonthStats() {
        try {
          const res = await getMonthStats()
          const d = res.data || {}
          this.updateMonthChart(d.monthlyCount || {})
          if (this.monthChart) {
            this.monthChart.off('click')
            this.monthChart.on('click', params => {
              const month = params?.name
              if (!month) return
              this.$router.push({
                path: '/violations',
                query: {
                  month,
                },
              })
            })
          }
        } catch (e) {
          console.error('获取月度统计失败', e)
        }
      },

      async loadTopPlates() {
        try {
          const res = await getTopPlates(5)
          this.topPlates = res.data || []
        } catch (e) {
          console.error('获取高频车牌失败', e)
        }
      }
    }
  }
  </script>
  

<style scoped>
.statistics-page {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
}

.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
}

.page-header p {
  margin-top: 6px;
  color: #888;
}

.stat-card {
  display: flex;
  align-items: center;
}

.stat-content {
  display: flex;
  flex-direction: column;
}

.stat-title {
  color: #888;
  font-size: 14px;
}

.stat-value {
  font-size: 26px;
  font-weight: bold;
}

.stat-sub {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  color: #fff;
  font-size: 22px;
}

.total {
  background: linear-gradient(135deg, #5b8ff9, #5ad8a6);
}
.money {
  background: linear-gradient(135deg, #f6bd16, #f08c00);
}
.points {
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
}

.warning-card {
  margin-top: 24px;
}

.warning-desc {
  color: #888;
  font-size: 13px;
}

.warning-box {
  padding: 20px;
  border-radius: 12px;
  text-align: center;
  color: #fff;
}

.warning-value {
  font-size: 32px;
  font-weight: bold;
  margin: 10px 0;
}

.red {
  background: linear-gradient(135deg, #ff4d4f, #ff7875);
}
.orange {
  background: linear-gradient(135deg, #fa8c16, #ffc069);
}
.blue {
  background: linear-gradient(135deg, #1890ff, #69c0ff);
}

.risk-summary {
  margin-top: 20px;
  font-size: 14px;
}

.high {
  color: #ff4d4f;
}
.mid {
  color: #fa8c16;
}
.low {
  color: #1890ff;
}
.chart-card {
  margin-top: 24px;
}

.chart-header {
  margin-bottom: 16px;
}

.chart-header h2 {
  margin: 0;
}

.chart-desc {
  font-size: 13px;
  color: #888;
}

.chart-container {
  width: 100%;
  height: 360px;
}

.warning-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 12px;
}

.charts-row {
  margin-top: 8px;
}

.bottom-row {
  margin-top: 16px;
}

.top-plates {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 16px 20px;
}

.top-plates-title {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
}

.top-plates-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.top-plates-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  padding: 6px 0;
  border-bottom: 1px dashed #e4e7ed;
}

.top-plates-item:last-child {
  border-bottom: none;
}

.top-plates-item .rank {
  color: #909399;
  width: 52px;
}

.top-plates-item .plate {
  font-weight: 600;
}

.top-plates-item .count {
  color: #606266;
}

.top-plates-empty {
  font-size: 13px;
  color: #909399;
}

</style>
