import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The backend allows http://localhost:3000 and http://localhost:5173 by default (app.cors.allowed-origins).
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
});
