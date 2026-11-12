import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// 后端无 CORS 配置（见《前后端技术设计》与现状），开发期统一用代理，
// 前端只请求同源 /api，生产由网关或后端静态托管再转发。
export default defineConfig({
  plugins: [
    vue(),
    // 模板里的 <el-*> 组件按需引入，替代 main.ts 全量 app.use(ElementPlus)。
    Components({
      resolvers: [ElementPlusResolver()],
      dts: false
    })
  ],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})