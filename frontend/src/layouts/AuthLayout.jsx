import { Outlet } from 'react-router-dom';
import { ChartBarIcon, ShieldCheckIcon, SparklesIcon, WalletIcon } from '@heroicons/react/24/outline';
import ToastContainer from '../components/feedback/ToastContainer';

const HIGHLIGHTS = [
  {
    icon: WalletIcon,
    title: 'Quản lý đa tài khoản tập trung',
    description: 'Theo dõi số dư tiền mặt, tài khoản ngân hàng và ví tiết kiệm theo thời gian thực.',
  },
  {
    icon: ChartBarIcon,
    title: 'Kiểm soát ngân sách & Cảnh báo thông minh',
    description: 'Nhận cảnh báo tự động khi chi tiêu đạt ngưỡng 80% hoặc chạm hạn mức tháng.',
  },
  {
    icon: ShieldCheckIcon,
    title: 'Giao dịch an toàn & Định kỳ tự động',
    description: 'Hỗ trợ Idempotency Key chống trùng lặp và tự động ghi nhận hóa đơn định kỳ.',
  },
];

export default function AuthLayout() {
  return (
    <div className="min-h-screen grid lg:grid-cols-12 bg-slate-50">
      <div className="hidden lg:flex lg:col-span-5 xl:col-span-6 bg-gradient-to-br from-slate-900 via-emerald-950 to-slate-900 text-white p-10 xl:p-14 flex-col justify-between relative overflow-hidden">
        <div className="relative z-10 flex items-center gap-3">
          <div className="h-10 w-10 rounded-xl bg-emerald-500 flex items-center justify-center shadow-lg">
            <SparklesIcon className="h-6 w-6 text-white" />
          </div>
          <div>
            <span className="text-xl font-bold tracking-tight">FinFlow</span>
            <p className="text-xs text-emerald-300">Personal Finance Manager</p>
          </div>
        </div>

        <div className="relative z-10 max-w-lg space-y-8 my-auto py-8">
          <div>
            <h1 className="text-3xl xl:text-4xl font-bold leading-tight tracking-tight">
              Kiểm soát dòng tiền cá nhân rõ ràng và chủ động hơn mỗi ngày.
            </h1>
            <p className="mt-3 text-slate-300 text-sm leading-relaxed">
              Ghi chép thu chi, đặt hạn mức ngân sách và theo dõi mục tiêu tiết kiệm trên cùng một nền tảng.
            </p>
          </div>

          <div className="space-y-4">
            {HIGHLIGHTS.map((item) => {
              const Icon = item.icon;
              return (
                <div
                  key={item.title}
                  className="flex items-start gap-3.5 rounded-2xl bg-white/5 border border-white/10 p-4 backdrop-blur-sm"
                >
                  <div className="h-9 w-9 rounded-xl bg-emerald-500/20 text-emerald-300 flex items-center justify-center shrink-0">
                    <Icon className="h-5 w-5" />
                  </div>
                  <div>
                    <h3 className="text-sm font-semibold text-white">{item.title}</h3>
                    <p className="text-xs text-slate-300 mt-0.5 leading-relaxed">
                      {item.description}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        </div>

        <p className="relative z-10 text-xs text-slate-400">
          © {new Date().getFullYear()} FinFlow. Hệ thống quản lý tài chính cá nhân.
        </p>
      </div>

      <main className="lg:col-span-7 xl:col-span-6 flex items-center justify-center p-6 sm:p-10">
        <div className="w-full max-w-md">
          <div className="lg:hidden flex items-center gap-2.5 mb-8">
            <div className="h-9 w-9 rounded-xl bg-emerald-600 flex items-center justify-center text-white">
              <SparklesIcon className="h-5 w-5" />
            </div>
            <span className="text-lg font-bold text-slate-900">FinFlow</span>
          </div>
          <Outlet />
        </div>
      </main>

      <ToastContainer />
    </div>
  );
}

