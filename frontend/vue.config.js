const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  configureWebpack: {
    resolve: {
      alias: {
        '@': require('path').resolve(__dirname, 'src'),  // 配置 @ 为 src 目录
      },
    },
  },
  devServer: {
    proxy: {
      '/api': {
        target: 'http://localhost:8081', // 与后端 application.properties 中的 server.port 保持一致
        changeOrigin: true,
        secure: false,
        pathRewrite: { '^/api': '/api' },
      },
    },
    client: {
      overlay: {
        errors: true,
        warnings: false,
        runtimeErrors: (error) => {
          // 忽略 ResizeObserver 错误（Element Plus 已知问题，不影响功能）
          const errorMessage = error.message || error.toString() || '';
          if (errorMessage.includes('ResizeObserver loop completed with undelivered notifications')) {
            return false;
          }
          // 显示其他运行时错误
          return true;
        },
      },
    },
  },
});
