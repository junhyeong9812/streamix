const subs = new Map();

export const EventBus = {
  on(event, handler) {
    if (!subs.has(event)) subs.set(event, new Set());
    subs.get(event).add(handler);
    return () => subs.get(event)?.delete(handler);
  },
  off(event, handler) {
    subs.get(event)?.delete(handler);
  },
  emit(event, payload) {
    const handlers = subs.get(event);
    if (!handlers) return;
    handlers.forEach((h) => {
      try {
        h(payload);
      } catch (e) {
        console.error(`[Streamix] handler error for ${event}:`, e);
      }
    });
  }
};

export const EVENTS = Object.freeze({
  FILE_UPLOAD_STARTED:  'file:upload:started',
  FILE_UPLOAD_PROGRESS: 'file:upload:progress',
  FILE_UPLOAD_SUCCESS:  'file:upload:success',
  FILE_UPLOAD_ERROR:    'file:upload:error',
  FILE_DELETED:         'file:deleted',
  SESSIONS_REFRESHED:   'sessions:refreshed',
  THEME_CHANGED:        'theme:changed',
  TOAST:                'ui:toast',
  MODAL_OPEN:           'ui:modal:open',
  MODAL_CLOSE:          'ui:modal:close',
  STORE_CHANGED:        'store:changed'
});
