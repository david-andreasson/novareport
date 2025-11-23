import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: './src/test/setupTests.ts',
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html', 'lcov'],
      lines: 0.8,
      statements: 0.8,
      functions: 0.8,
      branches: 0.8,
    },
  },
})
