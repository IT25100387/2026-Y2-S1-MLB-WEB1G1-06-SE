import react from '@vitejs/plugin-react';
import {defineConfig} from 'vite';
const proxy={'/api':{target:'http://localhost:8080',changeOrigin:true},'/uploads':{target:'http://localhost:8080',changeOrigin:true}};
export default defineConfig({plugins:[react()],server:{proxy,watch:{usePolling:process.platform==='win32',interval:300}},preview:{proxy}});
