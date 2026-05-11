import { EventBus, EVENTS } from '../event-bus.js';
import { spriteIcon } from '../utils.js';

const SVG_NS = 'http://www.w3.org/2000/svg';

function iconSymbol(type) {
  return type === 'error' ? 'exclamation-triangle' : 'check-circle';
}

function getContainer() {
  let c = document.getElementById('streamix-toasts');
  if (!c) {
    c = document.createElement('div');
    c.id = 'streamix-toasts';
    c.className = 'streamix-toast-container';
    document.body.appendChild(c);
  }
  return c;
}

export function showToast(message, type = 'success', delay = 3500) {
  const container = getContainer();
  const el = document.createElement('div');
  const kind = type === 'error' ? 'error' : 'success';
  el.className = `streamix-toast streamix-toast--${kind}`;
  el.setAttribute('role', 'alert');
  el.setAttribute('aria-live', 'assertive');
  el.setAttribute('aria-atomic', 'true');

  const icon = document.createElementNS(SVG_NS, 'svg');
  icon.setAttribute('class', 'streamix-icon');
  icon.setAttribute('aria-hidden', 'true');
  const use = document.createElementNS(SVG_NS, 'use');
  use.setAttribute('href', spriteIcon(iconSymbol(type)));
  icon.appendChild(use);

  const body = document.createElement('div');
  body.textContent = message || '';

  el.append(icon, body);
  container.appendChild(el);

  const timer = setTimeout(() => dismiss(el), delay);
  el.addEventListener('click', () => {
    clearTimeout(timer);
    dismiss(el);
  });
}

function dismiss(el) {
  if (!el.isConnected) return;
  el.classList.add('is-leaving');
  el.addEventListener('animationend', () => el.remove(), { once: true });
  setTimeout(() => { if (el.isConnected) el.remove(); }, 400);
}

EventBus.on(EVENTS.TOAST, (payload = {}) => {
  showToast(payload.message, payload.type, payload.delay);
});
