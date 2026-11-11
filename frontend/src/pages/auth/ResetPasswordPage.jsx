import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useForm } from 'react-hook-form';
import { authApi } from '../../services/authApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import { extractErrorMessage } from '../../utils/formatters';

const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&#^()_+=\-]).{8,72}$/;

export default function ResetPasswordPage() {
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
          message: res.message || 'Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.',
        })
      );
      navigate('/login', { replace: true });
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể đặt lại mật khẩu.'),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card-surface p-6 sm:p-8">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Đặt lại mật khẩu</h2>
        <p className="text-sm text-slate-500 mt-1">
          Nhập mã xác nhận và thiết lập mật khẩu mới cho tài khoản của bạn.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">
            Mã khôi phục (Token)
          </label>
          <input
            type="text"
            placeholder="Dán mã reset token"
            {...register('token', { required: 'Vui lòng nhập mã khôi phục' })}
            className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm font-mono focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.token && <p className="text-xs text-rose-600 mt-1">{errors.token.message}</p>}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">Mật khẩu mới</label>
          <input
            type="password"
            placeholder="Tối thiểu 8 ký tự, gồm hoa, thường, số, ký tự đặc biệt"
            {...register('newPassword', {
              required: 'Vui lòng nhập mật khẩu mới',
              pattern: {
                value: PASSWORD_REGEX,
                message: 'Mật khẩu 8–72 ký tự gồm chữ hoa, chữ thường, số và ký tự đặc biệt',
              },
            })}
            className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.newPassword && (
            <p className="text-xs text-rose-600 mt-1">{errors.newPassword.message}</p>
          )}
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-700 mb-1.5">
            Xác nhận mật khẩu mới
          </label>
          <input
            type="password"
            placeholder="Nhập lại mật khẩu mới"
            {...register('confirmPassword', {
              required: 'Vui lòng xác nhận mật khẩu mới',
              validate: (val) => val === watch('newPassword') || 'Mật khẩu xác nhận không khớp',
            })}
            className="w-full rounded-xl border border-slate-200 px-3.5 py-2.5 text-sm focus:border-emerald-500 focus:ring-2 focus:ring-emerald-500/20 outline-none"
          />
          {errors.confirmPassword && (
            <p className="text-xs text-rose-600 mt-1">{errors.confirmPassword.message}</p>
          )}
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors disabled:opacity-60"
        >
          {submitting ? 'Đang cập nhật...' : 'Cập nhật mật khẩu'}
        </button>
      </form>

      <p className="mt-6 text-center text-xs text-slate-500">
        <Link to="/login" className="font-semibold text-emerald-600 hover:text-emerald-700">
          Quay lại đăng nhập
        </Link>
      </p>
    </div>
  );
}

