import { describe, expect, it } from 'vitest'
import { translate, translateResourceReason, translateResourceStatus } from './messages'

describe('translations', () => {
  it('translates interface labels while preserving technical fallback values', () => {
    expect(translate('ko', 'workloads.subtitle')).toContain('Deployment')
    expect(translateResourceStatus('ko', 'critical')).toBe('심각')
    expect(translateResourceReason('ko', 'Healthy')).toBe('정상')
    expect(translateResourceReason('ko', 'CrashLoopBackOff')).toBe('CrashLoopBackOff')
  })

  it('keeps English resource labels unchanged', () => {
    expect(translateResourceStatus('en', 'warning')).toBe('warning')
    expect(translateResourceReason('en', 'UnavailableReplicas')).toBe('UnavailableReplicas')
  })
})
