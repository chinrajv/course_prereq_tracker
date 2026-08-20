import React from 'react';
import './StatusStates.css';

export function LoadingState({ label = 'Loading…' }) {
  return (
    <div className="status-state status-state--loading" role="status" aria-live="polite">
      <span className="spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({ title, hint }) {
  return (
    <div className="status-state status-state--empty">
      <div className="status-state__mark" aria-hidden="true">◇</div>
      <h3>{title}</h3>
      {hint && <p>{hint}</p>}
    </div>
  );
}

export function ErrorState({ message, onRetry }) {
  return (
    <div className="status-state status-state--error">
      <div className="status-state__mark" aria-hidden="true">!</div>
      <h3>Something went wrong</h3>
      <p>{message || 'The database could not be reached. Please try again.'}</p>
      {onRetry && (
        <button className="btn btn--outline" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}
