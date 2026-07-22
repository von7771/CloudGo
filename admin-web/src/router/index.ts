import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '数据概览' } },
        { path: 'trips', name: 'Trips', component: () => import('@/views/TripsView.vue'), meta: { title: '行程管理' } },
        { path: 'drivers', name: 'Drivers', component: () => import('@/views/DriversView.vue'), meta: { title: '司机管理' } },
        { path: 'passengers', name: 'Passengers', component: () => import('@/views/PassengersView.vue'), meta: { title: '乘客管理' } },
        { path: 'pricing', name: 'Pricing', component: () => import('@/views/PricingView.vue'), meta: { title: '计价规则' } },
        { path: 'driver-map', name: 'DriverMap', component: () => import('@/views/DriverMapView.vue'), meta: { title: '司机地图' } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (!to.meta.public && !user.isLoggedIn) {
    return '/login'
  }
  if (to.path === '/login' && user.isLoggedIn) {
    return '/dashboard'
  }
})

export default router
