import { Outlet } from 'react-router-dom';
import { ChartBarIcon, ShieldCheckIcon, WalletIcon } from '@heroicons/react/24/outline';
import ToastContainer from '../components/feedback/ToastContainer';
import BrandLogo from '../components/common/BrandLogo';
import { LanguageSwitcher, useTranslation } from '../i18n/I18nContext';
import { ThemeSwitcher } from '../theme/ThemeContext';

export default function AuthLayout() {
  const { t } = useTranslation();

  const highlights = [
    {
      icon: WalletIcon,
      title: t('auth.highlight1Title'),
      description: t('auth.highlight1Desc'),
    },
    {
      icon: ChartBarIcon,
      title: t('auth.highlight2Title'),
      description: t('auth.highlight2Desc'),
    },
    {
      icon: ShieldCheckIcon,
      title: t('auth.highlight3Title'),
      description: t('auth.highlight3Desc'),
    },
  ];

  return (
    <div className="min-h-screen grid lg:grid-cols-12 bg-slate-50 dark:bg-black text-slate-900 dark:text-slate-100 transition-colors duration-200">
      <div className="hidden lg:flex lg:col-span-5 xl:col-span-6 bg-gradient-to-br from-slate-900 via-emerald-950 to-black text-white p-10 xl:p-14 flex-col justify-between relative overflow-hidden">
        <div className="relative z-10 flex items-center gap-3">
          <BrandLogo className="h-10 w-10" />
          <div>
            <span className="text-xl font-bold tracking-tight">DangCap</span>
            <p className="text-xs text-emerald-300">Personal Finance Manager</p>
          </div>
        </div>

        <div className="relative z-10 max-w-lg space-y-8 my-auto py-8">
          <div>
            <h1 className="text-3xl xl:text-4xl font-bold leading-tight tracking-tight">
              {t('auth.heroTitle')}
            </h1>
            <p className="mt-3 text-slate-300 text-sm leading-relaxed">{t('auth.heroSubtitle')}</p>
          </div>

          <div className="space-y-4">
            {highlights.map((item) => {
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
          {t('auth.copyright', { year: new Date().getFullYear() })}
        </p>
      </div>

      <main className="lg:col-span-7 xl:col-span-6 flex flex-col justify-between p-6 sm:p-10">
        <div className="flex items-center justify-between lg:justify-end gap-2">
          <div className="lg:hidden flex items-center gap-2.5">
            <BrandLogo className="h-9 w-9" />
            <span className="text-lg font-bold text-slate-900 dark:text-white">DangCap</span>
          </div>
          <div className="flex items-center gap-2">
            <ThemeSwitcher showLabels={false} />
            <LanguageSwitcher />
          </div>
        </div>

        <div className="w-full max-w-md mx-auto my-auto py-6">
          <Outlet />
        </div>

        <div className="h-4" />
      </main>

      <ToastContainer />
    </div>
  );
}
