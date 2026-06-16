import type { AxiosAdapter, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { resolveDemoResponse } from './demoResponses'

export const isDemoMode = import.meta.env.VITE_DEMO_MODE === 'true'

export const demoHttpAdapter: AxiosAdapter = async (config: InternalAxiosRequestConfig) => {
  const data = resolveDemoResponse(config)
  const response: AxiosResponse = {
    data,
    status: 200,
    statusText: 'OK',
    headers: {},
    config,
    request: {},
  }

  return response
}
