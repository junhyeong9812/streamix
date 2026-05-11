import { EventBus, EVENTS } from './event-bus.js';

const state = {
  theme: 'system',
  activeSessions: [],
  files: [],
  pagination: { page: 0, size: 20, total: 0 },
  upload: { active: false, progress: 0 }
};

function resolve(path) {
  return path.split('.').reduce((o, k) => (o == null ? o : o[k]), state);
}

function set(path, value) {
  const parts = path.split('.');
  let target = state;
  for (let i = 0; i < parts.length - 1; i++) {
    if (target[parts[i]] == null || typeof target[parts[i]] !== 'object') {
      target[parts[i]] = {};
    }
    target = target[parts[i]];
  }
  target[parts.at(-1)] = value;
  EventBus.emit(EVENTS.STORE_CHANGED, { path, value });
}

export const Store = {
  get: resolve,
  set,
  state
};
