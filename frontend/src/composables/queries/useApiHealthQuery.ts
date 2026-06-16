import { useQuery } from '@tanstack/vue-query'
import { getApiHealth } from '../../api/platformApi'

export function useApiHealthQuery() {
  return useQuery({
    queryKey: ['api-health'],
    queryFn: getApiHealth,
    retry: false,
    refetchInterval: 30_000,
  })
}
