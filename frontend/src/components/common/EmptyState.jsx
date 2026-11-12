import { InboxIcon } from '@heroicons/react/24/outline';

export default function EmptyState({ icon: Icon = InboxIcon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-12 px-4">
      <div className="h-12 w-12 rounded-2xl bg-slate-100 dark:bg-zinc-900 text-slate-400 dark:text-zinc-500 flex items-center justify-center mb-3">
        <Icon className="h-6 w-6" />
      </div>
      <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-100">{title}</h4>
      {description && <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

