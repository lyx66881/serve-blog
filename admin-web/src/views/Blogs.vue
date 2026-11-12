<template>
  <div class="page-enter">
    <div class="page-header">
      <div>
        <h2 class="page-title">博客管理</h2>
        <p class="page-desc">全站内容审核 · 状态流转与删除</p>
      </div>
    </div>

    <div class="panel">
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
        <span class="toolbar-hint">共 {{ total }} 篇</span>
      </div>

      <!-- 作者筛选提示 -->
      <div v-if="query.authorId" class="filter-banner">
        <el-icon><User /></el-icon>
        <span>正在按作者筛选：{{ authorName || `用户 #${query.authorId}` }}</span>
        <el-button link type="primary" @click="clearAuthor">清除筛选</el-button>
      </div>

      <!-- 列表 -->
      <el-table v-loading="loading" :data="items">
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
            <el-tag v-for="t in row.tags || []" :key="t.id" size="small" effect="plain" round class="tag-item">{{ t.name }}</el-tag>
            <span v-if="!(row.tags && row.tags.length)" class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="light" round>{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="viewCount" label="浏览" width="80" />
        <el-table-column label="创建时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="196" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
            <el-dropdown trigger="click" @command="(cmd: string) => onStatusChange(row, cmd)">
              <el-button link type="primary">
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
            <el-button link type="danger" @click="onDelete(row)">删除</el-button>
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
        class="pagination-row"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" size="60%" :title="detail?.title || '博客详情'">
      <template v-if="detail">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="作者">{{ detail.author?.nickname || detail.author?.username }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusType(detail.status)" effect="light" round>{{ statusLabel(detail.status) }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="浏览量">{{ detail.viewCount ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="标签" :span="2">
            <el-tag v-for="t in detail.tags || []" :key="t.id" size="small" effect="plain" round class="tag-item">{{ t.name }}</el-tag>
            <span v-if="!(detail.tags && detail.tags.length)">-</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(detail.publishedAt) }}</el-descriptions-item>
        </el-descriptions>
        <h4 class="drawer-h4">摘要</h4>
        <p class="summary">{{ detail.summary || '-' }}</p>
        <h4 class="drawer-h4">正文</h4>
        <div v-if="detail.contentHtml" class="content-html" v-html="detail.contentHtml" />
        <el-empty v-else description="该博客未提供 HTML 正文（仅 TipTap JSON）" :image-size="60" />
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, ArrowDown, User } from '@element-plus/icons-vue'
import {
  apiAdminBlogs, apiAdminBlogDetail, apiAdminBlogStatus, apiAdminBlogDelete, apiPublicCategories
} from '../api/admin'
import type { BlogDetail, BlogSummary, CategoryItem } from '../types/api'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const items = ref<BlogSummary[]>([])
const total = ref(0)
const categories = ref<CategoryItem[]>([])
const detail = ref<BlogDetail | null>(null)
const detailVisible = ref(false)
const authorName = ref('')

const query = reactive({
  page: 1,
  pageSize: 10,
  keyword: '',
  status: '',
  categoryId: undefined as number | undefined,
  authorId: undefined as number | undefined
})

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
    if (query.authorId && page.items.length) {
      const author = page.items[0]?.author
      authorName.value = author ? (author.nickname || author.username) : ''
    } else if (!query.authorId) {
      authorName.value = ''
    }
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
  query.authorId = undefined
  router.replace({ query: {} })
  onSearch()
}

function clearAuthor() {
  query.authorId = undefined
  router.replace({ query: {} })
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

// 支持从用户管理页跳转：/blogs?authorId=xx
watch(() => route.query.authorId, val => {
  const id = val ? Number(val) : undefined
  query.authorId = id && !Number.isNaN(id) ? id : undefined
  query.page = 1
  load()
}, { immediate: false })

onMounted(() => {
  const id = route.query.authorId ? Number(route.query.authorId) : undefined
  if (id && !Number.isNaN(id)) {
    query.authorId = id
  }
  load()
  apiPublicCategories().then(cats => { categories.value = cats }).catch(() => {
    // 分类加载失败不影响列表
  })
})
</script>

<style scoped>
.tag-item {
  margin-right: 4px;
}
.summary {
  color: var(--ink-500);
  font-size: 13px;
  line-height: 1.7;
}
.drawer-h4 {
  margin: 18px 0 8px;
  color: var(--ink-900);
}
.content-html {
  font-size: 14px;
  line-height: 1.8;
  word-break: break-word;
  color: var(--ink-700);
}
.content-html :deep(img) {
  max-width: 100%;
}
</style>
