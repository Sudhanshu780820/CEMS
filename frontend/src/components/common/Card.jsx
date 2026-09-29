import React from 'react';

export default function Card({ children, className = '', hover = false, ...props }) {
  return (
    <div
      className={`bg-slate-900/80 border border-slate-800/80 rounded-2xl p-6 backdrop-blur-sm shadow-xl shadow-black/20 ${
        hover ? 'transition-all duration-200 hover:border-slate-700 hover:shadow-indigo-500/5' : ''
      } ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}
