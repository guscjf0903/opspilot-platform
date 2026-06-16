import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { translate, type Locale, type Theme, type TranslationKey } from '../i18n/messages'

const LOCALE_STORAGE_KEY = 'opspilot.locale'
const THEME_STORAGE_KEY = 'opspilot.theme'

function getStoredLocale(): Locale {
  const storedLocale = window.localStorage.getItem(LOCALE_STORAGE_KEY)

  return storedLocale === 'en' || storedLocale === 'ko' ? storedLocale : 'ko'
}

function getStoredTheme(): Theme {
  const storedTheme = window.localStorage.getItem(THEME_STORAGE_KEY)

  return storedTheme === 'light' || storedTheme === 'dark' ? storedTheme : 'dark'
}

export const useUiPreferencesStore = defineStore('ui-preferences', () => {
  const locale = ref<Locale>(getStoredLocale())
  const theme = ref<Theme>(getStoredTheme())

  watch(
    locale,
    (selectedLocale) => {
      window.localStorage.setItem(LOCALE_STORAGE_KEY, selectedLocale)
      document.documentElement.lang = selectedLocale
    },
    { immediate: true },
  )

  watch(
    theme,
    (selectedTheme) => {
      window.localStorage.setItem(THEME_STORAGE_KEY, selectedTheme)
      document.documentElement.dataset.theme = selectedTheme
    },
    { immediate: true },
  )

  function setLocale(selectedLocale: Locale) {
    locale.value = selectedLocale
  }

  function setTheme(selectedTheme: Theme) {
    theme.value = selectedTheme
  }

  function t(key: TranslationKey) {
    return translate(locale.value, key)
  }

  return {
    locale,
    theme,
    setLocale,
    setTheme,
    t,
  }
})
