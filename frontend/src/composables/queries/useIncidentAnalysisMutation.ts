import { useMutation } from '@tanstack/vue-query'
import { analyzeIncident } from '../../api/aiAnalysisApi'
import type { IncidentAnalysisRequest } from '../../types/aiAnalysis'

export function useIncidentAnalysisMutation(clusterId: () => string) {
  return useMutation({
    mutationFn: (request: IncidentAnalysisRequest) => analyzeIncident(clusterId(), request),
  })
}
