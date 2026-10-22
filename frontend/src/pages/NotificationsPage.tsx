import { useCallback, useEffect, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import type { Notification } from '../types';

export function NotificationsPage() {
  const [items, setItems] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [marking, setMarking] = useState<Set<number>>(new Set());
  const [markingAll, setMarkingAll] = useState(false);
  const requestId = useRef(0);

  const load = useCallback((showLoading = false) => {
    const currentRequest = ++requestId.current;
    if (showLoading) setLoading(true);
    setError('');
    return api.notifications()
      .then((page) => {
        if (currentRequest === requestId.current) setItems(page.items);
      })
      .catch((requestError: Error) => {
        if (currentRequest === requestId.current) setError(requestError.message);
      })
      .finally(() => {
        if (currentRequest === requestId.current) setLoading(false);
      });
  }, []);

  useEffect(() => {
    void load(true);
    const timer = setInterval(() => { void load(); }, 30_000);
    return () => {
      clearInterval(timer);
      requestId.current += 1;
    };
  }, [load]);

  const markRead = async (id: number) => {
    if (marking.has(id)) return;
    setMarking((current) => new Set(current).add(id));
    setError('');
    try {
      await api.readNotification(id);
      setItems((current) => current.map((item) => item.id === id ? { ...item, read: true } : item));
      window.dispatchEvent(new Event('notifications:changed'));
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setMarking((current) => {
        const next = new Set(current);
        next.delete(id);
        return next;
      });
    }
  };

  const markAllRead = async () => {
    if (markingAll) return;
    setMarkingAll(true);
    setError('');
    try {
      await api.readAll();
      setItems((current) => current.map((item) => ({ ...item, read: true })));
      window.dispatchEvent(new Event('notifications:changed'));
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setMarkingAll(false);
    }
  };

  return (
    <div className="form-page">
      <h1>通知</h1>
      <button disabled={markingAll || !items.some((item) => !item.read)} onClick={() => void markAllRead()}>
        {markingAll ? '处理中…' : '全部标记为已读'}
      </button>
      {loading && <p className="muted" role="status">正在加载通知…</p>}
      {error && (
        <div className="state-message error" role="alert">
          <span>{error}</span>
          <button onClick={() => void load(true)}>重试</button>
        </div>
      )}
      <ul className="plain notification-list" aria-busy={loading}>
        {items.map((n) => (
          <li key={n.id} className={n.read ? 'muted' : ''}>
            <div>
              <Link to={n.link.replace(/^\//, '/')}>{n.title}</Link>
              <span className="muted"> · {new Date(n.createdAt).toLocaleString('zh-CN')}</span>
            </div>
            {!n.read && (
              <button disabled={marking.has(n.id)} onClick={() => void markRead(n.id)}>
                {marking.has(n.id) ? '处理中…' : '标为已读'}
              </button>
            )}
          </li>
        ))}
        {!loading && !error && items.length === 0 && <li className="muted">暂时没有通知。</li>}
      </ul>
    </div>
  );
}
