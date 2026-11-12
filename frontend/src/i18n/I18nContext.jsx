import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { GlobeAltIcon } from '@heroicons/react/24/outline';
import { storage } from '../utils/storage';
import vi from './translations/vi';
import en from './translations/en';

const DICTIONARIES = { vi, en };

const I18nContext = createContext({
  language: 'vi',
  setLanguage: () => {},
  toggleLanguage: () => {},
  t: (key) => key,
});

function resolveKey(dict, key) {
  return key.split('.').reduce((acc, part) => (acc && acc[part] !== undefined ? acc[part] : undefined), dict);
}

function interpolate(template, params) {
  if (!params || typeof template !== 'string') return template;
  return template.replace(/\{\{(\w+)\}\}/g, (_, token) =>
    params[token] !== undefined && params[token] !== null ? String(params[token]) : ''
  );
}

export function I18nProvider({ children }) {
  const [language, setLanguageState] = useState(() => storage.getLanguage());

  const setLanguage = useCallback((nextLang) => {
    const normalized = nextLang === 'en' ? 'en' : 'vi';
    storage.setLanguage(normalized);
    setLanguageState(normalized);
  }, []);

  const toggleLanguage = useCallback(() => {
    setLanguage(language === 'vi' ? 'en' : 'vi');
  }, [language, setLanguage]);

  const t = useCallback(
    (key, params) => {
      const activeDict = DICTIONARIES[language] || vi;
      const val = resolveKey(activeDict, key) ?? resolveKey(vi, key);
      if (typeof val !== 'string') return key;
      return interpolate(val, params);
    },
    [language]
  );

  useEffect(() => {
    document.documentElement.lang = language;
    document.title = t('nav.documentTitle');
  }, [language, t]);

  const value = useMemo(
    () => ({ language, setLanguage, toggleLanguage, t }),
    [language, setLanguage, toggleLanguage, t]
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useTranslation() {
  return useContext(I18nContext);
}

export function LanguageSwitcher({ className = '' }) {
  const { language, setLanguage, t } = useTranslation();

  return (
    <div
      className={`inline-flex items-center gap-1 rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-950 p-0.5 shadow-sm ${className}`}
      role="group"
      aria-label={t('header.switchLangAria')}
    >
      <GlobeAltIcon className="h-3.5 w-3.5 text-slate-400 dark:text-slate-500 ml-1.5 mr-0.5 hidden sm:block" />
      <button
        type="button"
        onClick={() => setLanguage('vi')}
        className={`px-2 py-1 rounded-lg text-[11px] font-bold transition-colors ${
          language === 'vi'
            ? 'bg-emerald-600 text-white'
            : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-zinc-800'
        }`}
      >
        VI
      </button>
      <button
        type="button"
        onClick={() => setLanguage('en')}
        className={`px-2 py-1 rounded-lg text-[11px] font-bold transition-colors ${
          language === 'en'
            ? 'bg-emerald-600 text-white'
            : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-zinc-800'
        }`}
      >
        EN
      </button>
    </div>
  );
}

