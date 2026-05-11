import { EventBus, EVENTS } from '../event-bus.js';

const FOCUSABLE = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])'
].join(',');

const initialized = new WeakSet();
const trapHandlers = new WeakMap();
const previousFocus = new WeakMap();

function setup(overlay) {
  if (initialized.has(overlay)) return;
  initialized.add(overlay);

  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) close(overlay);
  });

  overlay.querySelectorAll('[data-streamix-modal-close]').forEach((btn) => {
    btn.addEventListener('click', () => close(overlay));
  });
}

function focusableElements(overlay) {
  return Array.from(overlay.querySelectorAll(FOCUSABLE))
    .filter((el) => el.offsetParent !== null || el === document.activeElement);
}

function trapFocus(overlay) {
  const handler = (e) => {
    if (e.key !== 'Tab') return;
    const items = focusableElements(overlay);
    if (items.length === 0) return;
    const first = items[0];
    const last = items[items.length - 1];
    if (e.shiftKey) {
      if (document.activeElement === first || !overlay.contains(document.activeElement)) {
        e.preventDefault();
        last.focus();
      }
    } else {
      if (document.activeElement === last || !overlay.contains(document.activeElement)) {
        e.preventDefault();
        first.focus();
      }
    }
  };
  overlay.addEventListener('keydown', handler);
  trapHandlers.set(overlay, handler);

  const items = focusableElements(overlay);
  if (items.length > 0) {
    setTimeout(() => items[0].focus(), 0);
  }
}

function releaseFocus(overlay) {
  const handler = trapHandlers.get(overlay);
  if (handler) {
    overlay.removeEventListener('keydown', handler);
    trapHandlers.delete(overlay);
  }
}

function open(overlay, context) {
  if (!overlay) return;
  setup(overlay);
  previousFocus.set(overlay, document.activeElement);
  overlay.classList.add('is-open');
  document.body.style.overflow = 'hidden';
  overlay.dispatchEvent(new CustomEvent('streamix:modal:open', { detail: context || {} }));
  EventBus.emit(EVENTS.MODAL_OPEN, { id: overlay.id, context });
  trapFocus(overlay);
}

function close(overlay) {
  if (!overlay) return;
  releaseFocus(overlay);
  overlay.classList.remove('is-open');
  document.body.style.overflow = '';
  const previous = previousFocus.get(overlay);
  if (previous instanceof HTMLElement) {
    previous.focus();
    previousFocus.delete(overlay);
  }
  overlay.dispatchEvent(new CustomEvent('streamix:modal:close'));
  EventBus.emit(EVENTS.MODAL_CLOSE, { id: overlay.id });
}

document.addEventListener('keydown', (e) => {
  if (e.key !== 'Escape') return;
  document.querySelectorAll('.streamix-modal-overlay.is-open').forEach((overlay) => close(overlay));
});

document.addEventListener('click', (e) => {
  const trigger = e.target.closest('[data-streamix-modal-target]');
  if (!trigger) return;
  const targetId = trigger.dataset.streamixModalTarget;
  const target = document.getElementById(targetId);
  if (!target) return;
  const context = { trigger, dataset: { ...trigger.dataset } };
  open(target, context);
});

export const Modal = { open, close };
