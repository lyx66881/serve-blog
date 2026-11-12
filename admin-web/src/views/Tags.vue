<template>
  <el-card shadow="never">
    <div class="toolbar">
      <span class="hint">标签由作者写文章时自动创建；重命名后所有引用处同步生效</span>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column label="标签名" min-width="160">
        <template #default="{ row }">
          <el-tag>{{ row.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="blogCount" label="关联博客数" width="120" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openRename(row)">重命名</el-button>
          <el-button size="small" type="danger" plain @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="renameVisible" title="重命名标签" width="420px">
    <el-input v-model="newName" maxlength="30" placeholder="≤30 字符，全局唯一" />
    <template #footer>
      <el-button @click="renameVisible = false">取消</el-button>
      <el-button type="primary" :loading="renaming" @click="onRename">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiAdminTags, apiAdminTagRename, apiAdminTagDelete } from '../api/admin'
import type { AdminTagItem } from '../types/api'

const loading = ref(false)
const items = ref<AdminTagItem[]>([])
const renameVisible = ref(false)
const renameTarget = ref<AdminTagItem | null>(null)
const newName = ref('')
const renaming = ref(false)

async function load() {
  loading.value = true
  try {
    items.value = await apiAdminTags()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openRename(row: AdminTagItem) {
  renameTarget.value = row
  newName.value = row.name
  renameVisible.value = true
}

async function onRename() {
  if (!renameTarget.value) return
  if (!newName.value.trim()) {
    ElMessage.warning('请输入标签名')
    return
  }
  renaming.value = true
  try {
    await apiAdminTagRename(renameTarget.value.id, newName.value.trim())
    ElMessage.success('已重命名')
    renameVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    renaming.value = false
  }
}

async function onDelete(row: AdminTagItem) {
  try {
    await ElMessageBox.confirm(
      `确定删除标签「${row.name}」吗？将同时移除 ${row.blogCount} 篇博客与该标签的关联。`,
      '删除标签', { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await apiAdminTagDelete(row.id)
    ElMessage.success('已删除')
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败')
  }
}

onMounted(load)
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #999;
}
</style>
