import type { ResourceStatus } from './kubernetes'

export interface IncidentAnalysisRequest {
  namespace: string
  targetKind: string
  targetName: string
  timeRangeMinutes: number
}

export interface IncidentEvidence {
  id: string
  type: string
  title: string
  message: string
  status: ResourceStatus
  timestamp?: string
}

export interface RootCauseCandidate {
  title: string
  confidence: number
  evidenceIds: string[]
}

export interface RecommendedAction {
  action: string
  risk: 'low' | 'medium' | 'high' | string
  reason: string
}

export interface AiTokenUsage {
  inputTokens: number
  cachedInputTokens: number
  outputTokens: number
  reasoningOutputTokens: number
  totalTokens: number
}

export interface IncidentAnalysisReport {
  analysisId: string
  clusterId: string
  namespace?: string
  targetKind: string
  targetName: string
  severity: ResourceStatus
  summary: string
  impactScope: string[]
  rootCauseCandidates: RootCauseCandidate[]
  evidence: IncidentEvidence[]
  recommendations: RecommendedAction[]
  nextChecks: string[]
  provider: string
  model?: string
  providerResponseId?: string
  tokenUsage: AiTokenUsage
  latencyMs: number
  createdAt: string
}
