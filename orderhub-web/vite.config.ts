import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  // Settings for the tests (npm test). jsdom is a fake browser that runs inside Node.
  test: {
    environment: "jsdom",
    restoreMocks: true,
    setupFiles: ["./src/test/setup.ts"],
  },
  server: {
    port: 5173,
    // DEV PROXY
    // The browser only talks to http://localhost:5173. Any request whose path starts with /api
    // is forwarded to the Spring Boot backend on port 8080, with the "/api" prefix removed:
    //   browser  ->  GET http://localhost:5173/api/products
    //   Vite     ->  GET http://localhost:8080/products
    // Because the browser thinks everything is the same origin, you do NOT need to add CORS
    // configuration to the backend while developing.
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
    },
  },
});
