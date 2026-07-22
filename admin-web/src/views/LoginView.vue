<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/admin'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const user = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'admin123',
})

async function onSubmit() {
  loading.value = true
  try {
    const { data } = await login(form.username, form.password)
    user.setToken(data.data.token)
    ElMessage.success(data.message || '登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <el-card class="login-card" shadow="hover">
      <h2>拼车管理后台</h2>
      <p class="hint">默认账号 admin / admin123</p>
      <el-form :model="form" @submit.prevent="onSubmit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-button type="primary" native-type="submit" :loading="loading" style="width: 100%">
          登录
        </el-button>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1677ff 0%, #001529 100%);
}

.login-card {
  width: 400px;
}

.login-card h2 {
  margin: 0 0 8px;
  text-align: center;
}

.hint {
  text-align: center;
  color: #888;
  font-size: 13px;
  margin-bottom: 24px;
}
</style>
