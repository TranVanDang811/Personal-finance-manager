import { NavLink } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  ArrowPathIcon,
  ArrowsRightLeftIcon,
  BanknotesIcon,
  BellIcon,
  ChartBarSquareIcon,
  ChartPieIcon,
  CreditCardIcon,
  HomeIcon,
  SparklesIcon,
  TagIcon,
  UserCircleIcon,
  XMarkIcon,
} from '@heroicons/react/24/outline';

const NAV_SECTIONS = [
  {
    title: 'Tổng quan',
    items: [
      { to: '/dashboard', label: 'Trang chủ', icon: HomeIcon },
      { to: '/reports', label: 'Báo cáo tài chính', icon: ChartBarSquareIcon },
    ],
  },
  {
    title: 'Dòng tiền & Tài khoản',
    items: [
      { to: '/transactions', label: 'Giao dịch', icon: ArrowsRightLeftIcon },
      { to: '/accounts', label: 'Tài khoản & Ví', icon: CreditCardIcon },
      { to: '/categories', label: 'Danh mục', icon: TagIcon },
      { to: '/recurring', label: 'Giao dịch định kỳ', icon: ArrowPathIcon },
    ],
  },
  {
    title: 'Kế hoạch & Tiết kiệm',
    items: [
      { to: '/budgets', label: 'Ngân sách tháng', icon: ChartPieIcon },
      { to: '/saving-goals', label: 'Mục tiêu tiết kiệm', icon: BanknotesIcon },
    ],
  },
  {
    title: 'Hệ thống',
    items: [
      { to: '/notifications', label: 'Thông báo', icon: BellIcon, showBadge: true },
      { to: '/profile', label: 'Hồ sơ cá nhân', icon: UserCircleIcon },
    ],
  },
];

function SidebarContent({ onNavigate }) {
  const unreadCount = useSelector((state) => state.notifications.unreadCount);
  const user = useSelector((state) => state.auth.user);

  return (
    <div className="flex flex-col h-full bg-slate-900 text-slate-300">
      <div className="flex items-center justify-between h-16 px-5 border-b border-slate-800/80">
        <div className="flex items-center gap-2.5">
          <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center text-white shadow-sm">
            <SparklesIcon className="h-5 w-5" />
          </div>
          <div>
            <span className="text-base font-bold tracking-tight text-white block leading-none">
              FinFlow
            </span>
            <span className="text-[11px] text-emerald-400 font-medium">Personal Finance</span>
          </div>
        </div>
        {onNavigate && (
          <button
            type="button"
            onClick={onNavigate}
            className="lg:hidden p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800"
            aria-label="Đóng menu"
          >
            <XMarkIcon className="h-5 w-5" />
          </button>
        )}
      </div>

      <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-6">
        {NAV_SECTIONS.map((section) => (
          <div key={section.title}>
            <p className="px-3 mb-2 text-[11px] font-semibold uppercase tracking-wider text-slate-500">
              {section.title}
            </p>
            <div className="space-y-1">
              {section.items.map((item) => {
                const Icon = item.icon;
                return (
                  <NavLink
                    key={item.to}
                    to={item.to}
                    onClick={onNavigate}
                    className={({ isActive }) =>
                      `flex items-center justify-between gap-2.5 px-3 py-2.5 rounded-xl text-sm font-medium transition-colors ${
                        isActive
                          ? 'bg-emerald-600/15 text-emerald-400 border border-emerald-500/20'
                          : 'text-slate-300 hover:text-white hover:bg-slate-800/70'
                      }`
                    }
                  >
                    <span className="flex items-center gap-3 min-w-0">
                      <Icon className="h-5 w-5 shrink-0" />
                      <span className="truncate">{item.label}</span>
                    </span>
                    {item.showBadge && unreadCount > 0 && (
                      <span className="inline-flex items-center justify-center min-w-[20px] h-5 px-1.5 rounded-full bg-rose-500 text-white text-[11px] font-semibold tabular-nums">
                        {unreadCount > 99 ? '99+' : unreadCount}
                      </span>
                    )}
                  </NavLink>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      <div className="p-4 border-t border-slate-800/80">
        <div className="rounded-xl bg-slate-800/70 p-3 flex items-center gap-3">
          <div className="h-9 w-9 rounded-full bg-emerald-600/20 text-emerald-400 font-semibold text-sm flex items-center justify-center shrink-0 overflow-hidden">
            {user?.avatarUrl ? (
              <img src={user.avatarUrl} alt={user.fullName} className="h-full w-full object-cover" />
            ) : (
              (user?.fullName?.[0] || 'U').toUpperCase()
            )}
          </div>
          <div className="min-w-0">
            <p className="text-xs font-semibold text-white truncate">
              {user?.fullName || 'Người dùng'}
            </p>
            <p className="text-[11px] text-slate-400 truncate">
              Tiền tệ: <span className="text-emerald-400 font-medium">{user?.currency || 'VND'}</span>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default function Sidebar({ mobileOpen, onCloseMobile }) {
  return (
    <>
      <aside className="hidden lg:fixed lg:inset-y-0 lg:left-0 lg:z-30 lg:w-64 lg:block border-r border-slate-800">
        <SidebarContent />
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div
            className="fixed inset-0 bg-slate-900/60 backdrop-blur-[1px]"
            onClick={onCloseMobile}
          />
          <div className="relative w-72 max-w-[85vw] h-full z-10 shadow-2xl">
            <SidebarContent onNavigate={onCloseMobile} />
          </div>
        </div>
      )}
    </>
  );
}

