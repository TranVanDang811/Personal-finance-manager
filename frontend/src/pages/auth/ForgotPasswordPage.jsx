import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useForm } from 'react-hook-form';
import { CheckCircleIcon } from '@heroicons/react/24/outline';
import { authApi } from '../../services/authApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import { extractErrorMessage } from '../../utils/formatters';
import { useTranslation } from '../../i18n/I18nContext';

export default function ForgotPasswordPage() {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const [submitting, setSubmitting] = useState(false);
  const [submitted, setSubmitted] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({ defaultValues: { email: '' } });

  const onSubmit = async ({ email }) => {
    setSubmitting(true);
    try {
      const res = await authApi.forgotPassword(email.trim());
      setSubmitted(true);
      dispatch(
        showToast({
          type: 'success',
          message: res.message || t('auth.forgotSuccessToast'),
        })
      );
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('auth.forgotErrorToast')),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card-surface p-6 sm:p-8">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-900 dark:text-white">{t('auth.forgotTitle')}</h2>
        <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">{t('auth.forgotSubtitle')}</p>
      </div>

      {submitted ? (
        <div className="space-y-4">
          <div className="rounded-xl bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-800 p-4 flex items-start gap-3">
            <CheckCircleIcon className="h-5 w-5 text-emerald-600 dark:text-emerald-400 shrink-0 mt-0.5" />
            <div className="text-xs text-emerald-900 dark:text-emerald-200 space-y-1">
              <p className="font-semibold">{t('auth.forgotSentTitle')}</p>
              <p className="leading-relaxed">{t('auth.forgotSentDesc')}</p>
            </div>
          </div>

          <Link
            to="/reset-password"
            className="block w-full text-center rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors"
          >
            {t('auth.enterResetToken')}
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
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

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors disabled:opacity-60"
          >
            {submitting ? t('auth.sendingRequest') : t('auth.sendResetBtn')}
          </button>
        </form>
      )}

      <p className="mt-6 text-center text-xs text-slate-500 dark:text-slate-400">
        {t('auth.rememberedPassword')}{' '}
        <Link to="/login" className="font-semibold text-emerald-600 dark:text-emerald-400 hover:text-emerald-700 dark:hover:text-emerald-300">
          {t('auth.backToLogin')}
        </Link>
      </p>
    </div>
  );
}
