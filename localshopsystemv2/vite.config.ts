/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// En desarrollo, /kiosco se reenvía al backend local: sin CORS y con la misma URL relativa que en producción.
const backend = process.env.BACKEND_URL ?? 'http://localhost:56784';

export default defineConfig({
  plugins: [react()],
  server: {
    port: Number(process.env.PORT ?? 80),
    proxy: {
      '/kiosco': { target: backend, changeOrigin: true },
    },
  },
  build: {
    outDir: 'build', // el Dockerfile copia /app/build
    sourcemap: false,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/setupTests.ts',
    css: false,
  },
});
