import axios from 'axios'
import { demoHttpAdapter, isDemoMode } from '../demo/demoHttpAdapter'

export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 5_000,
  adapter: isDemoMode ? demoHttpAdapter : undefined,
})
