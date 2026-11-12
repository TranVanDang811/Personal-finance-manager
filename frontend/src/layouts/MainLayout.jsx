import { useEffect, useState } from 'react';
import { Outlet } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import Sidebar from '../components/layout/Sidebar';
import Header from '../components/layout/Header';
import ToastContainer from '../components/feedback/ToastContainer';
import { fetchCurrentUser } from '../features/auth/authSlice';
import {
  fetchRecentNotifications,
  fetchUnreadCount,
} from '../features/notifications/notificationSlice';

export default function MainLayout() {
  const dispatch = useDispatch();
  const { isAuthenticated, user } = useSelector((state) => state.auth);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (!isAuthenticated) return;
    if (!user) {
      dispatch(fetchCurrentUser());
    }
    dispatch(fetchUnreadCount());
    dispatch(fetchRecentNotifications());

    const intervalId = setInterval(() => {
      dispatch(fetchUnreadCount());
    }, 60000);

    return () => clearInterval(intervalId);
  }, [dispatch, isAuthenticated, user]);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 dark:bg-black dark:text-slate-100 transition-colors duration-200">
      <Sidebar
        mobileOpen={mobileMenuOpen}
        onCloseMobile={() => setMobileMenuOpen(false)}
      />

      <div className="lg:pl-64 flex flex-col min-h-screen">
        <Header onOpenMobileMenu={() => setMobileMenuOpen(true)} />
        <main className="flex-1 p-4 sm:p-6 lg:p-8 max-w-7xl w-full mx-auto">
          <Outlet />
        </main>
      </div>

      <ToastContainer />
    </div>
  );
}

