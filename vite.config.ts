import { defineConfig } from "vite";
import path from "path";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
  resolve: {
    alias: [
      { find: "~", replacement: path.resolve(__dirname, "src") },
      {
        find: "@assets",
        replacement: path.resolve(__dirname, "./src/assets"),
      },
      {
        find: "@components",
        replacement: path.resolve(__dirname, "./src/components"),
      },
      {
        find: "@contexts",
        replacement: path.resolve(__dirname, "./src/contexts"),
      },
      {
        find: "@features",
        replacement: path.resolve(__dirname, "./src/features"),
      },
      {
        find: "@hooks",
        replacement: path.resolve(__dirname, "./src/hooks"),
      },
      {
        find: "@pages",
        replacement: path.resolve(__dirname, "./src/pages"),
      },
      {
        find: "@shared",
        replacement: path.resolve(__dirname, "./src/shared"),
      },
    ],
  },
  build: {
    manifest: true,
    assetsDir: "assets/js/ode-explorer/",
    rollupOptions: {
      output: {
        entryFileNames: `[name].js`,
        chunkFileNames: `[name].js`,
        assetFileNames: `[name].[ext]`,
      },
    },
  },
  plugins: [react()],
  server: {
    proxy: {
      // List of all applications
      "/applications-list": {
        target: "http://localhost:8090",
        changeOrigin: false,
      },
      // Public Conf
      "/conf/public": {
        target: "http://localhost:8090",
        changeOrigin: false,
      },
      "^/(?=assets|theme|locale|i18n|skin)": {
        target: "http://localhost:8090",
        changeOrigin: false,
      },
      // Entcore urls
      "^/(?=auth|cas|userbook|directory|communication|conversation|portal|session|timeline|workspace)":
        {
          target: "http://localhost:8090",
          changeOrigin: false,
        },
      // App urls
      "/blog": {
        target: "http://localhost:8090",
        changeOrigin: false,
      },
      "/explorer": {
        target: "http://localhost:8090",
        changeOrigin: false,
      },
    },
    host: "0.0.0.0",
    port: 3000,
  },
});
