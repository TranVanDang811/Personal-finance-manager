import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import { MoonIcon, SunIcon } from '@heroicons/react/24/outline';
import { storage } from '../utils/storage';
import { useTranslation } from '../i18n/I18nContext';

const ThemeContext = createContext(null);

function applyThemeToDocument(theme) {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;
  if (theme === 'dark') {
    root.classList.add('dark');
  } else {
    root.classList.remove('dark');
  }
}

export function ThemeProvider({ children }) {
  const [theme, setThemeState] = useState(() => {
    const initial = storage.getTheme();
    applyThemeToDocument(initial);
    return initial;
  });

  useEffect(() => {
    applyThemeToDocument(theme);
  }, [theme]);

  const setTheme = useCallback((nextTheme) => {
    const normalized = nextTheme === 'dark' ? 'dark' : 'light';
    storage.setTheme(normalized);
    setThemeState(normalized);
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme(theme === 'dark' ? 'light' : 'dark');
  }, [theme, setTheme]);

  const value = useMemo(
    () => ({
      theme,
      isDark: theme === 'dark',
      setTheme,
      toggleTheme,
    }),
    [theme, setTheme, toggleTheme]
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

export function useTheme() {
  const ctx = useContext(ThemeContext);
  if (!ctx) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return ctx;
}

export function ThemeSwitcher({ className = '', showLabels = true }) {
  const { theme, setTheme } = useTheme();
  const { t } = useTranslation();

  return (
    <div
      className={`inline-flex items-center gap-1 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-900 p-0.5 shadow-sm ${className}`}
      role="group"
      aria-label={t('header.switchThemeAria')}
    >
      <button
        type="button"
        onClick={() => setTheme('light')}
        title={t('header.switchToLight')}
        aria-pressed={theme === 'light'}
        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors ${
          theme === 'light'
            ? 'bg-emerald-600 text-white shadow-sm'
            : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
        }`}
      >
        <SunIcon className="h-3.5 w-3.5" />
        {showLabels && <span>{t('header.lightMode')}</span>}
      </button>
      <button
        type="button"
        onClick={() => setTheme('dark')}
        title={t('header.switchToDark')}
        aria-pressed={theme === 'dark'}
        className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors ${
          theme === 'dark'
            ? 'bg-emerald-600 text-white shadow-sm'
            : 'text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800'
        }`}
      >
        <MoonIcon className="h-3.5 w-3.5" />
        {showLabels && <span>{t('header.darkMode')}</span>}
      </button>
    </div>
  );
}

