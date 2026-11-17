import { Link } from 'react-router-dom';
import { HomeIcon, QuestionMarkCircleIcon } from '@heroicons/react/24/outline';
import { useTranslation } from '../../i18n/I18nContext';

export default function NotFoundPage() {
  const { t } = useTranslation();

  return (
    <div className="min-h-[70vh] flex items-center justify-center p-6">
      <div className="max-w-md w-full text-center space-y-5">
        <div className="mx-auto h-16 w-16 rounded-2xl bg-slate-100 dark:bg-zinc-900 text-slate-400 dark:text-zinc-500 flex items-center justify-center">
          <QuestionMarkCircleIcon className="h-10 w-10" />
        </div>

        <div className="space-y-2">
          <h1 className="text-3xl font-extrabold text-slate-900 dark:text-white">404</h1>
          <h2 className="text-lg font-bold text-slate-800 dark:text-slate-100">
            {t('error.notFoundTitle')}
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 max-w-sm mx-auto leading-relaxed">
            {t('error.notFoundDesc')}
          </p>
        </div>

        <div className="pt-2">
          <Link
            to="/dashboard"
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold transition-colors shadow-sm"
          >
            <HomeIcon className="h-4 w-4" />
            {t('error.backHome')}
          </Link>
        </div>
      </div>
    </div>
  );
}
