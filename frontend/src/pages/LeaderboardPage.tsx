import { useEffect, useRef, useState } from 'react';
import { api } from '../api';
import type { LeaderboardEntry } from '../types';

const PERIODS = [['week', '本周'], ['month', '本月'], ['term', '本学期'], ['all', '总榜']] as const;

export function LeaderboardPage() {
  const [period, setPeriod] = useState<string>('week');
  const [rows, setRows] = useState<LeaderboardEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [reloadKey, setReloadKey] = useState(0);
  const requestId = useRef(0);

  useEffect(() => {
    const currentRequest = ++requestId.current;
    setLoading(true);
    setError('');
    api.leaderboard(period)
      .then((result) => {
        if (currentRequest === requestId.current) setRows(result);
      })
      .catch((requestError: Error) => {
        if (currentRequest === requestId.current) setError(requestError.message);
      })
      .finally(() => {
        if (currentRequest === requestId.current) setLoading(false);
      });
    return () => {
      if (currentRequest === requestId.current) requestId.current += 1;
    };
  }, [period, reloadKey]);

  return (
    <div className="form-page">
      <h1>声誉排行榜</h1>
      <div className="tabs">
        {PERIODS.map(([value, label]) => (
          <button key={value} className={period === value ? 'active' : ''} onClick={() => setPeriod(value)}>
            {label}
          </button>
        ))}
      </div>
      {loading && <p className="muted" role="status">正在加载排行榜…</p>}
      {error && (
        <div className="state-message error" role="alert">
          <span>{error}</span>
          <button onClick={() => setReloadKey((value) => value + 1)}>重试</button>
        </div>
      )}
      <table className="board" aria-busy={loading}>
        <thead><tr><th>名次</th><th>用户</th><th>声誉</th></tr></thead>
        <tbody>
          {rows.map((r) => (
            <tr key={r.userId}>
              <td>{r.rank}</td>
              <td>{r.displayName}{r.verified && <span className="verified" title="已认证">✔</span>}</td>
              <td>{r.score}</td>
            </tr>
          ))}
        </tbody>
      </table>
      {!loading && !error && rows.length === 0 && <p className="muted">这个周期还没有数据。</p>}
    </div>
  );
}
