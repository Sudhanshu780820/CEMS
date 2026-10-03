import React from 'react';
import { formatEventDateBlock } from '../../utils/dateUtils';

/**
 * EventDateBlock Component
 * Displays a prominent, compact calendar date block for event cards:
 * ┌────────┐
 * │  06    │
 * │  OCT   │
 * │  2026  │
 * └────────┘
 */
export default function EventDateBlock({ date, size = 'md', className = '' }) {
  const { day, month, year } = formatEventDateBlock(date);

  if (size === 'sm') {
    return (
      <div
        className={`flex flex-col items-center justify-center w-11 h-12 rounded-xl bg-gradient-to-b from-indigo-950/90 to-slate-900 border border-indigo-700/40 shadow-sm shrink-0 select-none text-center p-1 ${className}`}
      >
        <span className="text-[9px] font-bold text-indigo-400 uppercase tracking-wider leading-none">
          {month}
        </span>
        <span className="text-sm font-extrabold text-white leading-tight mt-0.5">
          {day}
        </span>
        <span className="text-[8px] font-medium text-slate-400 leading-none">
          {year}
        </span>
      </div>
    );
  }

  return (
    <div
      className={`flex flex-col items-center justify-center w-13 h-14 min-w-[3.25rem] rounded-2xl bg-gradient-to-b from-indigo-950/90 to-slate-900 border border-indigo-700/50 shadow-md shrink-0 select-none text-center p-1.5 ${className}`}
    >
      <span className="text-[10px] font-bold text-indigo-400 uppercase tracking-wider leading-none">
        {month}
      </span>
      <span className="text-base font-black text-white leading-tight mt-0.5">
        {day}
      </span>
      <span className="text-[9px] font-medium text-slate-400 leading-none">
        {year}
      </span>
    </div>
  );
}
