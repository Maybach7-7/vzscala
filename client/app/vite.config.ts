import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    port: 80
  },
  define: {
    'process.env.WS_URL': JSON.stringify(process.env.WS_URL || 'ws://localhost:8080/ws')
  }
})