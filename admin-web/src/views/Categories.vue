<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建分类</el-button>
      <span class="hint">分类用于博客归类，删除后引用它的博客将不再显示分类名</span>
    </div>

    <el-table v-loading="loading" :data="items" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="分类名" min-width="160" />
      <el-table-column prop="description" label="描述" min-width="280" show-overflow-tooltip>
        <template #default="{ row }">{{ row.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" plain @click="onDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="editing ? '编辑分类' : '新建分类'" width="460px">
    <el-form label-width="80px">
      <el-form-item label="分类名" required>
        <el-input v-model="form.name" maxlength="50" placeholder="唯一，≤50 字符" />
      </el-form-item>
      <el-form-item label="描述">
        <el-input v-model="form.description" type="textarea" :rows="3" maxlength="200" placeholder="可选，≤200 字符" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiPublicCategories, apiAdminCategoryCreate, apiAdminCategoryUpdate, apiAdminCategoryDelete } from '../api/admin'
import type { CategoryItem } from '../types/api'

const loading = ref(false)
const items = ref<CategoryItem[]>([])
const dialogVisible = ref(false)
const editing = ref<CategoryItem | null>(null)
const saving = ref(false)
const form = reactive({ name: '', description: '' })

async function load() {
  loading.value = true
  try {
    items.value = await apiPublicCategories()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  form.name = ''
  form.description = ''
  dialogVisible.value = true
}

function openEdit(row: CategoryItem) {
  editing.value = row
  form.name = row.name
  form.description = row.description || ''
  dialogVisible.value = true
}

async function onSave() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      await apiAdminCategoryUpdate(editing.value.id, { name: form.name.trim(), description: form.description || undefined })
      ElMessage.success('分类已更新')
    } else {
      await apiAdminCategoryCreate({ name: form.name.trim(), description: form.description || undefined })
      ElMessage.success('分类已创建')
    }
    dialogVisible.value = false
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function onDelete(row: CategoryItem) {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除分类', { type: 'warning' })
  } catch {
    return
  }
  try {
    await apiAdminCategoryDelete(row.id)
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
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}
.hint {
  font-size: 12px;
  color: #999;
}
</style>
