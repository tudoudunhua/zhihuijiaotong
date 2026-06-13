import { createApp } from 'vue';
import App from './App.vue';
import axios from 'axios';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import router from './router';

const app = createApp(App);

/* ================= Axios 全局配置（关键修改） ================= */
axios.defaults.baseURL = 'http://localhost:8081'; // ✅ 直连后端
axios.defaults.timeout = 5000;

// 如果本地已有 token，自动带上
const savedToken = localStorage.getItem('jwt_token');
if (savedToken) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${savedToken}`;
}

// 全局挂载 axios
app.config.globalProperties.$axios = axios;

/* ================= 使用插件 ================= */
app.use(ElementPlus);
app.use(router);

/* ================= 忽略已知的良性报错 ================= */
const originalError = console.error;
console.error = (...args) => {
  const errorMessage = args[0];
  const msg = typeof errorMessage === 'string' ? errorMessage : (errorMessage?.message || '');
  const ignorePatterns = [
    'ResizeObserver loop completed with undelivered notifications',
    '[ECharts] cartesian2d cannot be found for series'  // 图表仍正常显示，属 ECharts 时序问题
  ];
  if (ignorePatterns.some(p => msg.includes(p))) {
    return;
  }
  originalError.apply(console, args);
};

const handleError = (event) => {
  const errorMessage = event.message || event.reason?.message || '';
  if (errorMessage.includes('ResizeObserver loop completed with undelivered notifications')) {
    event.preventDefault();
    event.stopPropagation();
    return false;
  }
};

window.addEventListener('error', handleError, true);
window.addEventListener('unhandledrejection', handleError);

/* ================= 挂载应用 ================= */
app.mount('#app');
