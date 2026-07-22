<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const activeMenu = computed(() => route.path)

const menus = [
  { path: '/dashboard', title: '数据概览', icon: 'DataAnalysis' },
  { path: '/trips', title: '行程管理', icon: 'List' },
  { path: '/drivers', title: '司机管理', icon: 'Van' },
  { path: '/passengers', title: '乘客管理', icon: 'User' },
  { path: '/driver-map', title: '司机地图', icon: 'Location' },
  { path: '/pricing', title: '计价规则', icon: 'Money' },
]

function logout() {
  user.logout()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">拼车管理后台</div>
      <el-menu
        :default-active="activeMenu"
        router
        class="menu"
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#fff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="page-title">{{ (route.meta.title as string) || '管理后台' }}</span>
        <el-button type="danger" plain @click="logout">退出登录</el-button>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.aside {
  background: #001529;
  color: #fff;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-weight: 700;
  font-size: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.menu {
  border-right: none;
  background: transparent;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #eee;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.main {
  padding: 20px;
}
</style>
