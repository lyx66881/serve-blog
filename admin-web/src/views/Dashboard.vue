<template>
  <div v-loading="loading" class="page-enter">
    <div class="page-header">
      <div>
        <h2 class="page-title">数据看板</h2>
        <p class="page-desc">站点运行概览 · 每 14 天趋势一览</p>
      </div>
      <div class="page-actions">
        <el-button @click="load" :loading="loading">
          <el-icon style="margin-right: 4px"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stat-grid">
      <div
        v-for="(card, i) in cards"
        :key="card.label"
        class="stat-card"
        :class="{ clickable: !!card.to }"
        :style="{ animationDelay: `${i * 60}ms` }"
        @click="card.to && router.push(card.to)"
      >
        <div class="stat-head">
          <span class="stat-label">{{ card.label }}</span>
          <span class="stat-chip" :style="{ color: card.color, backgroundColor: card.soft }">
            <el-icon :size="16"><component :is="card.icon" /></el-icon>
          </span>
        </div>
        <div class="stat-value">{{ card.value }}</div>
        <div class="stat-foot">{{ card.foot }}</div>
        <el-icon v-if="card.to" class="stat-arrow"><ArrowRight /></el-icon>
      </div>
    </div>

    <!-- 图表 + 概览 -->
    <el-row :gutter="16">
      <el-col :xs="24" :sm="16" style="margin-bottom: 16px">
        <div class="panel">
          <div class="panel-title">近 14 天发布 / 注册趋势</div>
          <div ref="trendChartRef" class="trend-chart" />
        </div>
      </el-col>
      <el-col :xs="24" :sm="8" style="margin-bottom: 16px">
        <div class="panel overview">
          <div class="panel-title">内容概览</div>
          <div class="overview-row"><span>已发布博客</span><el-tag type="success" effect="light" round>{{ stats?.publishedBlogs ?? '-' }}</el-tag></div>
          <div class="overview-row"><span>草稿</span><el-tag type="info" effect="light" round>{{ stats?.draftBlogs ?? '-' }}</el-tag></div>
          <div class="overview-row"><span>已下架</span><el-tag type="warning" effect="light" round>{{ stats?.offlineBlogs ?? '-' }}</el-tag></div>
          <div class="overview-row"><span>分类 / 标签</span><span class="overview-num">{{ stats?.totalCategories ?? '-' }} / {{ stats?.totalTags ?? '-' }}</span></div>
          <div class="overview-row">
            <span>待处理反馈</span>
            <span class="overview-num" :class="{ danger: stats?.pendingFeedbacks }">{{ stats?.pendingFeedbacks ?? 0 }} 条</span>
          </div>
          <el-button type="primary" plain round class="go-feedback" @click="$router.push('/feedbacks')">
            前往处理反馈
          </el-button>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { apiGetStats } from '../api/admin'
import type { AdminStats } from '../types/api'

const router = useRouter()
const loading = ref(false)
const stats = ref<AdminStats | null>(null)
const trendChartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const cards = computed(() => [
  { label: '用户总数', value: stats.value?.totalUsers ?? '-', icon: 'User', color: 'var(--primary)', soft: 'var(--primary-soft)', foot: '累计注册', to: '' },
  { label: '博客总数', value: stats.value?.totalBlogs ?? '-', icon: 'Document', color: 'var(--success)', soft: '#e6f7f1', foot: '含草稿与下架', to: '/blogs' },
  { label: '总浏览量', value: stats.value?.totalViews ?? '-', icon: 'View', color: 'var(--warning)', soft: '#fdf3e7', foot: '全部文章', to: '' },
  { label: '待处理反馈', value: stats.value?.pendingFeedbacks ?? '-', icon: 'ChatDotRound', color: 'var(--danger)', soft: '#fbe9ec', foot: '点击直达处理', to: '/feedbacks' }
])

async function load() {
  loading.value = true
  try {
    stats.value = await apiGetStats()
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!trendChartRef.value || !stats.value) return
  if (!chart) {
    chart = echarts.init(trendChartRef.value)
    resizeHandler = () => chart?.resize()
    window.addEventListener('resize', resizeHandler)
  }
  const trend = stats.value.trend || []
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['发布博客', '新注册用户'], top: 0, right: 0, itemWidth: 16, textStyle: { color: '#6b7280' } },
    grid: { left: 36, right: 16, top: 44, bottom: 28 },
    xAxis: { type: 'category', data: trend.map(p => p.date.slice(5)), axisLine: { lineStyle: { color: '#d9dce5' } }, axisLabel: { color: '#9aa1b2' } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { color: '#9aa1b2' }, splitLine: { lineStyle: { color: '#eceef3' } } },
    series: [
      { name: '发布博客', type: 'line', smooth: true, data: trend.map(p => p.blogsPublished), itemStyle: { color: '#3b5bdb' }, lineStyle: { width: 2.5 }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [ { offset: 0, color: 'rgba(59,91,219,0.18)' }, { offset: 1, color: 'rgba(59,91,219,0)' } ] } }, symbolSize: 6 },
      { name: '新注册用户', type: 'line', smooth: true, data: trend.map(p => p.newUsers), itemStyle: { color: '#0e9f6e' }, lineStyle: { width: 2.5 }, areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [ { offset: 0, color: 'rgba(14,159,110,0.15)' }, { offset: 1, color: 'rgba(14,159,110,0)' } ] } }, symbolSize: 6 }
    ]
  })
}

onMounted(load)

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
/* ---------- 统计卡片 ---------- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}
@media (max-width: 992px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
.stat-card {
  position: relative;
  background: var(--card);
  border-radius: var(--radius);
  box-shadow: var(--shadow-card);
  padding: 18px 20px 16px;
  animation: page-enter 0.4s cubic-bezier(0.22, 0.61, 0.36, 1) both;
  transition: transform 0.22s, box-shadow 0.22s;
}
.stat-card.clickable {
  cursor: pointer;
}
.stat-card.clickable:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-pop);
}
.stat-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.stat-label {
  font-size: 13px;
  color: var(--ink-500);
  letter-spacing: 1px;
}
.stat-chip {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.stat-value {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 700;
  color: var(--ink-900);
  line-height: 1.25;
  margin-top: 6px;
  font-variant-numeric: tabular-nums;
}
.stat-foot {
  font-size: 12px;
  color: var(--ink-400);
  margin-top: 2px;
}
.stat-arrow {
  position: absolute;
  right: 14px;
  bottom: 16px;
  color: var(--ink-200);
}

/* ---------- 面板 ---------- */
.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-900);
  margin-bottom: 12px;
  letter-spacing: 1px;
}
.trend-chart {
  width: 100%;
  height: 320px;
}

/* ---------- 内容概览 ---------- */
.overview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 11px 0;
  border-bottom: 1px dashed var(--ink-100);
  font-size: 14px;
  color: var(--ink-700);
}
.overview-num {
  font-variant-numeric: tabular-nums;
  color: var(--ink-900);
  font-weight: 600;
}
.overview-num.danger {
  color: var(--danger);
}
.go-feedback {
  width: 100%;
  margin-top: 18px;
}
</style>
