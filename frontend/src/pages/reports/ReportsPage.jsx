import { useCallback, useEffect, useState } from 'react';
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
import { extractErrorMessage, formatCurrency } from '../../utils/formatters';

export default function ReportsPage() {
  const dispatch = useDispatch();
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
          message: extractErrorMessage(err, 'Không thể tải dữ liệu báo cáo.'),
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
          <h2 className="text-sm font-bold text-slate-900">Bộ lọc kỳ báo cáo</h2>
          <p className="text-xs text-slate-500">
            Phân tích dòng tiền, cơ cấu thu chi và biến động hàng ngày trong tháng {month}/{year}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <select
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none"
          >
            {Array.from({ length: 12 }, (_, idx) => idx + 1).map((m) => (
              <option key={m} value={m}>
                Tháng {m}
              </option>
            ))}
          </select>

          <select
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none"
          >
            {years.map((y) => (
              <option key={y} value={y}>
                Năm {y}
              </option>
            ))}
          </select>

          <select
            value={trendMonths}
            onChange={(e) => setTrendMonths(Number(e.target.value))}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none"
          >
            <option value={3}>Xu hướng 3 tháng</option>
            <option value={6}>Xu hướng 6 tháng</option>
            <option value={12}>Xu hướng 12 tháng</option>
          </select>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner label="Đang phân tích dữ liệu báo cáo..." />
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500">Tổng thu nhập kỳ</p>
              <p className="mt-2 text-xl font-bold text-emerald-600 tabular-nums">
                {formatCurrency(totalIncome, currency)}
              </p>
            </div>
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500">Tổng chi tiêu kỳ</p>
              <p className="mt-2 text-xl font-bold text-rose-600 tabular-nums">
                {formatCurrency(totalExpense, currency)}
              </p>
            </div>
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500">Chênh lệch thu chi</p>
              <p
                className={`mt-2 text-xl font-bold tabular-nums ${
                  netCashflow >= 0 ? 'text-slate-900' : 'text-rose-600'
                }`}
              >
                {formatCurrency(netCashflow, currency)}
              </p>
            </div>
            <div className="card-surface p-5">
              <p className="text-xs font-medium text-slate-500">Tỷ lệ tiết kiệm kỳ</p>
              <p className="mt-2 text-xl font-bold text-slate-900 tabular-nums">
                {savingRatePct.toFixed(1)}%
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            <div className="lg:col-span-7 card-surface p-5">
              <h3 className="text-sm font-bold text-slate-900 mb-1">
                Xu hướng dòng tiền ({trendMonths} tháng)
              </h3>
              <p className="text-xs text-slate-500 mb-4">
                Biểu đồ cột so sánh thu nhập và chi tiêu qua các tháng
              </p>
              <CashflowTrendChart data={trendData} currency={currency} height={310} />
            </div>

            <div className="lg:col-span-5 card-surface p-5">
              <div className="flex items-center justify-between gap-2 mb-4">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Cơ cấu theo danh mục</h3>
                  <p className="text-xs text-slate-500">
                    Tổng: {formatCurrency(breakdownData?.totalAmount ?? 0, currency)}
                  </p>
                </div>
                <div className="flex rounded-xl bg-slate-100 p-1">
                  {[
                    { value: 'EXPENSE', label: 'Chi' },
                    { value: 'INCOME', label: 'Thu' },
                  ].map((tab) => (
                    <button
                      key={tab.value}
                      type="button"
                      onClick={() => setBreakdownType(tab.value)}
                      className={`px-3 py-1 rounded-lg text-xs font-semibold transition-colors ${
                        breakdownType === tab.value
                          ? 'bg-white text-slate-900 shadow-sm'
                          : 'text-slate-500'
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
              <h3 className="text-sm font-bold text-slate-900 mb-1">
                Biến động thu chi từng ngày (Tháng {month}/{year})
              </h3>
              <p className="text-xs text-slate-500 mb-4">
                Theo dõi các ngày phát sinh chi tiêu hoặc thu nhập cao đột biến
              </p>
              <DailyAreaChart data={dailyPoints} currency={currency} height={290} />
            </div>

            <div className="lg:col-span-5 card-surface p-5">
              <h3 className="text-sm font-bold text-slate-900 mb-4">
                Chi tiết xếp hạng danh mục ({breakdownType === 'EXPENSE' ? 'Chi tiêu' : 'Thu nhập'})
              </h3>
              {!categoryItems.length ? (
                <p className="text-xs text-slate-500 py-8 text-center">
                  Không có dữ liệu danh mục trong kỳ này.
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
                          <span className="font-semibold text-slate-800 truncate">
                            {item.categoryName}
                          </span>
                        </div>
                        <span className="font-bold text-slate-900 tabular-nums shrink-0">
                          {formatCurrency(item.amount ?? item.totalAmount, currency)} (
                          {Number(item.percentage ?? 0).toFixed(1)}%)
                        </span>
                      </div>
                      <div className="h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
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
