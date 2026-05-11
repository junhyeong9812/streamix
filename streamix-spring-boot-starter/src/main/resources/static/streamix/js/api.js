export class ApiError extends Error {
  constructor(status, body) {
    super((body && body.message) || `HTTP ${status}`);
    this.name = 'ApiError';
    this.status = status;
    this.body = body || {};
  }
}

const baseUrl = (document.body && document.body.dataset.apiBasePath) || '/api/streamix';

async function request(method, path, options = {}) {
  const res = await fetch(`${baseUrl}${path}`, {
    method,
    headers: {
      Accept: 'application/json',
      ...(options.headers || {})
    },
    body: options.body,
    credentials: 'same-origin'
  });

  if (!res.ok) {
    let body = {};
    try {
      body = await res.json();
    } catch {
      body = { message: res.statusText };
    }
    throw new ApiError(res.status, body);
  }

  if (res.status === 204) return null;
  const ctype = res.headers.get('content-type') || '';
  return ctype.includes('json') ? res.json() : res.text();
}

export const Api = {
  baseUrl,
  request,
  files: {
    list:   (page = 0, size = 20) => request('GET', `/files?page=${page}&size=${size}`),
    get:    (id) => request('GET', `/files/${id}`),
    delete: (id) => request('DELETE', `/files/${id}`)
  },
  sessions: {
    active: () => request('GET', '/sessions/active')
  }
};
