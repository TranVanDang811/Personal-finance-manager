import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalized = id.replace(/\\/g, '/');
          if (!normalized.includes('/node_modules/')) {
            return undefined;
          }
          if (normalized.includes('/recharts/') || normalized.includes('/d3-') || normalized.includes('/victory-vendor/')) {
            return 'vendor-charts';
          }
          if (normalized.includes('/@heroicons/')) {
            return 'vendor-icons';
          }
          if (
            normalized.includes('/react/') ||
            normalized.includes('/react-dom/') ||
            normalized.includes('/scheduler/') ||
            normalized.includes('/react-router') ||
            normalized.includes('/@reduxjs/') ||
            normalized.includes('/react-redux/') ||
            normalized.includes('/react-hook-form/') ||
            normalized.includes('/use-sync-external-store/')
          ) {
            return 'vendor-react';
          }
          if (normalized.includes('/axios/') || normalized.includes('/date-fns/')) {
            return 'vendor-utils';
          }
          return undefined;
        },
      },
    },
  },
});

