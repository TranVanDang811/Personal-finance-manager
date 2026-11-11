import { InboxIcon } from '@heroicons/react/24/outline';

export default function EmptyState({ icon: Icon = InboxIcon, title, description, action }) {
  return (
    <div className="flex flex-col items-center justify-center text-center py-12 px-4">
      <div className="h-12 w-12 rounded-2xl bg-slate-100 text-slate-400 flex items-center justify-center mb-3">
        <Icon className="h-6 w-6" />
      </div>
      <h4 className="text-sm font-semibold text-slate-800">{title}</h4>
      {description && <p className="text-xs text-slate-500 mt-1 max-w-sm">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}

