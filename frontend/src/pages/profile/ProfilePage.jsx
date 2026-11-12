import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import { CameraIcon, KeyIcon, MoonIcon, SunIcon, UserCircleIcon } from '@heroicons/react/24/outline';
import { userApi } from '../../services/userApi';
import { setUserProfile } from '../../features/auth/authSlice';
import { showToast } from '../../features/feedback/feedbackSlice';
import { CURRENCIES } from '../../constants/enums';
import { useTranslation } from '../../i18n/I18nContext';
import { useTheme } from '../../theme/ThemeContext';
import { extractErrorMessage, validateImageFile } from '../../utils/formatters';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+=\-]).{8,72}$/;

export default function ProfilePage() {
  const dispatch = useDispatch();
  const { t, language, setLanguage } = useTranslation();
  const { theme, setTheme } = useTheme();
  const user = useSelector((state) => state.auth.user);

  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [changingPassword, setChangingPassword] = useState(false);

  const profileForm = useForm({
    defaultValues: {
      fullName: user?.fullName || '',
      currency: user?.currency || 'VND',
    },
  });

  const passwordForm = useForm({
    defaultValues: {
      currentPassword: '',
      newPassword: '',
      confirmPassword: '',
    },
  });

  useEffect(() => {
    let mounted = true;
    userApi
      .getCurrentProfile()
      .then((res) => {
        if (!mounted || !res.data) return;
        dispatch(setUserProfile(res.data));
        profileForm.reset({
          fullName: res.data.fullName || '',
          currency: res.data.currency || 'VND',
        });
      })
      .catch(() => {});
    return () => {
      mounted = false;
    };
  }, [dispatch, profileForm]);

  const handleAvatarChange = async (e) => {
    const file = e.target.files?.[0];
    e.target.value = '';
    if (!file) return;
    const validationErr = validateImageFile(file, 5, t);
    if (validationErr) {
      dispatch(showToast({ type: 'error', message: validationErr }));
      return;
    }

    setUploadingAvatar(true);
    try {
      const res = await userApi.uploadAvatar(file);
      if (res.data) {
        dispatch(setUserProfile(res.data));
      }
      dispatch(showToast({ type: 'success', message: t('profile.avatarUpdated') }));
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('profile.avatarError')),
        })
      );
    } finally {
      setUploadingAvatar(false);
    }
  };

  const onSubmitProfile = async (values) => {
    setSavingProfile(true);
    try {
      const res = await userApi.updateProfile({
        fullName: values.fullName.trim(),
        currency: values.currency,
      });
      if (res.data) {
        dispatch(setUserProfile(res.data));
      }
      dispatch(showToast({ type: 'success', message: t('profile.profileUpdated') }));
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('profile.profileError')),
        })
      );
    } finally {
      setSavingProfile(false);
    }
  };

  const onSubmitPassword = async (values) => {
    setChangingPassword(true);
    try {
      await userApi.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });
      passwordForm.reset();
      dispatch(showToast({ type: 'success', message: t('profile.passwordChanged') }));
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, t('profile.passwordError')),
        })
      );
    } finally {
      setChangingPassword(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div className="lg:col-span-6 card-surface p-6 space-y-6">
        <div className="flex items-center gap-2.5 border-b border-slate-100 dark:border-zinc-800 pb-4">
          <UserCircleIcon className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
          <div>
            <h2 className="text-base font-bold text-slate-900 dark:text-white">{t('profile.accountInfoTitle')}</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">{t('profile.accountInfoSubtitle')}</p>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="relative h-16 w-16 rounded-2xl bg-emerald-600 text-white text-xl font-bold flex items-center justify-center overflow-hidden shrink-0">
            {user?.avatarUrl ? (
              <img src={user.avatarUrl} alt={user.fullName} className="h-full w-full object-cover" />
            ) : (
              (user?.fullName?.[0] || 'U').toUpperCase()
            )}
          </div>
          <div className="space-y-1.5">
            <label className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 dark:border-zinc-700 bg-white dark:bg-zinc-900 hover:bg-slate-50 dark:hover:bg-zinc-800 px-3 py-1.5 text-xs font-semibold text-slate-700 dark:text-slate-200 cursor-pointer">
              <CameraIcon className="h-4 w-4 text-slate-500 dark:text-slate-400" />
              {uploadingAvatar ? t('profile.uploadingAvatar') : t('profile.changeAvatar')}
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp"
                className="hidden"
                disabled={uploadingAvatar}
                onChange={handleAvatarChange}
              />
            </label>
            <p className="text-[11px] text-slate-400 dark:text-slate-500">{t('common.uploadMaxHint')}</p>
          </div>
        </div>

        <form onSubmit={profileForm.handleSubmit(onSubmitProfile)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.email')}
            </label>
            <input
              type="email"
              value={user?.email || ''}
              disabled
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-slate-100 dark:bg-zinc-900 px-3.5 py-2.5 text-sm text-slate-500 dark:text-slate-400 outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.fullName')}
            </label>
            <input
              type="text"
              {...profileForm.register('fullName', {
                required: t('profile.fullNameRequired'),
                minLength: { value: 2, message: t('profile.fullNameMin') },
                maxLength: { value: 100, message: t('profile.fullNameMax') },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {profileForm.formState.errors.fullName && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {profileForm.formState.errors.fullName.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.defaultCurrency')}
            </label>
            <select
              {...profileForm.register('currency')}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
            >
              {CURRENCIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.languageLabel')}
            </label>
            <div className="grid grid-cols-2 gap-2.5">
              {[
                { value: 'vi', label: t('profile.languageVi') },
                { value: 'en', label: t('profile.languageEn') },
              ].map((opt) => (
                <button
                  key={opt.value}
                  type="button"
                  onClick={() => setLanguage(opt.value)}
                  className={`rounded-xl border px-3.5 py-2.5 text-sm font-medium transition-colors text-left ${
                    language === opt.value
                      ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300'
                      : 'border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
            <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">{t('profile.languageHint')}</p>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.themeLabel')}
            </label>
            <div className="grid grid-cols-2 gap-2.5">
              {[
                { value: 'light', label: t('profile.themeLight'), Icon: SunIcon },
                { value: 'dark', label: t('profile.themeDark'), Icon: MoonIcon },
              ].map((opt) => {
                const ThemeIcon = opt.Icon;
                return (
                  <button
                    key={opt.value}
                    type="button"
                    onClick={() => setTheme(opt.value)}
                    className={`rounded-xl border px-3.5 py-2.5 text-sm font-medium transition-colors flex items-center gap-2 ${
                      theme === opt.value
                        ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300'
                        : 'border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-zinc-800'
                    }`}
                  >
                    <ThemeIcon className="h-4 w-4 shrink-0" />
                    <span>{opt.label}</span>
                  </button>
                );
              })}
            </div>
            <p className="text-[11px] text-slate-400 dark:text-slate-500 mt-1">{t('profile.themeHint')}</p>
          </div>

          <button
            type="submit"
            disabled={savingProfile}
            className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2.5 text-xs font-semibold transition-colors disabled:opacity-60"
          >
            {savingProfile ? t('common.saving') : t('profile.saveInfo')}
          </button>
        </form>
      </div>

      <div className="lg:col-span-6 card-surface p-6 space-y-6">
        <div className="flex items-center gap-2.5 border-b border-slate-100 dark:border-zinc-800 pb-4">
          <KeyIcon className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
          <div>
            <h2 className="text-base font-bold text-slate-900 dark:text-white">{t('profile.securityTitle')}</h2>
            <p className="text-xs text-slate-500 dark:text-slate-400">{t('profile.securitySubtitle')}</p>
          </div>
        </div>

        <form onSubmit={passwordForm.handleSubmit(onSubmitPassword)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.currentPassword')}
            </label>
            <input
              type="password"
              {...passwordForm.register('currentPassword', {
                required: t('profile.currentPasswordRequired'),
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {passwordForm.formState.errors.currentPassword && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {passwordForm.formState.errors.currentPassword.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.newPassword')}
            </label>
            <input
              type="password"
              placeholder={t('profile.passwordHint')}
              {...passwordForm.register('newPassword', {
                required: t('profile.newPasswordRequired'),
                pattern: {
                  value: PASSWORD_REGEX,
                  message: t('profile.passwordRuleError'),
                },
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {passwordForm.formState.errors.newPassword && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {passwordForm.formState.errors.newPassword.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1.5">
              {t('profile.confirmNewPassword')}
            </label>
            <input
              type="password"
              {...passwordForm.register('confirmPassword', {
                required: t('profile.confirmRequired'),
                validate: (val) =>
                  val === passwordForm.watch('newPassword') || t('profile.passwordMismatch'),
              })}
              className="w-full rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 text-slate-900 dark:text-slate-100 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {passwordForm.formState.errors.confirmPassword && (
              <p className="text-xs text-rose-600 dark:text-rose-400 mt-1">
                {passwordForm.formState.errors.confirmPassword.message}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={changingPassword}
            className="rounded-xl bg-slate-900 dark:bg-emerald-600 hover:bg-slate-800 dark:hover:bg-emerald-700 text-white px-5 py-2.5 text-xs font-semibold transition-colors disabled:opacity-60"
          >
            {changingPassword ? t('profile.updating') : t('profile.changePasswordBtn')}
          </button>
        </form>
      </div>
    </div>
  );
}
