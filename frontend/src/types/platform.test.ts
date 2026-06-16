import { describe, expect, it } from 'vitest'
import { toPlatformAvailability } from './platform'

describe('toPlatformAvailability', () => {
  it('maps the actuator UP status to available', () => {
    expect(toPlatformAvailability('UP')).toBe('available')
  })

  it('uses unknown for missing or unexpected statuses', () => {
    expect(toPlatformAvailability()).toBe('unknown')
    expect(toPlatformAvailability('OUT_OF_SERVICE')).toBe('unknown')
  })
})
