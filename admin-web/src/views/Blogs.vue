<template>
  <el-card shadow="never">
    <!-- 筛选区 -->
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索标题 / 作者" clearable style="width: 220px" @keyup.enter="onSearch" @clear="onSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="onSearch">
        <el-option label="草稿" value="draft" />
        <el-option label="已发布" value="published" />
        <el-option label="已下架" value="offline" />
      </el-select>
      <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 150px" @change="onSearch">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" plain @click="onSearch">查询</el-button>
      <el-button @click="onReset">重置</el-button>
    </div>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="标题" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          <el-link type="primary" :underline="false" @click="openDetail(row)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column label="作者" width="140" show-overflow-tooltip>
        <template #default="{ row }">{{ row.author?.nickname || row.author?.username || '-' }}</template>
      </el-table-column>
      <el-table-column label="分类" width="110">
        <template #default="{ row }">{{ row.categoryName || '-' }}</template>
      </el-table-column>
      <el-table-column label="标签" min-width="140">
        <template #default="{ row }">
          <el-tag v-for="t in row.tags || []" :key="t.id" size="small" class="tag-item">{{ t.name }}</el-tag>
          <span v-if="!(row.tags && row.tags.length)">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column label="创建时间" width="165">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-dropdown trigger="click" @command="(cmd: string) => onStatusChange(row, cmd)">
            <el-button size="small" type="primary" plain>
              状态<el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="published" :disabled="row.status === 'published'">上架（发布）</el-dropdown-item>
                <el-dropdown-item command="offline" :disabled="row.status === 'offline'">下架</el-dropdown-item>
                <el-dropdown-item command="draft" :disabled="row.status === 'draft'">转为草稿</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button size="small" type="primary" plain @click="openDetail(row)">详情</el-button>
          <el-button size="small" type="danger" plain @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
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

  <!-- 详情抽屉 -->
  <el-drawer v-model="detailVisible" size="60%" :title="detail?.title || '博客详情'">
    <template v-if="detail">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="作者">{{ detail.author?.nickname || detail.author?.username }}</el-descriptions-item>
        <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status)">{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="浏览量">{{ detail.viewCount ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="标签" :span="2">
          <el-tag v-for="t in detail.tags || []" :key="t.id" size="small" class="tag-item">{{ t.name }}</el-tag>
          <span v-if="!(detail.tags && detail.tags.length)">-</span>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="发布时间">{{ formatTime(detail.publishedAt) }}</el-descriptions-item>
      </el-descriptions>
      <h4>摘要</h4>
      <p class="summary">{{ detail.summary || '-' }}</p>
      <h4>正文</h4>
      <div v-if="detail.contentHtml" class="content-html" v-html="detail.contentHtml" />
      <el-empty v-else description="该博客未提供 HTML 正文（仅 TipTap JSON）" :image-size="60" />
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import {
  apiAdminBlogs, apiAdminBlogDetail, apiAdminBlogStatus, apiAdminBlogDelete, apiPublicCategories
} from '../api/admin'
import type { BlogDetail, BlogSummary, CategoryItem } from '../types/api'

const loading = ref(false)
const items = ref<BlogSummary[]>([])
const total = ref(0)
const categories = ref<CategoryItem[]>([])
const detail = ref<BlogDetail | null>(null)
const detailVisible = ref(false)

const query = reactive({ page: 1, pageSize: 10, keyword: '', status: '', categoryId: undefined as number | undefined })

function statusLabel(status: string) {
  return status === 'published' ? '已发布' : status === 'offline' ? '已下架' : '草稿'
}
function statusType(status: string) {
  return status === 'published' ? 'success' : status === 'offline' ? 'warning' : 'info'
}
function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

async function load() {
  loading.value = true
  try {
    const page = await apiAdminBlogs({ ...query })
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

function onReset() {
  query.keyword = ''
  query.status = ''
  query.categoryId = undefined
  onSearch()
}

async function openDetail(row: BlogSummary) {
  try {
    detail.value = await apiAdminBlogDetail(row.id)
    detailVisible.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载详情失败')
  }
}

async function onStatusChange(row: BlogSummary, status: string) {
  const label = statusLabel(status)
  try {
    await ElMessageBox.confirm(`确定将「${row.title}」设为 ${label} 吗？`, '修改状态', { type: 'warning' })
  } catch {
    return
  }
  try {
    await apiAdminBlogStatus(row.id, status)
    ElMessage.success('状态已更新')
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function onDelete(row: BlogSummary) {
  try {
    await ElMessageBox.confirm(`确定删除博客「${row.title}」吗？删除后不可恢复。`, '删除博客', { type: 'error' })
  } catch {
    return
  }
  try {
    await apiAdminBlogDelete(row.id)
    ElMessage.success('已删除')
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(async () => {
  load()
  try {
    categories.value = await apiPublicCategories()
  } catch {
    // 分类加载失败不影响列表
  }
})
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}
.tag-item {
  margin-right: 4px;
}
.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
.summary {
  color: #666;
  font-size: 13px;
  line-height: 1.7;
}
.content-html {
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
}
.content-html :deep(img) {
  max-width: 100%;
}
</style>
