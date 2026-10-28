import { useEffect, useState, useCallback } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';
import { api } from './api';

const CATS = ['SPAM', 'HARASSMENT', 'HATE_SPEECH', 'MISINFORMATION', 'OTHER'];
const COLORS = ['#6366f1', '#ef4444', '#f59e0b', '#10b981', '#64748b'];

function Login({ onLogin }) {
  const [u, setU] = useState('');
  const [p, setP] = useState('');
  const [err, setErr] = useState('');
  const go = async (mode) => {
    try {
      const r = await api('/auth/' + mode, { method: 'POST', body: { username: u, password: p } });
      const user = { username: r.username, role: r.role };
      localStorage.setItem('token', r.token);
      localStorage.setItem('user', JSON.stringify(user));
      onLogin(user);
    } catch (e) { setErr(e.message); }
  };
  return (
    <div className="card login">
      <h2>Content Moderation</h2>
      <input placeholder="Username" value={u} onChange={(e) => setU(e.target.value)} />
      <input placeholder="Password" type="password" value={p} onChange={(e) => setP(e.target.value)} />
      <div className="row">
        <button onClick={() => go('login')}>Login</button>
        <button className="ghost" onClick={() => go('register')}>Register</button>
      </div>
      {err && <p className="err">{err}</p>}
      <small>Demo: admin/admin123 · mod/mod123 · alice/alice123</small>
    </div>
  );
}

function Feed() {
  const [posts, setPosts] = useState([]);
  const [text, setText] = useState('');
  const [msg, setMsg] = useState('');
  const [rep, setRep] = useState(null);
  const [cat, setCat] = useState('SPAM');
  const [reason, setReason] = useState('');

  const load = useCallback(() => { api('/posts').then(setPosts).catch((e) => setMsg(e.message)); }, []);
  useEffect(() => { load(); }, [load]);

  const create = async () => {
    if (!text.trim()) return;
    try { await api('/posts', { method: 'POST', body: { content: text } }); setText(''); load(); }
    catch (e) { setMsg(e.message); }
  };
  const submit = async () => {
    try {
      await api(`/posts/${rep}/report`, { method: 'POST', body: { category: cat, reason } });
      setMsg('Report submitted. Thank you!'); setRep(null); setReason('');
    } catch (e) { setMsg(e.message); }
  };

  return (
    <div>
      <div className="card">
        <textarea placeholder="Write a post..." value={text} onChange={(e) => setText(e.target.value)} />
        <button onClick={create}>Post</button>
      </div>
      {msg && <p className="ok">{msg}</p>}
      {posts.map((p) => (
        <div className="card" key={p.id}>
          <b>@{p.author}</b> <small>{new Date(p.createdAt).toLocaleString()}</small>
          <p>{p.content}</p>
          {rep === p.id ? (
            <div className="row">
              <select value={cat} onChange={(e) => setCat(e.target.value)}>
                {CATS.map((c) => <option key={c}>{c}</option>)}
              </select>
              <input placeholder="Reason (optional)" value={reason} onChange={(e) => setReason(e.target.value)} />
              <button onClick={submit}>Submit</button>
              <button className="ghost" onClick={() => setRep(null)}>Cancel</button>
            </div>
          ) : (
            <button className="ghost" onClick={() => setRep(p.id)}>Report</button>
          )}
        </div>
      ))}
    </div>
  );
}

function Queue() {
  const [status, setStatus] = useState('OPEN');
  const [category, setCategory] = useState('');
  const [page, setPage] = useState(0);
  const [d, setD] = useState({ items: [], totalPages: 0, totalElements: 0 });
  const [err, setErr] = useState('');

  const load = useCallback(() => {
    const q = new URLSearchParams({ page, size: 5 });
    if (status) q.set('status', status);
    if (category) q.set('category', category);
    api('/moderation/reports?' + q.toString()).then(setD).catch((e) => setErr(e.message));
  }, [page, status, category]);
  useEffect(() => { load(); }, [load]);

  const act = async (id, action) => {
    try { await api(`/moderation/reports/${id}/resolve`, { method: 'POST', body: { action } }); load(); }
    catch (e) { setErr(e.message); }
  };

  return (
    <div>
      <div className="card row">
        <select value={status} onChange={(e) => { setStatus(e.target.value); setPage(0); }}>
          <option value="">All statuses</option>
          {['OPEN', 'APPROVED', 'REMOVED', 'WARNED'].map((s) => <option key={s}>{s}</option>)}
        </select>
        <select value={category} onChange={(e) => { setCategory(e.target.value); setPage(0); }}>
          <option value="">All categories</option>
          {CATS.map((c) => <option key={c}>{c}</option>)}
        </select>
        <small>{d.totalElements} reports</small>
      </div>
      {err && <p className="err">{err}</p>}
      {d.items.map((r) => (
        <div className={'card' + (r.priority ? ' flag' : '')} key={r.id}>
          <div className="row">
            <b>#{r.id}</b>
            <span className="tag">{r.category}</span>
            <span className="tag">{r.status}</span>
            {r.priority === 1 && <span className="tag red">⚑ keyword: {r.flaggedKeyword}</span>}
          </div>
          <p>"{r.postContent}" <small>by @{r.postAuthor}</small></p>
          <small>Reported by @{r.reporter}{r.reason ? ' — ' + r.reason : ''}</small>
          {r.status === 'OPEN' ? (
            <div className="row">
              <button onClick={() => act(r.id, 'APPROVE')}>Approve</button>
              <button className="danger" onClick={() => act(r.id, 'REMOVE')}>Remove</button>
              <button className="warn" onClick={() => act(r.id, 'WARN')}>Warn</button>
            </div>
          ) : <small> · resolved by @{r.resolvedBy}</small>}
        </div>
      ))}
      {d.items.length === 0 && <p>No reports found.</p>}
      <div className="row">
        <button className="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
        <span>Page {page + 1} / {Math.max(d.totalPages, 1)}</span>
        <button className="ghost" disabled={page + 1 >= d.totalPages} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </div>
  );
}

function Audit() {
  const [page, setPage] = useState(0);
  const [d, setD] = useState({ items: [], totalPages: 0 });
  useEffect(() => { api('/moderation/audit?page=' + page).then(setD).catch(() => {}); }, [page]);
  return (
    <div className="card">
      <h3>Audit Log</h3>
      <table>
        <thead><tr><th>Time</th><th>Moderator</th><th>Action</th><th>Report</th><th>Post</th></tr></thead>
        <tbody>
          {d.items.map((a) => (
            <tr key={a.id}>
              <td>{new Date(a.createdAt).toLocaleString()}</td>
              <td>{a.moderator}</td><td>{a.action}</td><td>#{a.reportId}</td><td>#{a.postId}</td>
            </tr>
          ))}
        </tbody>
      </table>
      <div className="row">
        <button className="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Prev</button>
        <span>Page {page + 1} / {Math.max(d.totalPages, 1)}</span>
        <button className="ghost" disabled={page + 1 >= d.totalPages} onClick={() => setPage(page + 1)}>Next</button>
      </div>
    </div>
  );
}

function Analytics() {
  const [a, setA] = useState(null);
  useEffect(() => { api('/admin/analytics').then(setA).catch(() => {}); }, []);
  if (!a) return <p>Loading analytics…</p>;
  const cat = Object.entries(a.byCategory).map(([name, value]) => ({ name, value }));
  const st = Object.entries(a.byStatus).map(([name, value]) => ({ name, value }));
  return (
    <div>
      <div className="stats">
        <div className="card"><h3>{a.total}</h3><small>Total reports</small></div>
        <div className="card"><h3>{a.flagged}</h3><small>Keyword-flagged</small></div>
        <div className="card"><h3>{a.byStatus.OPEN || 0}</h3><small>Open</small></div>
      </div>
      <div className="charts">
        <div className="card chart">
          <h4>Reports by category</h4>
          <ResponsiveContainer width="100%" height={240}>
            <PieChart>
              <Pie data={cat} dataKey="value" nameKey="name" outerRadius={85} label>
                {cat.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>
        <div className="card chart">
          <h4>Reports by status</h4>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={st}>
              <XAxis dataKey="name" /><YAxis allowDecimals={false} /><Tooltip />
              <Bar dataKey="value" fill="#6366f1" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
      <h3>All reports (filter &amp; paginate)</h3>
      <Queue />
    </div>
  );
}

export default function App() {
  const [user, setUser] = useState(() => JSON.parse(localStorage.getItem('user') || 'null'));
  const [tab, setTab] = useState('feed');
  if (!user) return <Login onLogin={setUser} />;
  const staff = user.role !== 'USER';
  const admin = user.role === 'ADMIN';
  const logout = () => { localStorage.clear(); setUser(null); };
  return (
    <div className="wrap">
      <header>
        <h2>🛡️ Moderation Dashboard</h2>
        <div className="row">
          <span>@{user.username} <span className="tag">{user.role}</span></span>
          <button className="ghost" onClick={logout}>Logout</button>
        </div>
      </header>
      <nav className="row">
        <button className={tab === 'feed' ? '' : 'ghost'} onClick={() => setTab('feed')}>Feed</button>
        {staff && <button className={tab === 'queue' ? '' : 'ghost'} onClick={() => setTab('queue')}>Review Queue</button>}
        {staff && <button className={tab === 'audit' ? '' : 'ghost'} onClick={() => setTab('audit')}>Audit Log</button>}
        {admin && <button className={tab === 'analytics' ? '' : 'ghost'} onClick={() => setTab('analytics')}>Analytics</button>}
      </nav>
      {tab === 'feed' && <Feed />}
      {tab === 'queue' && staff && <Queue />}
      {tab === 'audit' && staff && <Audit />}
      {tab === 'analytics' && admin && <Analytics />}
    </div>
  );
}
