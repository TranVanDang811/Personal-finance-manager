import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import { CameraIcon, KeyIcon, UserCircleIcon } from '@heroicons/react/24/outline';
import { userApi } from '../../services/userApi';
import { setUserProfile } from '../../features/auth/authSlice';
import { showToast } from '../../features/feedback/feedbackSlice';
import { CURRENCIES } from '../../constants/enums';
import { extractErrorMessage, validateImageFile } from '../../utils/formatters';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+=\-]).{8,72}$/;

export default function ProfilePage() {
  const dispatch = useDispatch();
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
    const validationErr = validateImageFile(file, 5);
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
      dispatch(showToast({ type: 'success', message: 'Cập nhật ảnh đại diện thành công.' }));
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải ảnh đại diện.'),
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
      dispatch(showToast({ type: 'success', message: 'Cập nhật thông tin cá nhân thành công.' }));
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể cập nhật hồ sơ.'),
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
      dispatch(showToast({ type: 'success', message: 'Đổi mật khẩu thành công.' }));
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể đổi mật khẩu.'),
        })
      );
    } finally {
      setChangingPassword(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
      <div className="lg:col-span-6 card-surface p-6 space-y-6">
        <div className="flex items-center gap-2.5 border-b border-slate-100 pb-4">
          <UserCircleIcon className="h-5 w-5 text-emerald-600" />
          <div>
            <h2 className="text-base font-bold text-slate-900">Thông tin tài khoản</h2>
            <p className="text-xs text-slate-500">Quản lý tên hiển thị, tiền tệ mặc định và ảnh đại diện</p>
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
            <label className="inline-flex items-center gap-1.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 px-3 py-1.5 text-xs font-semibold text-slate-700 cursor-pointer">
              <CameraIcon className="h-4 w-4 text-slate-500" />
              {uploadingAvatar ? 'Đang tải ảnh...' : 'Đổi ảnh đại diện'}
              <input
                type="file"
                accept="image/png,image/jpeg,image/webp"
                className="hidden"
                disabled={uploadingAvatar}
                onChange={handleAvatarChange}
              />
            </label>
            <p className="text-[11px] text-slate-400">Hỗ trợ JPG, PNG, WEBP tối đa 5MB.</p>
          </div>
        </div>

        <form onSubmit={profileForm.handleSubmit(onSubmitProfile)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Email</label>
            <input
              type="email"
              value={user?.email || ''}
              disabled
              className="w-full rounded-xl border border-slate-200 bg-slate-100 px-3.5 py-2.5 text-sm text-slate-500 outline-none"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Họ và tên</label>
            <input
              type="text"
              {...profileForm.register('fullName', {
                required: 'Vui lòng nhập họ tên',
                minLength: { value: 2, message: 'Họ tên tối thiểu 2 ký tự' },
                maxLength: { value: 100, message: 'Họ tên tối đa 100 ký tự' },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {profileForm.formState.errors.fullName && (
              <p className="text-xs text-rose-600 mt-1">
                {profileForm.formState.errors.fullName.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Tiền tệ mặc định</label>
            <select
              {...profileForm.register('currency')}
              className="w-full rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm outline-none focus:border-emerald-500"
            >
              {CURRENCIES.map((c) => (
                <option key={c.value} value={c.value}>
                  {c.label}
                </option>
              ))}
            </select>
          </div>

          <button
            type="submit"
            disabled={savingProfile}
            className="rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-5 py-2.5 text-xs font-semibold transition-colors disabled:opacity-60"
          >
            {savingProfile ? 'Đang lưu...' : 'Lưu thông tin'}
          </button>
        </form>
      </div>

      <div className="lg:col-span-6 card-surface p-6 space-y-6">
        <div className="flex items-center gap-2.5 border-b border-slate-100 pb-4">
          <KeyIcon className="h-5 w-5 text-emerald-600" />
          <div>
            <h2 className="text-base font-bold text-slate-900">Bảo mật & Mật khẩu</h2>
            <p className="text-xs text-slate-500">Thay đổi mật khẩu định kỳ để bảo vệ tài khoản</p>
          </div>
        </div>

        <form onSubmit={passwordForm.handleSubmit(onSubmitPassword)} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Mật khẩu hiện tại
            </label>
            <input
              type="password"
              {...passwordForm.register('currentPassword', {
                required: 'Vui lòng nhập mật khẩu hiện tại',
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {passwordForm.formState.errors.currentPassword && (
              <p className="text-xs text-rose-600 mt-1">
                {passwordForm.formState.errors.currentPassword.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Mật khẩu mới</label>
            <input
              type="password"
              placeholder="Tối thiểu 8 ký tự, gồm hoa, thường, số, ký tự đặc biệt"
              {...passwordForm.register('newPassword', {
                required: 'Vui lòng nhập mật khẩu mới',
                pattern: {
                  value: PASSWORD_REGEX,
                  message: 'Mật khẩu 8–72 ký tự gồm chữ hoa, chữ thường, số và ký tự đặc biệt',
                },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {passwordForm.formState.errors.newPassword && (
              <p className="text-xs text-rose-600 mt-1">
                {passwordForm.formState.errors.newPassword.message}
              </p>
            )}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">
              Xác nhận mật khẩu mới
            </label>
            <input
              type="password"
              {...passwordForm.register('confirmPassword', {
                required: 'Vui lòng xác nhận mật khẩu mới',
                validate: (val) =>
                  val === passwordForm.watch('newPassword') || 'Mật khẩu xác nhận không khớp',
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm outline-none focus:border-emerald-500"
            />
            {passwordForm.formState.errors.confirmPassword && (
              <p className="text-xs text-rose-600 mt-1">
                {passwordForm.formState.errors.confirmPassword.message}
              </p>
            )}
          </div>

          <button
            type="submit"
            disabled={changingPassword}
            className="rounded-xl bg-slate-900 hover:bg-slate-800 text-white px-5 py-2.5 text-xs font-semibold transition-colors disabled:opacity-60"
          >
            {changingPassword ? 'Đang cập nhật...' : 'Đổi mật khẩu'}
          </button>
        </form>
      </div>
    </div>
  );
}

