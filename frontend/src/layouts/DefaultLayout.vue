<script setup lang="ts">
import type { Component } from 'vue'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, House, SwitchButton, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { initials } from '../utils/format'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const navs: Array<{ to: string; label: string; icon: Component }> = [
  { to: '/dashboard', label: '工作台', icon: House },
  { to: '/customers', label: '客户管理', icon: User }
]

const pageTitle = computed(() =>
  typeof route.meta.title === 'string' ? route.meta.title : '工作台'
)
const avatarText = computed(() => initials(auth.user?.realName || auth.user?.username))
const roleName = computed(() => auth.user?.roleName ?? '')

async function handleCommand(command: string): Promise<void> {
  if (command !== 'logout') {
    return
  }
  try {
    await ElMessageBox.confirm('退出后需要重新登录，确定退出？', '退出登录', {
      type: 'warning',
      confirmButtonText: '退出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await auth.logout()
  await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">HM</div>
        <div>
          <p class="brand-name">硬质合金出口</p>
          <p class="brand-sub">CARBIDE EXPORT SUITE</p>
        </div>
      </div>

      <nav class="nav" aria-label="主导航">
        <router-link
          v-for="item in navs"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          active-class="is-active"
        >
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-note">
        当前已接入：认证、客户档案、联系人、时间轴、字典。<br />
        报价 / 订单 / 价格 / 合规等模块接口开发中。
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="topbar-left">
          <span class="crumb">硬质合金出口</span>
          <span class="crumb-sep">/</span>
          <strong class="topbar-title">{{ pageTitle }}</strong>
          <nav class="mobile-nav" aria-label="移动端导航">
            <router-link v-for="item in navs" :key="item.to" :to="item.to" class="mobile-link">
              {{ item.label }}
            </router-link>
          </nav>
        </div>

        <el-dropdown trigger="click" @command="handleCommand">
          <button class="user-chip" type="button">
            <span class="avatar">{{ avatarText }}</span>
            <span class="user-meta">
              <span class="user-name">{{ auth.displayName }}</span>
              <span class="user-role">{{ roleName }}</span>
            </span>
            <el-icon class="user-arrow"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </header>

      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: 232px minmax(0, 1fr);
  min-height: 100vh;
}

.sidebar {
  position: sticky;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 26px;
  height: 100vh;
  padding: 22px 16px;
  background: var(--surface);
  border-right: 1px solid var(--border);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, #eaf0ff, #dbe6ff);
  color: var(--primary);
  font-size: 12.5px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.brand-name {
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.3;
}

.brand-sub {
  color: var(--text-3);
  font-size: 9.5px;
  letter-spacing: 0.08em;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  color: var(--text-2);
  font-size: 13.5px;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.nav-link:hover {
  background: var(--surface-2);
  color: var(--text);
}

.nav-link.is-active {
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 600;
}

.sidebar-note {
  margin-top: auto;
  padding: 12px;
  border-radius: 10px;
  background: var(--surface-2);
  color: var(--text-3);
  font-size: 11.5px;
  line-height: 1.8;
}

.main {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 60px;
  padding: 0 28px;
  background: rgba(255, 255, 255, 0.88);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid var(--border);
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.crumb {
  color: var(--text-3);
  font-size: 12.5px;
}

.crumb-sep {
  color: var(--border-strong);
  font-size: 12px;
}

.topbar-title {
  color: var(--text);
  font-size: 13.5px;
  font-weight: 600;
}

.mobile-nav {
  display: none;
  gap: 14px;
  margin-left: 8px;
}

.mobile-link {
  color: var(--text-2);
  font-size: 13px;
}

.mobile-link.router-link-active {
  color: var(--primary);
  font-weight: 600;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 8px 6px 6px;
  border: 1px solid transparent;
  border-radius: 999px;
  background: transparent;
  cursor: pointer;
  font-family: inherit;
}

.user-chip:hover {
  background: var(--surface-2);
  border-color: var(--border);
}

.avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 12px;
  font-weight: 600;
}

.user-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
}

.user-name {
  color: var(--text);
  font-size: 12.5px;
  font-weight: 600;
}

.user-role {
  color: var(--text-3);
  font-size: 11px;
}

.user-arrow {
  color: var(--text-3);
  font-size: 12px;
}

.content {
  width: 100%;
  max-width: 1240px;
  margin: 0 auto;
  padding: 26px 28px 56px;
}

@media (max-width: 900px) {
  .app-shell {
    grid-template-columns: minmax(0, 1fr);
  }

  .sidebar {
    display: none;
  }

  .topbar {
    height: 56px;
    padding: 0 16px;
  }

  .crumb,
  .crumb-sep {
    display: none;
  }

  .mobile-nav {
    display: flex;
  }

  .user-meta {
    display: none;
  }

  .content {
    padding: 18px 16px 40px;
  }
}
</style>