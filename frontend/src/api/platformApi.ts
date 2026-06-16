import { httpClient } from './httpClient'
import type { ApiHealthResponse } from '../types/platform'

export async function getApiHealth(): Promise<ApiHealthResponse> {
  const response = await httpClient.get<ApiHealthResponse>('/actuator/health')

  return response.data
}
