import { httpClient } from './httpClient'
import type { IncidentAnalysisReport, IncidentAnalysisRequest } from '../types/aiAnalysis'

export async function analyzeIncident(
  clusterId: string,
  request: IncidentAnalysisRequest,
): Promise<IncidentAnalysisReport> {
  const response = await httpClient.post<IncidentAnalysisReport>(
    `/api/clusters/${clusterId}/analysis/incidents`,
    request,
  )

  return response.data
}

export async function getIncidentAnalysis(
  clusterId: string,
  analysisId: string,
): Promise<IncidentAnalysisReport> {
  const response = await httpClient.get<IncidentAnalysisReport>(
    `/api/clusters/${clusterId}/analysis/incidents/${analysisId}`,
  )

  return response.data
}
