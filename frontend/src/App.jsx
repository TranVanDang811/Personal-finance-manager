import { Suspense } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute, PublicRoute } from './routes/RouteGuards';
import LoadingSpinner from './components/feedback/LoadingSpinner';
import AuthLayout from './layouts/AuthLayout';
import MainLayout from './layouts/MainLayout';
import { lazyWithRetry } from './utils/lazyWithRetry';

const LoginPage = lazyWithRetry(() => import('./pages/auth/LoginPage'));
const RegisterPage = lazyWithRetry(() => import('./pages/auth/RegisterPage'));
const ForgotPasswordPage = lazyWithRetry(() => import('./pages/auth/ForgotPasswordPage'));
const ResetPasswordPage = lazyWithRetry(() => import('./pages/auth/ResetPasswordPage'));
const DashboardPage = lazyWithRetry(() => import('./pages/dashboard/DashboardPage'));
const AccountsPage = lazyWithRetry(() => import('./pages/accounts/AccountsPage'));
const CategoriesPage = lazyWithRetry(() => import('./pages/categories/CategoriesPage'));
const TransactionsPage = lazyWithRetry(() => import('./pages/transactions/TransactionsPage'));
const BudgetsPage = lazyWithRetry(() => import('./pages/budgets/BudgetsPage'));
const SavingGoalsPage = lazyWithRetry(() => import('./pages/savings/SavingGoalsPage'));
const RecurringTransactionsPage = lazyWithRetry(() => import('./pages/recurring/RecurringTransactionsPage'));
const ReportsPage = lazyWithRetry(() => import('./pages/reports/ReportsPage'));
const NotificationsPage = lazyWithRetry(() => import('./pages/notifications/NotificationsPage'));
const ProfilePage = lazyWithRetry(() => import('./pages/profile/ProfilePage'));
const NotFoundPage = lazyWithRetry(() => import('./pages/error/NotFoundPage'));

function RouteFallback() {
  return (
    <div className="min-h-[60vh] flex items-center justify-center">
      <LoadingSpinner />
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<RouteFallback />}>
        <Routes>
          <Route element={<PublicRoute />}>
            <Route element={<AuthLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />
              <Route path="/forgot-password" element={<ForgotPasswordPage />} />
              <Route path="/reset-password" element={<ResetPasswordPage />} />
            </Route>
          </Route>

          <Route element={<ProtectedRoute />}>
            <Route element={<MainLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/accounts" element={<AccountsPage />} />
              <Route path="/categories" element={<CategoriesPage />} />
              <Route path="/transactions" element={<TransactionsPage />} />
              <Route path="/budgets" element={<BudgetsPage />} />
              <Route path="/saving-goals" element={<SavingGoalsPage />} />
              <Route path="/recurring" element={<RecurringTransactionsPage />} />
              <Route path="/reports" element={<ReportsPage />} />
              <Route path="/notifications" element={<NotificationsPage />} />
              <Route path="/profile" element={<ProfilePage />} />
            </Route>
          </Route>

          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="*" element={<NotFoundPage />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}
