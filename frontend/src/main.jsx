import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Provider } from 'react-redux';
import { store } from './store';
import { configureAxiosAuthCallbacks } from './services/axiosInstance';
import { clearCredentials, setCredentials } from './features/auth/authSlice';
import { resetNotifications } from './features/notifications/notificationSlice';
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
      <App />
    </Provider>
  </StrictMode>
);
