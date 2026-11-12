<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col v-for="card in cards" :key="card.label" :xs="12" :sm="6" style="margin-bottom: 16px">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ backgroundColor: card.color }">
            <el-icon :size="22"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-body">
            <div class="stat-value">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16">
      <el-col :xs="24" :sm="16">
        <el-card shadow="never">
          <template #header>近 14 天发布 / 注册趋势</template>
          <div ref="trendChartRef" class="trend-chart" />
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="never">
          <template #header>内容概览</template>
          <div class="overview-row"><span>已发布博客</span><el-tag type="success">{{ stats?.publishedBlogs ?? '-' }}</el-tag></div>
          <div class="overview-row"><span>草稿</span><el-tag type="info">{{ stats?.draftBlogs ?? '-' }}</el-tag></div>
          <div class="overview-row"><span>已下架</span><el-tag type="warning">{{ stats?.offlineBlogs ?? '-' }}</el-tag></div>
          <div class="overview-row"><span>分类 / 标签</span><span>{{ stats?.totalCategories ?? '-' }} / {{ stats?.totalTags ?? '-' }}</span></div>
          <div class="overview-row">
            <span>待处理反馈</span>
            <el-badge :value="stats?.pendingFeedbacks ?? 0" :type="stats?.pendingFeedbacks ? 'danger' : 'info'">
              <span style="padding-right: 24px">{{ stats?.totalFeedbacks ?? '-' }} 条反馈</span>
            </el-badge>
          </div>
          <el-button type="primary" plain class="go-feedback" @click="$router.push('/feedbacks')">
            前往处理反馈
          </el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, nextTick } from 'vue'
import * as echarts from 'echarts'
import { apiGetStats } from '../api/admin'
import type { AdminStats } from '../types/api'

const loading = ref(false)
const stats = ref<AdminStats | null>(null)
const trendChartRef = ref<HTMLDivElement>()
let chart: echarts.ECharts | null = null
let resizeHandler: (() => void) | null = null

const cards = computed(() => [
  { label: '用户总数', value: stats.value?.totalUsers ?? '-', icon: 'User', color: '#e8f1ff' },
  { label: '博客总数', value: stats.value?.totalBlogs ?? '-', icon: 'Document', color: '#e8fff0' },
  { label: '总浏览量', value: stats.value?.totalViews ?? '-', icon: 'View', color: '#fff7e8' },
  { label: '待处理反馈', value: stats.value?.pendingFeedbacks ?? '-', icon: 'ChatDotRound', color: '#ffecec' }
])

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
    legend: { data: ['发布博客', '新注册用户'] },
    grid: { left: 32, right: 16, top: 40, bottom: 28 },
    xAxis: { type: 'category', data: trend.map(p => p.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '发布博客', type: 'line', smooth: true, data: trend.map(p => p.blogsPublished), itemStyle: { color: '#1677ff' }, areaStyle: { opacity: 0.12 } },
      { name: '新注册用户', type: 'line', smooth: true, data: trend.map(p => p.newUsers), itemStyle: { color: '#52c41a' }, areaStyle: { opacity: 0.12 } }
    ]
  })
}

onMounted(async () => {
  loading.value = true
  try {
    stats.value = await apiGetStats()
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  if (resizeHandler) {
    window.removeEventListener('resize', resizeHandler)
  }
  chart?.dispose()
  chart = null
})
</script>

<style scoped>
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
}
.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #1677ff;
}
.stat-value {
  font-size: 24px;
  font-weight: 600;
  line-height: 1.2;
}
.stat-label {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
}
.trend-chart {
  width: 100%;
  height: 320px;
}
.overview-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #f0f0f0;
  font-size: 14px;
}
.go-feedback {
  width: 100%;
  margin-top: 16px;
}
</style>
