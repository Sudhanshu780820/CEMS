import React from 'react';
import { Loader2 } from 'lucide-react';

export default function LoadingSpinner({ message = 'Loading...', size = 'md' }) {
  const sizes = {
    sm: 'w-5 h-5',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
  };

  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 gap-3">
      <Loader2 className={`${sizes[size]} animate-spin text-indigo-500`} />
      {message && <p className="text-sm font-medium text-slate-400">{message}</p>}
    </div>
  );
}
