import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  ArrowDownLeftIcon,
  ArrowTrendingDownIcon,
  ArrowTrendingUpIcon,
  ArrowUpRightIcon,
  BanknotesIcon,
  ChartPieIcon,
  PlusIcon,
  WalletIcon,
} from '@heroicons/react/24/outline';
import { reportApi } from '../../services/reportApi';
import { showToast } from '../../features/feedback/feedbackSlice';
import { CashflowTrendChart, CategoryPieChart } from '../../components/charts/FinanceCharts';
import CategoryIcon from '../../components/common/CategoryIcon';
import EmptyState from '../../components/common/EmptyState';
import LoadingSpinner from '../../components/feedback/LoadingSpinner';
import { TRANSACTION_TYPE_MAP } from '../../constants/enums';
import { extractErrorMessage, formatCurrency, formatDateTime } from '../../utils/formatters';

function ChangeBadge({ value, invertPositive = false }) {
  if (value === null || value === undefined) return null;
  const num = Number(value);
  if (!Number.isFinite(num)) return null;
  const isUp = num >= 0;
  const isGood = invertPositive ? !isUp : isUp;

  return (
    <span
      className={`inline-flex items-center gap-0.5 px-2 py-0.5 rounded-full text-[11px] font-semibold tabular-nums ${
        isGood ? 'bg-emerald-50 text-emerald-700' : 'bg-rose-50 text-rose-700'
      }`}
    >
      {isUp ? (
        <ArrowTrendingUpIcon className="h-3 w-3" />
      ) : (
        <ArrowTrendingDownIcon className="h-3 w-3" />
      )}
      {isUp ? '+' : ''}
      {num.toFixed(1)}% vs tháng trước
    </span>
  );
}

export default function DashboardPage() {
  const dispatch = useDispatch();
  const currency = useSelector((state) => state.auth.user?.currency || 'VND');

  const now = new Date();
  const [month, setMonth] = useState(now.getMonth() + 1);
  const [year, setYear] = useState(now.getFullYear());
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState(null);
  const [trendPoints, setTrendPoints] = useState([]);

  const loadDashboard = useCallback(async () => {
    setLoading(true);
    try {
      const [summaryRes, trendRes] = await Promise.all([
        reportApi.getDashboardSummary({ month, year }),
        reportApi.getCashflowTrend({ months: 6, month, year }),
      ]);
      setSummary(summaryRes.data || null);
      const tData = trendRes.data;
      const points =
        tData?.points || tData?.monthlyPoints || tData?.items || (Array.isArray(tData) ? tData : []);
      setTrendPoints(points);
    } catch (err) {
      dispatch(
        showToast({
          type: 'error',
          message: extractErrorMessage(err, 'Không thể tải dữ liệu tổng quan.'),
        })
      );
    } finally {
      setLoading(false);
    }
  }, [dispatch, month, year]);

  useEffect(() => {
    loadDashboard();
  }, [loadDashboard]);

  const years = [now.getFullYear() - 2, now.getFullYear() - 1, now.getFullYear(), now.getFullYear() + 1];
  const budgetOverview = summary?.budgetOverview;
  const savingOverview = summary?.savingGoalOverview;
  const recentTransactions = summary?.recentTransactions || [];
  const topExpenseCategories = summary?.topExpenseCategories || [];

  const totalGoalSaved = Number(
    savingOverview?.totalCurrentAmount ?? savingOverview?.totalSavedAmount ?? summary?.totalSavedInGoals ?? 0
  );
  const totalGoalTarget = Number(savingOverview?.totalTargetAmount ?? 0);
  const goalPct =
    totalGoalTarget > 0
      ? Math.min(100, Number(savingOverview?.overallProgressPercentage ?? (totalGoalSaved / totalGoalTarget) * 100))
      : 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-bold text-slate-900">Tình hình tài chính</h2>
          <p className="text-xs text-slate-500">
            Theo dõi số dư, thu chi và tiến độ kế hoạch trong tháng {month}/{year}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2.5">
          <select
            value={month}
            onChange={(e) => setMonth(Number(e.target.value))}
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none focus:border-emerald-500"
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
            className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-xs font-medium text-slate-700 outline-none focus:border-emerald-500"
          >
            {years.map((y) => (
              <option key={y} value={y}>
                Năm {y}
              </option>
            ))}
          </select>

          <Link
            to="/transactions"
            className="inline-flex items-center gap-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white px-3.5 py-2 text-xs font-semibold transition-colors"
          >
            <PlusIcon className="h-4 w-4" />
            Ghi giao dịch
          </Link>
        </div>
      </div>

      {loading ? (
        <LoadingSpinner label="Đang tổng hợp báo cáo tài chính..." />
      ) : (
        <>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4">
            <div className="card-surface p-5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-500">Tổng tài sản ròng</span>
                <div className="h-8 w-8 rounded-xl bg-slate-100 text-slate-700 flex items-center justify-center">
                  <WalletIcon className="h-4 w-4" />
                </div>
              </div>
              <p className="mt-2.5 text-2xl font-bold text-slate-900 tabular-nums">
                {formatCurrency(summary?.netWorth, currency)}
              </p>
              <p className="mt-2 text-[11px] text-slate-500 tabular-nums">
                Ví: {formatCurrency(summary?.totalActiveAccountBalance, currency)} • Tiết kiệm:{' '}
                {formatCurrency(summary?.totalSavedInGoals, currency)}
              </p>
            </div>

            <div className="card-surface p-5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-500">Thu nhập tháng {month}</span>
                <div className="h-8 w-8 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center">
                  <ArrowDownLeftIcon className="h-4 w-4" />
                </div>
              </div>
              <p className="mt-2.5 text-2xl font-bold text-emerald-600 tabular-nums">
                {formatCurrency(summary?.monthlyIncome, currency)}
              </p>
              <div className="mt-2">
                <ChangeBadge value={summary?.incomeChangePercentage} />
              </div>
            </div>

            <div className="card-surface p-5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-500">Chi tiêu tháng {month}</span>
                <div className="h-8 w-8 rounded-xl bg-rose-50 text-rose-600 flex items-center justify-center">
                  <ArrowUpRightIcon className="h-4 w-4" />
                </div>
              </div>
              <p className="mt-2.5 text-2xl font-bold text-rose-600 tabular-nums">
                {formatCurrency(summary?.monthlyExpense, currency)}
              </p>
              <div className="mt-2">
                <ChangeBadge value={summary?.expenseChangePercentage} invertPositive />
              </div>
            </div>

            <div className="card-surface p-5">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-500">Dòng tiền ròng</span>
                <div className="h-8 w-8 rounded-xl bg-sky-50 text-sky-600 flex items-center justify-center">
                  <BanknotesIcon className="h-4 w-4" />
                </div>
              </div>
              <p
                className={`mt-2.5 text-2xl font-bold tabular-nums ${
                  Number(summary?.netCashflow ?? 0) >= 0 ? 'text-slate-900' : 'text-rose-600'
                }`}
              >
                {formatCurrency(summary?.netCashflow, currency)}
              </p>
              <p className="mt-2 text-[11px] text-slate-500 tabular-nums">
                Tỷ lệ tiết kiệm:{' '}
                <strong className="text-slate-700">
                  {Number(summary?.savingRatePercentage ?? 0).toFixed(1)}%
                </strong>
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            <div className="lg:col-span-7 card-surface p-5">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Xu hướng thu chi 6 tháng</h3>
                  <p className="text-xs text-slate-500">So sánh tổng thu nhập và chi tiêu hàng tháng</p>
                </div>
                <Link to="/reports" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
                  Chi tiết
                </Link>
              </div>
              <CashflowTrendChart data={trendPoints} currency={currency} height={290} />
            </div>

            <div className="lg:col-span-5 card-surface p-5">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Top danh mục chi tiêu</h3>
                  <p className="text-xs text-slate-500">Phân bổ chi tiêu trong tháng {month}/{year}</p>
                </div>
                <Link to="/reports" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
                  Phân tích
                </Link>
              </div>
              <CategoryPieChart data={topExpenseCategories} currency={currency} height={220} />
            </div>
          </div>

          <div className="grid grid-cols-1 lg:grid-cols-12 gap-5">
            <div className="lg:col-span-5 space-y-5">
              <div className="card-surface p-5">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <ChartPieIcon className="h-4 w-4 text-emerald-600" />
                    <h3 className="text-sm font-bold text-slate-900">Ngân sách tháng {month}</h3>
                  </div>
                  <Link to="/budgets" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
                    Quản lý
                  </Link>
                </div>

                {budgetOverview && Number(budgetOverview.totalLimitAmount ?? 0) > 0 ? (
                  <div className="space-y-3">
                    <div className="flex justify-between text-xs text-slate-600 tabular-nums">
                      <span>
                        Đã dùng:{' '}
                        <strong className="text-slate-900">
                          {formatCurrency(budgetOverview.totalSpentAmount, currency)}
                        </strong>
                      </span>
                      <span>
                        Hạn mức:{' '}
                        <strong className="text-slate-900">
                          {formatCurrency(budgetOverview.totalLimitAmount, currency)}
                        </strong>
                      </span>
                    </div>
                    <div className="h-2.5 w-full rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className={`h-full rounded-full transition-all ${
                          Number(budgetOverview.overallUsagePercentage ?? 0) > 100
                            ? 'bg-rose-600'
                            : Number(budgetOverview.overallUsagePercentage ?? 0) >= 80
                              ? 'bg-amber-500'
                              : 'bg-emerald-500'
                        }`}
                        style={{
                          width: `${Math.min(100, Number(budgetOverview.overallUsagePercentage ?? 0))}%`,
                        }}
                      />
                    </div>
                    <div className="flex flex-wrap gap-2 text-[11px]">
                      <span className="px-2 py-0.5 rounded-full bg-amber-50 text-amber-700 font-medium">
                        Cảnh báo: {budgetOverview.warningCount ?? 0}
                      </span>
                      <span className="px-2 py-0.5 rounded-full bg-orange-50 text-orange-700 font-medium">
                        Chạm mức: {budgetOverview.reachedCount ?? 0}
                      </span>
                      <span className="px-2 py-0.5 rounded-full bg-rose-50 text-rose-700 font-medium">
                        Vượt mức: {budgetOverview.exceededCount ?? 0}
                      </span>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs text-slate-500 py-2">
                    Chưa thiết lập ngân sách cho tháng {month}/{year}.
                  </p>
                )}
              </div>

              <div className="card-surface p-5">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-2">
                    <BanknotesIcon className="h-4 w-4 text-sky-600" />
                    <h3 className="text-sm font-bold text-slate-900">Mục tiêu tiết kiệm</h3>
                  </div>
                  <Link to="/saving-goals" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
                    Xem tất cả
                  </Link>
                </div>

                {totalGoalTarget > 0 ? (
                  <div className="space-y-3">
                    <div className="flex justify-between text-xs text-slate-600 tabular-nums">
                      <span>
                        Đã tích lũy:{' '}
                        <strong className="text-slate-900">
                          {formatCurrency(totalGoalSaved, currency)}
                        </strong>
                      </span>
                      <span>
                        Mục tiêu:{' '}
                        <strong className="text-slate-900">
                          {formatCurrency(totalGoalTarget, currency)}
                        </strong>
                      </span>
                    </div>
                    <div className="h-2.5 w-full rounded-full bg-slate-100 overflow-hidden">
                      <div
                        className="h-full rounded-full bg-sky-500 transition-all"
                        style={{ width: `${goalPct}%` }}
                      />
                    </div>
                    <p className="text-[11px] text-slate-500 tabular-nums">
                      Hoàn thành chung: <strong className="text-slate-700">{goalPct.toFixed(1)}%</strong>
                      {savingOverview?.inProgressCount !== undefined &&
                        ` • ${savingOverview.inProgressCount} mục tiêu đang thực hiện`}
                    </p>
                  </div>
                ) : (
                  <p className="text-xs text-slate-500 py-2">
                    Bạn chưa tạo mục tiêu tiết kiệm nào.
                  </p>
                )}
              </div>
            </div>

            <div className="lg:col-span-7 card-surface p-5">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-sm font-bold text-slate-900">Giao dịch gần đây</h3>
                  <p className="text-xs text-slate-500">Các giao dịch phát sinh mới nhất</p>
                </div>
                <Link to="/transactions" className="text-xs font-semibold text-emerald-600 hover:text-emerald-700">
                  Xem toàn bộ
                </Link>
              </div>

              {!recentTransactions.length ? (
                <EmptyState
                  title="Chưa có giao dịch gần đây"
                  description="Ghi nhận giao dịch đầu tiên để theo dõi dòng tiền của bạn."
                />
              ) : (
                <div className="divide-y divide-slate-100">
                  {recentTransactions.map((tx) => {
                    const typeInfo = TRANSACTION_TYPE_MAP[tx.transactionType] || {};
                    const isExpense = tx.transactionType === 'EXPENSE';
                    const isIncome = tx.transactionType === 'INCOME';
                    return (
                      <div key={tx.id} className="py-3 flex items-center justify-between gap-3">
                        <div className="flex items-center gap-3 min-w-0">
                          <div
                            className="h-9 w-9 rounded-xl flex items-center justify-center shrink-0 border border-slate-200/70"
                            style={{
                              backgroundColor: tx.categoryColor ? `${tx.categoryColor}15` : '#F1F5F9',
                            }}
                          >
                            <CategoryIcon
                              icon={tx.categoryIcon}
                              color={tx.categoryColor || '#0EA5E9'}
                              className="h-4 w-4"
                            />
                          </div>
                          <div className="min-w-0">
                            <p className="text-xs font-semibold text-slate-900 truncate">
                              {tx.description || tx.categoryName || 'Chuyển khoản nội bộ'}
                            </p>
                            <p className="text-[11px] text-slate-500 truncate">
                              {tx.accountName}
                              {tx.toAccountName ? ` → ${tx.toAccountName}` : ''} •{' '}
                              {formatDateTime(tx.transactionDate)}
                            </p>
                          </div>
                        </div>

                        <div className="text-right shrink-0">
                          <p
                            className={`text-xs font-bold tabular-nums ${
                              isIncome
                                ? 'text-emerald-600'
                                : isExpense
                                  ? 'text-rose-600'
                                  : 'text-sky-600'
                            }`}
                          >
                            {isIncome ? '+' : isExpense ? '-' : ''}
                            {formatCurrency(tx.amount, currency)}
                          </p>
                          <span
                            className={`inline-block mt-0.5 px-1.5 py-0.5 rounded text-[10px] font-medium border ${
                              typeInfo.badgeClass || 'bg-slate-50 text-slate-600 border-slate-200'
                            }`}
                          >
                            {typeInfo.label || tx.transactionType}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </>
      )}
    </div>
  );
}

