import { useCallback, useEffect, useRef, useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { MarkdownPreview } from '../components/MarkdownPreview';
import type { QuestionDetail } from '../types';

export function QuestionDetailPage() {
  const { id } = useParams();
  const questionId = Number(id);
  const { me } = useAuth();
  const [q, setQ] = useState<QuestionDetail | null>(null);
  const [related, setRelated] = useState<{ id: number; title: string }[]>([]);
  const [draft, setDraft] = useState('');
  const [error, setError] = useState('');
  const [answerSubmitting, setAnswerSubmitting] = useState(false);

  const load = useCallback(() => {
    api.question(questionId).then(setQ).catch((e) => setError(e.message));
  }, [questionId]);

  useEffect(() => {
    load();
    api.related(questionId).then(setRelated).catch(() => undefined);
  }, [load, questionId]);

  const act = (fn: () => Promise<unknown>) => fn().then(load).catch((e) => setError(e.message));

  const submitAnswer = async () => {
    if (answerSubmitting) return;
    setAnswerSubmitting(true);
    setError('');
    try {
      await api.answer(questionId, draft);
      setDraft('');
      load();
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setAnswerSubmitting(false);
    }
  };

  if (!q) return <p className="muted">{error || '加载中…'}</p>;

  const canManage = q.permissions.canManage;

  return (
    <div className="layout">
      <article>
        <h1>{q.title}</h1>
        <p className="muted">
          {q.author.displayName} · {new Date(q.createdAt).toLocaleString('zh-CN')} · {q.viewCount} 次浏览
          {q.course && <> · 课程 {q.course.name}</>}
        </p>

        <div className="post">
          <div className="votebox">
            <button disabled={!me} onClick={() => act(() => api.vote('QUESTION', q.id, q.myVote === 1 ? 0 : 1))}>▲</button>
            <b>{q.score}</b>
            <button disabled={!me} onClick={() => act(() => api.vote('QUESTION', q.id, q.myVote === -1 ? 0 : -1))}>▼</button>
          </div>
          <div className="body">
            {/* 正文由后端渲染并经 commonmark + OWASP Sanitizer 双重净化 */}
            <div dangerouslySetInnerHTML={{ __html: q.bodyHtml }} />
            <div className="tagrow">{q.tags.map((t) => <span key={t} className="tag">{t}</span>)}</div>
            <CommentList comments={q.comments} targetType="QUESTION" targetId={q.id} onDone={load} canComment={!!me} />
            {canManage && <BountyDialog questionId={q.id} onDone={load} />}
          </div>
        </div>

        <h2>{q.answerCount} 个回答</h2>
        {q.answers.map((a) => (
          <div className={`post ${q.acceptedAnswerId === a.id ? 'is-accepted' : ''}`} key={a.id} id={`answer-${a.id}`}>
            <div className="votebox">
              <button disabled={!me} onClick={() => act(() => api.vote('ANSWER', a.id, a.myVote === 1 ? 0 : 1))}>▲</button>
              <b>{a.score}</b>
              <button disabled={!me} onClick={() => act(() => api.vote('ANSWER', a.id, a.myVote === -1 ? 0 : -1))}>▼</button>
              {q.acceptedAnswerId === a.id && <span className="check-mark" title="已采纳">✔</span>}
            </div>
            <div className="body">
              <div dangerouslySetInnerHTML={{ __html: a.bodyHtml }} />
              <p className="muted">
                {a.author.displayName}
                {a.endorsedBy && <span className="endorsed">教师认证 · {a.endorsedBy.displayName}</span>}
                · {new Date(a.createdAt).toLocaleString('zh-CN')}
              </p>
              <div className="actions">
                {q.permissions.canAccept && !q.acceptedAnswerId &&
                  <button onClick={() => act(() => api.accept(q.id, a.id))}>采纳此回答</button>}
                {canManage && !a.endorsedBy && <button onClick={() => act(() => api.endorse(a.id))}>教师认证</button>}
              </div>
              <CommentList comments={a.comments} targetType="ANSWER" targetId={a.id} onDone={load} canComment={!!me} />
            </div>
          </div>
        ))}

        {me ? (
          <form className="answer-form" onSubmit={(event) => { event.preventDefault(); void submitAnswer(); }}>
            <h3>写回答</h3>
            <textarea rows={8} value={draft} onChange={(e) => setDraft(e.target.value)}
                      placeholder="支持 Markdown，代码请使用 ``` 包裹" />
            <MarkdownPreview source={draft} />
            <button className="primary" type="submit" disabled={answerSubmitting}>
              {answerSubmitting ? '提交中…' : '提交回答'}
            </button>
          </form>
        ) : <p className="muted"><Link to="/login">登录</Link>后可以回答与投票。</p>}
        {error && <p className="error">{error}</p>}
      </article>

      <aside>
        <h3>相关问题</h3>
        <ul className="plain">
          {related.map((r) => <li key={r.id}><Link to={`/questions/${r.id}`}>{r.title}</Link></li>)}
          {related.length === 0 && <li className="muted">暂无</li>}
        </ul>
      </aside>
    </div>
  );
}

function BountyDialog({ questionId, onDone }: { questionId: number; onDone: () => void }) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const openerRef = useRef<HTMLButtonElement>(null);
  const pointsRef = useRef<HTMLInputElement>(null);
  const focusFrame = useRef<number | null>(null);
  const [points, setPoints] = useState('50');
  const [days, setDays] = useState('7');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const open = () => {
    setError('');
    dialogRef.current?.showModal();
    focusFrame.current = requestAnimationFrame(() => {
      pointsRef.current?.focus();
      focusFrame.current = null;
    });
  };

  const close = () => {
    if (focusFrame.current !== null) {
      cancelAnimationFrame(focusFrame.current);
      focusFrame.current = null;
    }
    if (dialogRef.current?.open) dialogRef.current.close();
  };

  useEffect(() => () => {
    if (focusFrame.current !== null) cancelAnimationFrame(focusFrame.current);
  }, []);

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (submitting) return;
    setSubmitting(true);
    setError('');
    try {
      await api.bounty(questionId, Number(points), Number(days));
      close();
      onDone();
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <button ref={openerRef} className="link" onClick={open}>发起悬赏</button>
      <dialog
        ref={dialogRef}
        className="modal"
        aria-labelledby="bounty-title"
        onCancel={(event) => {
          event.preventDefault();
          if (!submitting) close();
        }}
        onClose={() => openerRef.current?.focus()}
      >
        <form onSubmit={(event) => void submit(event)}>
          <h2 id="bounty-title">发起悬赏</h2>
          <p className="muted">积分会在悬赏期间冻结。</p>
          <label>悬赏积分（10–500）
            <input
              ref={pointsRef}
              type="number"
              min={10}
              max={500}
              required
              value={points}
              onChange={(event) => setPoints(event.target.value)}
            />
          </label>
          <label>有效天数（1–14）
            <input
              type="number"
              min={1}
              max={14}
              required
              value={days}
              onChange={(event) => setDays(event.target.value)}
            />
          </label>
          {error && <p className="error" role="alert">{error}</p>}
          <div className="dialog-actions">
            <button type="button" disabled={submitting} onClick={close}>取消</button>
            <button className="primary" type="submit" disabled={submitting}>
              {submitting ? '提交中…' : '确认悬赏'}
            </button>
          </div>
        </form>
      </dialog>
    </>
  );
}

function CommentList({ comments, targetType, targetId, onDone, canComment }: {
  comments: { id: number; author: { displayName: string }; body: string; createdAt: string }[];
  targetType: 'QUESTION' | 'ANSWER';
  targetId: number;
  onDone: () => void;
  canComment: boolean;
}) {
  const [text, setText] = useState('');
  const [open, setOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const submit = async () => {
    if (submitting) return;
    setSubmitting(true);
    setError('');
    try {
      await api.comment(targetType, targetId, text);
      setText('');
      setOpen(false);
      onDone();
    } catch (requestError) {
      setError((requestError as Error).message);
    } finally {
      setSubmitting(false);
    }
  };
  return (
    <div className="comments">
      {comments.map((c) => (
        <div className="comment" key={c.id}>
          <span>{c.body}</span>
          <span className="muted"> — {c.author.displayName}</span>
        </div>
      ))}
      {canComment && (open ? (
        <div className="comment-form">
          <input value={text} onChange={(e) => setText(e.target.value)} placeholder="评论，可用 @昵称 提醒他人" />
          <button disabled={submitting} onClick={() => void submit()}>
            {submitting ? '发表中…' : '发表'}
          </button>
          {error && <span className="error" role="alert">{error}</span>}
        </div>
      ) : <button className="link" onClick={() => setOpen(true)}>添加评论</button>)}
    </div>
  );
}
