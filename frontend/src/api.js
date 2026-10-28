export async function api(path, opts = {}) {
  const token = localStorage.getItem('token');
  const res = await fetch('/api' + path, {
    method: opts.method || 'GET',
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: 'Bearer ' + token } : {}) },
    body: opts.body ? JSON.stringify(opts.body) : undefined
  });
  if (!res.ok) {
    if ((res.status === 401 || res.status === 403) && !path.startsWith('/auth')) {
      localStorage.clear();
      location.reload();
    }
    let msg = 'Request failed (' + res.status + ')';
    try { const j = await res.json(); if (j.message) msg = j.message; } catch (e) {}
    throw new Error(msg);
  }
  return res.json();
}
