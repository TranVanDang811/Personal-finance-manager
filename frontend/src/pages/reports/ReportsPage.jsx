import { useCallback, useEffect, useRef, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { reportApi } from '../../services/reportApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import {
  CashflowTrendChart,
  CategoryPieChart,
  DailyAreaChart,
} from '../../components/charts/FinanceCharts';
import CategoryIcon from '../../components/common/CategoryIcon';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import { useTranslation } from '../../i18n/I18nContext';
import { extractErrorMessage, formatCurrency } from '../../utils/formatters';

export default function ReportsPage() {
  const dispatch = useDispatch();
  const { t } = useTranslation();
  const tRef = useRef(t);
  tRef.current = t;

  const currency = useSelector((state) => state.auth.user?.currency || 'VND');

  const now = new Date();
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [year, setYear] = useState(now.getFullYear());
  const [trendMonths, setTrendMonths] = useState(6);
  const [breakdownType, setBreakdownType] = useState('EXPENSE');

  const [loading, setLoading] = useState(true);
  const [trendData, setTrendData] = useState([]);
  const [breakdownData, setBreakdownData] = useState(null);
  const [periodSummary, setPeriodSummary] = useState(null);

  const loadReports = useCallback(async () => {
    setLoading(true);
    try {
      const [trendRes, breakdownRes, periodRes] = await Promise.all([
        reportApi.getCashflowTrend({ months: trendMonths, month, year }),
        reportApi.getCategoryBreakdown({ type: breakdownType, month, year }),
        reportApi.getPeriodSummary({ month, year }),
      ]);

      const tData = trendRes.data;
      setTrendData(
        tData?.points || tData?.monthlyPoints || tData?.items || (Array.isArray(tData) ? tData : [])
      );
      setBreakdownData(breakdownRes.data || null);
      setPeriodSummary(periodRes.data || null);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, tRef.current('reports.loadError')),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch, month, year, trendMonths, breakdownType]);

  useEffect(() => {
    loadReports();
  }, [loadReports]);

  const years = [now.getFullYear() - 2, now.getFullYear() - 1, now.getFullYear(), now.getFullYear() + 1];
  const categoryItems = breakdownData?.items || breakdownData?.categories || [];
  const dailyPoints = periodSummary?.dailySeries || periodSummary?.dailyPoints || [];
  const totalIncome = Number(periodSummary?.totalIncome ?? 0);
  const totalExpense = Number(periodSummary?.totalExpense ?? 0);
  const netCashflow = Number(periodSummary?.netCashflow ?? totalIncome - totalExpense);
  const savingRatePct = Number(periodSummary?.savingRatePercentage ?? 0);

  return (
    <div className="space-y-6">
      <div className="card-surface p-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-bold text-slate-900 dark:text-white">{t('reports.filterTitle')}</h2>
          <p className="text-xs text-slate-500 dark:text-slate-400">
            {t('reports.filterSubtitle', { month, year })}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <select
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs font-medium text-slate-700 dark:text-slate-200 outline-none"
          >
            {Array.from({ length: 12 }, (_, idx) => idx + 1).map((m) => (
              <option key={m} value={m}>
                {t('dashboard.monthOption', { m })}
              </option>
            ))}
          </select>

          <select
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs font-medium text-slate-700 dark:text-slate-200 outline-none"
          >
            {years.map((y) => (
              <option key={y} value={y}>
                {t('dashboard.yearOption', { y })}
              </option>
            ))}
          </select>

          <select
            value={trendMonths}
            onChange={(e) => setTrendMonths(Number(e.target.value))}
            className="rounded-xl border border-slate-200 dark:border-zinc-800 bg-white dark:bg-zinc-900 px-3 py-2 text-xs font-medium text-slate-700 dark:text-slate-200 outline-none"
          >
            <option value={3}>{t('reports.trendMonthsOption', { count: 3 })}</option>
            <option value={6}>{t('reports.trendMonthsOption', { count: 6 })}</option>
            <option value={12}>{t('reports.trendMonthsOption', { count: 12 })}</option>
          </select>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner label={t('reports.loading')} />
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('reports.periodIncome')}</p>
              <p className="mt-2 text-xl font-bold text-emerald-600 dark:text-emerald-400 tabular-nums">
                {formatCurrency(totalIncome, currency)}
              </p>
            </div>
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('reports.periodExpense')}</p>
              <p className="mt-2 text-xl font-bold text-rose-600 dark:text-rose-400 tabular-nums">
                {formatCurrency(totalExpense, currency)}
              </p>
            </div>
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('reports.netDiff')}</p>
              <p
                className={`mt-2 text-xl font-bold tabular-nums ${
                  netCashflow >= 0 ? 'text-slate-900 dark:text-white' : 'text-rose-600 dark:text-rose-400'
                }`}
              >
                {formatCurrency(netCashflow, currency)}
              </p>
            </div>
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500 dark:text-slate-400">{t('reports.savingRate')}</p>
              <p className="mt-2 text-xl font-bold text-slate-900 dark:text-white tabular-nums">
                {savingRatePct.toFixed(1)}%
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            <div className="lg:col-span-7 card-surface p-5">
              <h3 className="text-sm font-bold text-slate-900 dark:text-white mb-1">
                {t('reports.trendTitle', { months: trendMonths })}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">{t('reports.trendSubtitle')}</p>
              <CashflowTrendChart data={trendData} currency={currency} height={310} />
            </div>

            <div className="lg:col-span-5 card-surface p-5">
              <div className="flex items-center justify-between gap-2 mb-4">
                <div>
                  <h3 className="text-sm font-bold text-slate-900 dark:text-white">{t('reports.categoryTitle')}</h3>
                  <p className="text-xs text-slate-500 dark:text-slate-400">
                    {t('reports.totalAmount', {
                      amount: formatCurrency(breakdownData?.totalAmount ?? 0, currency),
                    })}
                  </p>
                </div>
                <div className="flex rounded-xl bg-slate-100 dark:bg-zinc-900 p-1">
                  {[
                    { value: 'EXPENSE', label: t('reports.expenseShort') },
                    { value: 'INCOME', label: t('reports.incomeShort') },
                  ].map((tab) => (
                    <button
                      key={tab.value}
                      type="button"
                      onClick={() => setBreakdownType(tab.value)}
                      className={`px-3 py-1 rounded-lg text-xs font-semibold transition-colors ${
                        breakdownType === tab.value
                          ? 'bg-white dark:bg-zinc-800 text-slate-900 dark:text-white shadow-sm'
                          : 'text-slate-500 dark:text-slate-400'
                      }`}
                    >
                      {tab.label}
                    </button>
                  ))}
                </div>
              </div>
              <CategoryPieChart data={categoryItems} currency={currency} height={220} />
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            <div className="lg:col-span-7 card-surface p-5">
              <h3 className="text-sm font-bold text-slate-900 dark:text-white mb-1">
                {t('reports.dailyTitle', { month, year })}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400 mb-4">{t('reports.dailySubtitle')}</p>
              <DailyAreaChart data={dailyPoints} currency={currency} height={290} />
            </div>

            <div className="lg:col-span-5 card-surface p-5">
              <h3 className="text-sm font-bold text-slate-900 dark:text-white mb-4">
                {t('reports.rankingTitle', {
                  type:
                    breakdownType === 'EXPENSE'
                      ? t('enums.categoryTypes.EXPENSE')
                      : t('enums.categoryTypes.INCOME'),
                })}
              </h3>
              {!categoryItems.length ? (
                <p className="text-xs text-slate-500 dark:text-slate-400 py-8 text-center">
                  {t('reports.noCategoryData')}
                </p>
              ) : (
                <div className="space-y-3 max-h-72 overflow-y-auto pr-1">
                  {categoryItems.map((item, idx) => (
                    <div key={`${item.categoryId || idx}`} className="space-y-1.5">
                      <div className="flex items-center justify-between gap-2 text-xs">
                        <div className="flex items-center gap-2 min-w-0">
                          <CategoryIcon
                            icon={item.categoryIcon}
                            color={item.categoryColor || '#64748B'}
                            className="h-4 w-4 shrink-0"
                          />
                          <span className="font-semibold text-slate-800 dark:text-slate-100 truncate">
                            {item.categoryName}
                          </span>
                        </div>
                        <span className="font-bold text-slate-900 dark:text-white tabular-nums shrink-0">
                          {formatCurrency(item.amount ?? item.totalAmount, currency)} (
                          {Number(item.percentage ?? 0).toFixed(1)}%)
                        </span>
                      </div>
                      <div className="h-1.5 w-full rounded-full bg-slate-100 dark:bg-zinc-800 overflow-hidden">
                        <div
                          className="h-full rounded-full"
                          style={{
                            width: `${Math.min(100, Number(item.percentage ?? 0))}%`,
                            backgroundColor: item.categoryColor || '#10B981',
                          }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}
