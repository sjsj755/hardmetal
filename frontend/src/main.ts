import { createApp } from 'vue'
import { createPinia } from 'pinia'
import { ElLoading } from 'element-plus'
import 'element-plus/es/components/loading/style/css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import App from './App.vue'
import router from './router'
import './styles/index.css'

const app = createApp(App)

// 顺序：Pinia → Router。路由守卫里会用到 auth store，
// 因此 Pinia 必须先于首次导航安装。
// Element Plus 组件由 unplugin-vue-components 在模板编译时按需引入；
// v-loading 指令与消息类服务不经过模板解析，需要在这里显式安装/引样式。
app.use(createPinia())
app.use(router)
app.use(ElLoading)

app.mount('#app')