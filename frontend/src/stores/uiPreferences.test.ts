import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { useUiPreferencesStore } from './uiPreferences'

const storedPreferences = new Map<string, string>()
const documentElement = {
  dataset: {} as Record<string, string>,
  lang: '',
}

beforeEach(() => {
  storedPreferences.clear()
  documentElement.dataset = {}
  documentElement.lang = ''

  vi.stubGlobal('window', {
    localStorage: {
      getItem: (key: string) => storedPreferences.get(key) ?? null,
      setItem: (key: string, value: string) => storedPreferences.set(key, value),
    },
  })
  vi.stubGlobal('document', { documentElement })
  setActivePinia(createPinia())
})

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('ui preferences store', () => {
  it('starts in Korean dark mode and applies the document preferences', () => {
    const store = useUiPreferencesStore()

    expect(store.locale).toBe('ko')
    expect(store.theme).toBe('dark')
    expect(documentElement.lang).toBe('ko')
    expect(documentElement.dataset.theme).toBe('dark')
  })

  it('persists selected English light mode preferences', async () => {
    const store = useUiPreferencesStore()

    store.setLocale('en')
    store.setTheme('light')
    await nextTick()

    expect(store.t('dashboard.title')).toBe('Dashboard')
    expect(storedPreferences.get('opspilot.locale')).toBe('en')
    expect(storedPreferences.get('opspilot.theme')).toBe('light')
    expect(documentElement.lang).toBe('en')
    expect(documentElement.dataset.theme).toBe('light')
  })
})
