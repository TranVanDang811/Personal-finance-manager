import { Suspense, lazy } from 'react';
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ProtectedRoute, PublicRoute } from './routes/RouteGuards';
import LoadingSpinner from './components/feedback/LoadingSpinner';
import AuthLayout from './layouts/AuthLayout';
import MainLayout from './layouts/MainLayout';

const LoginPage = lazy(() => import('./pages/auth/LoginPage'));
const RegisterPage = lazy(() => import('./pages/auth/RegisterPage'));
const ForgotPasswordPage = lazy(() => import('./pages/auth/ForgotPasswordPage'));
const ResetPasswordPage = lazy(() => import('./pages/auth/ResetPasswordPage'));
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const AccountsPage = lazy(() => import('./pages/accounts/AccountsPage'));
const CategoriesPage = lazy(() => import('./pages/categories/CategoriesPage'));
const TransactionsPage = lazy(() => import('./pages/transactions/TransactionsPage'));
const BudgetsPage = lazy(() => import('./pages/budgets/BudgetsPage'));
const SavingGoalsPage = lazy(() => import('./pages/savings/SavingGoalsPage'));
const RecurringTransactionsPage = lazy(() => import('./pages/recurring/RecurringTransactionsPage'));
const ReportsPage = lazy(() => import('./pages/reports/ReportsPage'));
const NotificationsPage = lazy(() => import('./pages/notifications/NotificationsPage'));
const ProfilePage = lazy(() => import('./pages/profile/ProfilePage'));

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
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
}

