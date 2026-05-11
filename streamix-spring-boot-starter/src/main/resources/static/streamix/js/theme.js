import { EventBus, EVENTS } from './event-bus.js';

const KEY = 'streamix.theme';
const VALID = new Set(['system', 'light', 'dark']);

function getStored() {
  const v = localStorage.getItem(KEY);
  return VALID.has(v) ? v : 'system';
}

function resolve(theme) {
  if (theme === 'system') {
    return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }
  return theme;
}

function apply(theme) {
  const actual = resolve(theme);
  document.documentElement.classList.toggle('dark', actual === 'dark');
  EventBus.emit(EVENTS.THEME_CHANGED, { theme, resolved: actual });
}

export const Theme = {
  current: getStored(),

  resolved() {
    return resolve(this.current);
  },

  set(theme) {
    if (!VALID.has(theme)) return;
    this.current = theme;
    localStorage.setItem(KEY, theme);
    apply(theme);
  },

  toggle() {
    const next = this.current === 'light' ? 'dark'
               : this.current === 'dark'  ? 'system'
               : 'light';
    this.set(next);
  },

  init() {
    if (this._initialized) return;
    this._initialized = true;
    apply(this.current);

    const mq = matchMedia('(prefers-color-scheme: dark)');
    const mqListener = () => {
      if (this.current === 'system') apply('system');
    };
    if (mq.addEventListener) mq.addEventListener('change', mqListener);
    else if (mq.addListener) mq.addListener(mqListener);

    window.addEventListener('storage', (e) => {
      if (e.key === KEY && e.newValue && VALID.has(e.newValue)) {
        this.current = e.newValue;
        apply(e.newValue);
      }
    });
  }
};
