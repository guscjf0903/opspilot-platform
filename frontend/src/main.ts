import { VueQueryPlugin } from '@tanstack/vue-query'
import { createPinia } from 'pinia'
import { createApp } from 'vue'
import { router } from './router'
import './style.css'
import App from './App.vue'

createApp(App).use(createPinia()).use(VueQueryPlugin).use(router).mount('#app')
