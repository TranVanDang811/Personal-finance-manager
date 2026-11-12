import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useForm } from 'react-hook-form';
import { authApi } from '../../services/authApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import { extractErrorMessage } from '../../utils/formatters';
import { useTranslation } from '../../i18n/I18nContext';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+=\-]).{8,72}$/;

export default function ResetPasswordPage() {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [submitting, setSubmitting] = useState(false);

  const {
    register,
    watch,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      token: searchParams.get('token') || '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  const onSubmit = async (values) => {
    setSubmitting(true);
    try {
      const res = await authApi.resetPassword({
        token: values.token.trim(),
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });
      dispatch(
        showToast({
          type: 'success',
          message: res.message || t('auth.resetSuccessToast'),
        })
      );
      navigate('/login', { replace: true });
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('auth.resetErrorToast')),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card-surface p-6 sm:p-8">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white">{t('auth.resetTitle')}</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t('auth.resetSubtitle')}</p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            {t('auth.resetTokenLabel')}
          </label>
          <input
            type="text"
            placeholder={t('auth.resetTokenPlaceholder')}
            {...register('token', { required: t('auth.resetTokenRequired') })}
            className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm font-mono focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.token && <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.token.message}</p>}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            {t('auth.newPasswordLabel')}
          </label>
          <input
            type="password"
            placeholder={t('auth.passwordHint')}
            {...register('newPassword', {
              required: t('auth.newPasswordRequired'),
              pattern: {
                value: PASSWORD_REGEX,
                message: t('auth.passwordRuleError'),
              },
            })}
            className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.newPassword && (
            <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.newPassword.message}</p>
          )}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
            {t('auth.confirmNewPasswordLabel')}
          </label>
          <input
            type="password"
            placeholder={t('auth.confirmPasswordPlaceholder')}
            {...register('confirmPassword', {
              required: t('auth.confirmNewPasswordRequired'),
              validate: (val) => val === watch('newPassword') || t('auth.passwordMismatch'),
            })}
            className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.confirmPassword && (
            <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">{errors.confirmPassword.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors disabled:opacity-60"
        >
          {submitting ? t('auth.updating') : t('auth.resetBtn')}
        </button>
      </form>

      <p className="mt-6 text-center text-xs text-slate-500 dark:text-slate-400">
        <Link to="/login" className="font-semibold text-emerald-600 dark:text-emerald-400 hover:text-emerald-700 dark:hover:text-emerald-300">
          {t('auth.backToLogin')}
        </Link>
      </p>
    </div>
  );
}
