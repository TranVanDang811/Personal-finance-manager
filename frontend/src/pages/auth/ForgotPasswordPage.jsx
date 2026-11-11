import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { useForm } from 'react-hook-form';
import { CheckCircleIcon } from '@heroicons/react/24/outline';
import { authApi } from '../../services/authApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import { extractErrorMessage } from '../../utils/formatters';

export default function ForgotPasswordPage() {
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
          message: res.message || 'Nếu email tồn tại, hướng dẫn đặt lại mật khẩu đã được gửi.',
        })
      );
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể gửi yêu cầu khôi phục mật khẩu.'),
        })
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card-surface p-6 sm:p-8">
      <div className="mb-6">
        <h2 className="text-2xl font-bold text-slate-900">Quên mật khẩu</h2>
        <p className="text-sm text-slate-500 mt-1">
          Nhập email đã đăng ký để nhận mã đặt lại mật khẩu.
        </p>
      </div>

      {submitted ? (
        <div className="space-y-4">
          <div className="rounded-xl bg-emerald-50 border border-emerald-200 p-4 flex items-start gap-3">
            <CheckCircleIcon className="h-5 w-5 text-emerald-600 shrink-0 mt-0.5" />
            <div className="text-xs text-emerald-900 space-y-1">
              <p className="font-semibold">Yêu cầu đã được tiếp nhận</p>
              <p className="leading-relaxed">
                Nếu email tồn tại trong hệ thống, mã đặt lại mật khẩu đã được tạo và gửi đi (hoặc ghi nhận trong nhật ký dịch vụ ở môi trường dev).
              </p>
            </div>
          </div>

          <Link
            to="/reset-password"
            className="block w-full text-center rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors"
          >
            Nhập mã đặt lại mật khẩu
          </Link>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
          <div>
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

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-sm py-2.5 transition-colors disabled:opacity-60"
          >
            {submitting ? 'Đang gửi yêu cầu...' : 'Gửi hướng dẫn khôi phục'}
          </button>
        </form>
      )}

      <p className="mt-6 text-center text-xs text-slate-500">
        Nhớ mật khẩu rồi?{' '}
        <Link to="/login" className="font-semibold text-emerald-600 hover:text-emerald-700">
          Quay lại đăng nhập
        </Link>
      </p>
    </div>
  );
}

