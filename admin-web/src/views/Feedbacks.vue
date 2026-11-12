<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-radio-group v-model="query.status" @change="onSearch">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待处理</el-radio-button>
        <el-radio-button value="processed">已处理</el-radio-button>
      </el-radio-group>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="提交用户" width="170" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="user-cell">
            <el-avatar :size="28" :src="absoluteUrl(row.avatar)">
              {{ (row.nickname || row.username || '匿').slice(0, 1).toUpperCase() }}
            </el-avatar>
            <div>
              <div>{{ row.nickname || row.username || '匿名用户' }}</div>
              <div v-if="row.username" class="sub">@{{ row.username }}</div>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="联系方式" width="170" show-overflow-tooltip>
        <template #default="{ row }">{{ row.contact || '-' }}</template>
      </el-table-column>
      <el-table-column label="反馈内容" min-width="260">
        <template #default="{ row }">
          <el-tooltip :content="row.content" placement="top" :show-after="300">
            <span class="content-text">{{ row.content }}</span>
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'pending' ? 'warning' : 'success'">
            {{ row.status === 'pending' ? '待处理' : '已处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" width="165">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button
            size="small"
            :type="row.status === 'pending' ? 'success' : 'info'"
            plain
            @click="onToggleStatus(row)"
          >
            {{ row.status === 'pending' ? '标记已处理' : '标记待处理' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      class="pagination"
      @size-change="load"
      @current-change="load"
    />
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiAdminFeedbacks, apiAdminFeedbackStatus, apiAdminFeedbackDelete } from '../api/admin'
import type { FeedbackItem } from '../types/api'

const loading = ref(false)
const items = ref<FeedbackItem[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, status: '' })

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function absoluteUrl(url?: string) {
  if (!url || url.startsWith('http')) return url
  return `${location.origin}${url}`
}

async function load() {
  loading.value = true
  try {
    const page = await apiAdminFeedbacks({ ...query })
    items.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

async function onToggleStatus(row: FeedbackItem) {
  const toProcessed = row.status === 'pending'
  try {
    await apiAdminFeedbackStatus(row.id, toProcessed ? 'processed' : 'pending')
    ElMessage.success(toProcessed ? '已标记为已处理' : '已标记为待处理')
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function onDelete(row: FeedbackItem) {
  try {
    await ElMessageBox.confirm('确定删除这条反馈吗？', '删除反馈', { type: 'warning' })
  } catch {
    return
  }
  try {
    await apiAdminFeedbackDelete(row.id)
    ElMessage.success('已删除')
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  margin-bottom: 14px;
}
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
.sub {
  font-size: 12px;
  color: #999;
}
.content-text {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
