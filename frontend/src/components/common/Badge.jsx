import React from 'react';

export default function Badge({ children, variant = 'default', size = 'md', className = '' }) {
  const variants = {
    default: 'bg-slate-800 text-slate-300 border-slate-700',
    primary: 'bg-indigo-950/80 text-indigo-300 border-indigo-700/60',
    success: 'bg-emerald-950/80 text-emerald-300 border-emerald-700/60',
    warning: 'bg-amber-950/80 text-amber-300 border-amber-700/60',
    danger: 'bg-rose-950/80 text-rose-300 border-rose-700/60',
    info: 'bg-cyan-950/80 text-cyan-300 border-cyan-700/60',
    purple: 'bg-purple-950/80 text-purple-300 border-purple-700/60',
  };

  const sizes = {
    sm: 'text-xs px-2 py-0.5',
    md: 'text-xs px-2.5 py-1',
    lg: 'text-sm px-3 py-1.5',
  };

  // Helper mapping for common system status strings
  let appliedVariant = variant;
  if (typeof children === 'string') {
    const text = children.toUpperCase();
    if (['APPROVED', 'REGISTERED', 'PRESENT', 'ATTENDED', 'PUBLISHED', 'ACTIVE'].includes(text)) {
      appliedVariant = 'success';
    } else if (['PENDING', 'REGISTRATION CLOSED', 'DRAFT'].includes(text)) {
      appliedVariant = 'warning';
    } else if (['REJECTED', 'SUSPENDED', 'CANCELLED', 'ABSENT', 'FULL', 'NOT ELIGIBLE'].includes(text)) {
      appliedVariant = 'danger';
    } else if (['COMPLETED'].includes(text)) {
      appliedVariant = 'purple';
    } else if (['REGISTER NOW', 'ONGOING'].includes(text)) {
      appliedVariant = 'primary';
    }
  }

  return (
    <span
      className={`inline-flex items-center font-medium rounded-full border ${variants[appliedVariant] || variants.default} ${sizes[size]} ${className}`}
    >
      {children}
    </span>
  );
}
