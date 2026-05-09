<template>
  <div class="editor-page fade-in">
    <section class="glass-card editor-main">
      <div class="head">
        <h2>{{ isEdit ? '编辑攻略' : '发布攻略' }}</h2>
        <p>支持 Markdown 与富文本双模式编辑，正文两者至少填写一个即可。</p>
      </div>

      <el-form :model="form" label-position="top" class="editor-form">
        <el-form-item label="攻略标题">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="请输入标题" />
        </el-form-item>

        <el-row :gutter="12">
          <el-col :md="10" :sm="24">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="请选择游戏分区" style="width: 100%">
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :md="14" :sm="24">
            <el-form-item label="标签（逗号分隔）">
              <el-input v-model="form.tagText" placeholder="如: 动作, 新手, 速通" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="摘要">
          <el-input v-model="form.summary" type="textarea" :rows="2" maxlength="1200" show-word-limit />
        </el-form-item>

        <el-form-item label="封面图 URL">
          <div class="cover-row">
            <el-input v-model="form.coverImageUrl" placeholder="可直接粘贴图片URL，或通过上传生成" />
            <el-upload :auto-upload="false" :show-file-list="false" :on-change="uploadCover">
              <el-button>上传封面</el-button>
            </el-upload>
          </div>
        </el-form-item>

        <el-tabs v-model="activeTab">
          <el-tab-pane label="Markdown 编辑" name="markdown">
            <el-input v-model="form.contentMarkdown" type="textarea" :rows="16" placeholder="可填写 Markdown 内容（可留空）" />
          </el-tab-pane>

          <el-tab-pane label="富文本编辑" name="html">
            <div class="rich-editor" contenteditable="true" ref="richEditor" @input="syncHtml"></div>
          </el-tab-pane>

          <el-tab-pane label="Markdown 预览" name="preview">
            <article class="preview" v-html="renderedMarkdown"></article>
          </el-tab-pane>
        </el-tabs>

        <div class="tool-row">
          <el-upload :auto-upload="false" :show-file-list="false" :on-change="uploadContentImage">
            <el-button>上传正文图片</el-button>
          </el-upload>

          <el-input v-model="form.changeNote" placeholder="本次更新说明（可选）" style="max-width: 260px" />
        </div>

        <div class="action-row">
          <el-button :loading="loading" @click="submit('DRAFT')">保存草稿</el-button>
          <el-button type="primary" :loading="loading" @click="submit('PUBLISHED')">发布攻略</el-button>
        </div>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { marked } from 'marked'
import http from '../api/http'

const route = useRoute()
const router = useRouter()

const isEdit = computed(() => Boolean(route.params.id))
const loading = ref(false)
const activeTab = ref('markdown')
const richEditor = ref(null)
const categories = ref([])

const form = reactive({
  title: '',
  summary: '',
  categoryId: undefined,
  tagText: '',
  contentMarkdown: '',
  contentHtml: '',
  coverImageUrl: '',
  changeNote: ''
})

const renderedMarkdown = computed(() => marked.parse(form.contentMarkdown || ''))

const loadCategories = async () => {
  const res = await http.get('/categories')
  categories.value = res.data
}

const loadArticleForEdit = async () => {
  if (!isEdit.value) return

  const res = await http.get(`/articles/${route.params.id}/edit`)
  const data = res.data
  form.title = data.article.title
  form.summary = data.article.summary || ''
  form.categoryId = categories.value.find((c) => c.name === data.article.category)?.id
  form.tagText = (data.article.tags || []).join(', ')
  form.contentMarkdown = data.contentMarkdown
  form.contentHtml = data.contentHtml
  form.coverImageUrl = data.article.coverImageUrl || ''
  await nextTick()
  if (richEditor.value) {
    richEditor.value.innerHTML = form.contentHtml
  }
}

const syncHtml = () => {
  if (!richEditor.value) return
  form.contentHtml = richEditor.value.innerHTML || ''
}

const uploadCover = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await http.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    form.coverImageUrl = res.data.url
    ElMessage.success('封面上传成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const uploadContentImage = async (uploadFile) => {
  const file = uploadFile.raw
  if (!file) return

  const formData = new FormData()
  formData.append('file', file)

  try {
    const res = await http.post('/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    const url = res.data.url
    form.contentMarkdown += `\n![image](${url})\n`
    if (richEditor.value) {
      richEditor.value.innerHTML += `<p><img src="${url}" alt="image" /></p>`
      syncHtml()
    }
    ElMessage.success('图片已插入正文')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const submit = async (status) => {
  syncHtml()
  if (!form.title.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择游戏分区')
    return
  }
  if (!form.contentMarkdown.trim() && !form.contentHtml.trim()) {
    ElMessage.warning('Markdown 与富文本内容至少填写一个')
    return
  }

  loading.value = true
  const payload = {
    title: form.title,
    summary: form.summary,
    categoryId: form.categoryId,
    tags: form.tagText
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean),
    contentMarkdown: form.contentMarkdown,
    contentHtml: form.contentHtml,
    coverImageUrl: form.coverImageUrl,
    changeNote: form.changeNote,
    status
  }

  try {
    if (isEdit.value) {
      await http.put(`/articles/${route.params.id}`, payload)
      ElMessage.success(status === 'PUBLISHED' ? '攻略已更新并发布' : '草稿已保存')
    } else {
      await http.post('/articles', payload)
      ElMessage.success(status === 'PUBLISHED' ? '攻略发布成功' : '草稿保存成功')
    }
    router.push('/my-articles')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    await loadCategories()
    await loadArticleForEdit()
  } catch (error) {
    ElMessage.error(error.message)
    router.push('/my-articles')
  }
})
</script>

<style scoped>
.editor-main {
  padding: 20px;
}

.head h2 {
  margin: 0;
}

.head p {
  color: var(--text-secondary);
}

.cover-row {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
}

.rich-editor {
  min-height: 360px;
  border: 1px solid var(--card-border);
  border-radius: 10px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.65);
  outline: none;
}

.rich-editor:focus {
  border-color: var(--brand);
}

.preview {
  min-height: 320px;
  border: 1px solid var(--card-border);
  border-radius: 10px;
  padding: 14px;
  background: rgba(255, 255, 255, 0.65);
  line-height: 1.7;
}

.preview :deep(img) {
  max-width: 100%;
  border-radius: 8px;
}

.tool-row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
  margin: 10px 0 16px;
}

.action-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
