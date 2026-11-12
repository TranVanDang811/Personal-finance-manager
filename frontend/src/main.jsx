import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { store } from './store';
import { configureAxiosAuthCallbacks } from './services/axiosInstance';
import { clearCredentials, setCredentials } from './features/auth/authSlice';
import { resetNotifications } from './features/notifications/notificationSlice';
import { I18nProvider } from './i18n/I18nContext';
import { ThemeProvider } from './theme/ThemeContext';
import App from './App';
import './index.css';

configureAxiosAuthCallbacks({
  handleTokensRefreshed: (authData) => {
    store.dispatch(setCredentials(authData));
  },
  handleSessionExpired: () => {
    store.dispatch(clearCredentials());
    store.dispatch(resetNotifications());
  },
});

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <Provider store={store}>
      <ThemeProvider>
        <I18nProvider>
          <App />
        </I18nProvider>
      </ThemeProvider>
    </Provider>
  </StrictMode>
);
