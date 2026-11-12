import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import { clearAuthError, registerUser } from '../../features/auth/authSlice';
import { showToast } from '../../features/feedback/feedbackSlice';
import { CURRENCIES } from '../../constants/enums';
import { useTranslation } from '../../i18n/I18nContext';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+=\-]).{8,72}$/;

export default function RegisterPage() {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { loading, error } = useSelector((state) => state.auth);
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      fullName: '',
      email: '',
      currency: 'VND',
      password: '',
      confirmPassword: '',
    },
  });

  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch]);

  const onSubmit = async (values) => {
    const payload = {
      fullName: values.fullName.trim(),
      email: values.email.trim(),
      currency: values.currency,
      password: values.password,
    };
    const result = await dispatch(registerUser(payload));
    if (registerUser.fulfilled.match(result)) {
      dispatch(showToast({ type: 'success', message: t('auth.registerSuccess') }));
      navigate('/dashboard', { replace: true });
    }
  };

  return (
    <div className="card-surface p-6 sm:p-8">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white">{t('auth.registerTitle')}</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t('auth.registerSubtitle')}</p>
      </div>

      {error && (
        <div className="mb-4 rounded-xl bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-800 px-4 py-3 text-xs font-medium text-rose-700 dark:text-rose-300">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            {t('auth.fullNameLabel')}
          </label>
          <input
            type="text"
            placeholder={t('auth.fullNamePlaceholder')}
            {...register('fullName', {
              required: t('auth.fullNameRequired'),
              minLength: { value: 2, message: t('auth.fullNameMin') },
              maxLength: { value: 100, message: t('auth.fullNameMax') },
            })}
            className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.fullName && (
            <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.fullName.message}</p>
          )}
        </div>

        <div className="grid sm:grid-cols-3 gap-3">
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('auth.emailLabel')}
            </label>
            <input
              type="email"
              placeholder="name@example.com"
              {...register('email', {
                required: t('auth.emailRequired'),
                pattern: {
                  value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                  message: t('auth.emailInvalid'),
                },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
            />
            {errors.email && <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.email.message}</p>}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('auth.currencyLabel')}
            </label>
            <select
              {...register('currency')}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
            >
              {CURRENCIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            {t('auth.passwordLabel')}
          </label>
          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder={t('auth.passwordHint')}
              {...register('password', {
                required: t('auth.passwordRequired'),
                pattern: {
                  value: PASSWORD_REGEX,
                  message: t('auth.passwordRuleError'),
                },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 pr-10 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
            />
            <button
              type="button"
              onClick={() => setShowPassword((prev) => !prev)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200"
              aria-label={t('auth.togglePasswordAria')}
            >
              {showPassword ? <EyeSlashIcon className="h-4 w-4" /> : <EyeIcon className="h-4 w-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.password.message}</p>
          )}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            {t('auth.confirmPasswordLabel')}
          </label>
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder={t('auth.confirmPasswordPlaceholder')}
            {...register('confirmPassword', {
              required: t('auth.confirmPasswordRequired'),
              validate: (val) => val === watch('password') || t('auth.passwordMismatch'),
            })}
            className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.confirmPassword && (
            <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.confirmPassword.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors disabled:opacity-60"
        >
          {loading ? t('auth.creatingAccount') : t('auth.registerBtn')}
        </button>
      </form>

      <p className="mt-6 text-center text-xs text-slate-500 dark:text-slate-400">
        {t('auth.hasAccount')}{' '}
        <Link to="/login" className="font-semibold text-emerald-600 dark:text-emerald-400 hover:text-emerald-700 dark:hover:text-emerald-300">
          {t('auth.loginLink')}
        </Link>
      </p>
    </div>
  );
}
