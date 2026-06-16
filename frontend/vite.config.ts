import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), '')
  const apiProxyTarget = environment.VITE_API_PROXY_TARGET || 'http://localhost:8080'
  const publicBasePath = environment.VITE_PUBLIC_BASE_PATH || '/'

  return {
    base: publicBasePath,
    plugins: [vue()],
    server: {
      proxy: {
        '/actuator': apiProxyTarget,
        '/api': apiProxyTarget,
      },
    },
  }
})
