import { Api } from '../api.js';
import { EventBus, EVENTS } from '../event-bus.js';

let intervalId = null;
let lastError = null;

async function tick() {
  if (document.visibilityState !== 'visible') return;
  try {
    const sessions = await Api.sessions.active();
    lastError = null;
    EventBus.emit(EVENTS.SESSIONS_REFRESHED, sessions);
  } catch (e) {
    lastError = e;
  }
}

export const SessionsPoller = {
  start(periodMs = 5000) {
    if (intervalId) return;
    tick();
    intervalId = setInterval(tick, periodMs);
  },
  stop() {
    if (intervalId) {
      clearInterval(intervalId);
      intervalId = null;
    }
  },
  isRunning() {
    return intervalId !== null;
  },
  lastError() {
    return lastError;
  }
};
