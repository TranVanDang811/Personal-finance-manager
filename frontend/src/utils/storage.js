const ACCESS_TOKEN_KEY = 'pfm_access_token';
const REFRESH_TOKEN_KEY = 'pfm_refresh_token';
const USER_KEY = 'pfm_user';
const LANGUAGE_KEY = 'pfm_language';
const THEME_KEY = 'pfm_theme';

export const storage = {
  getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },
  getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },
  getUser() {
    try {
      const raw = localStorage.getItem(USER_KEY);
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  },
  setAuthSession({ accessToken, refreshToken, user }) {
    if (accessToken) localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    if (refreshToken) localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  setUser(user) {
    if (user) localStorage.setItem(USER_KEY, JSON.stringify(user));
  },
  clearAuthSession() {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  },
  getLanguage() {
    const saved = localStorage.getItem(LANGUAGE_KEY);
    return saved === 'en' ? 'en' : 'vi';
  },
  setLanguage(lang) {
    localStorage.setItem(LANGUAGE_KEY, lang === 'en' ? 'en' : 'vi');
  },
  getTheme() {
    const saved = localStorage.getItem(THEME_KEY);
    return saved === 'dark' ? 'dark' : 'light';
  },
  setTheme(theme) {
    localStorage.setItem(THEME_KEY, theme === 'dark' ? 'dark' : 'light');
  },
};
