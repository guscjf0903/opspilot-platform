export interface ApiHealthResponse {
  status?: string
}

export type PlatformAvailability = 'available' | 'unavailable' | 'unknown'

export function toPlatformAvailability(status?: string): PlatformAvailability {
  if (status === 'UP') {
    return 'available'
  }

  if (status === 'DOWN') {
    return 'unavailable'
  }

  return 'unknown'
}
