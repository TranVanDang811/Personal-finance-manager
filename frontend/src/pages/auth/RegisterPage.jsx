import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { useForm } from 'react-hook-form';
import { EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import { clearAuthError, registerUser } from '../../features/auth/authSlice';
import { showToast } from '../../features/feedback/feedbackSlice';
import { CURRENCIES } from '../../constants/enums';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+=\-]).{8,72}$/;

export default function RegisterPage() {
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
      dispatch(showToast({ type: 'success', message: 'Tạo tài khoản thành công!' }));
      navigate('/dashboard', { replace: true });
    }
  };

  return (
    <div className="card-surface p-6 sm:p-8">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Tạo tài khoản mới</h2>
        <p className="text-sm text-slate-500 mt-1">
          Bắt đầu quản lý tài chính cá nhân thông minh cùng FinFlow.
        </p>
      </div>

      {error && (
        <div className="mb-4 rounded-xl bg-rose-50 border border-rose-200 px-4 py-3 text-xs font-medium text-rose-700">
          {error}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">Họ và tên</label>
          <input
            type="text"
            placeholder="Nguyễn Văn A"
            {...register('fullName', {
              required: 'Vui lòng nhập họ tên',
              minLength: { value: 2, message: 'Họ tên tối thiểu 2 ký tự' },
              maxLength: { value: 100, message: 'Họ tên tối đa 100 ký tự' },
            })}
            className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.fullName && (
            <p className="text-xs text-rose-600 mt-1">{errors.fullName.message}</p>
          )}
        </div>

        <div className="grid sm:grid-cols-3 gap-3">
          <div className="sm:col-span-2">
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Email</label>
            <input
              type="email"
              placeholder="name@example.com"
              {...register('email', {
                required: 'Vui lòng nhập email',
                pattern: {
                  value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                  message: 'Email không hợp lệ',
                },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
            />
            {errors.email && <p className="text-xs text-rose-600 mt-1">{errors.email.message}</p>}
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1.5">Tiền tệ</label>
            <select
              {...register('currency')}
              className="w-full rounded-xl border border-slate-200 px-3 py-2.5 text-sm bg-white focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
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
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">Mật khẩu</label>
          <div className="relative">
            <input
              type={showPassword ? 'text' : 'password'}
              placeholder="Tối thiểu 8 ký tự, gồm hoa, thường, số, ký tự đặc biệt"
              {...register('password', {
                required: 'Vui lòng nhập mật khẩu',
                pattern: {
                  value: PASSWORD_REGEX,
                  message: 'Mật khẩu 8–72 ký tự gồm chữ hoa, chữ thường, số và ký tự đặc biệt',
                },
              })}
              className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 pr-10 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
            />
            <button
              type="button"
              onClick={() => setShowPassword((prev) => !prev)}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600"
            >
              {showPassword ? <EyeSlashIcon className="h-4 w-4" /> : <EyeIcon className="h-4 w-4" />}
            </button>
          </div>
          {errors.password && (
            <p className="text-xs text-rose-600 mt-1">{errors.password.message}</p>
          )}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">
            Xác nhận mật khẩu
          </label>
          <input
            type={showPassword ? 'text' : 'password'}
            placeholder="Nhập lại mật khẩu"
            {...register('confirmPassword', {
              required: 'Vui lòng xác nhận mật khẩu',
              validate: (val) => val === watch('password') || 'Mật khẩu xác nhận không khớp',
            })}
            className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.confirmPassword && (
            <p className="text-xs text-rose-600 mt-1">{errors.confirmPassword.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors disabled:opacity-60"
        >
          {loading ? 'Đang tạo tài khoản...' : 'Đăng ký'}
        </button>
      </form>

      <p className="mt-6 text-center text-xs text-slate-500">
        Đã có tài khoản?{' '}
        <Link to="/login" className="font-semibold text-emerald-600 hover:text-emerald-700">
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}

