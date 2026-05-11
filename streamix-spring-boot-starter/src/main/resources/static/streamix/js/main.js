import { EventBus, EVENTS } from './event-bus.js';
import { Store } from './store.js';
import { Api, ApiError } from './api.js';
import { Theme } from './theme.js';
import {
  spriteBase,
  spriteIcon,
  formatFileSize,
  formatDuration,
  formatDateTime
} from './utils.js';
import { showToast } from './components/toast.js';
import { Modal } from './components/modal.js';
import { initUpload, detectFileType } from './components/upload.js';
import { SessionsPoller } from './components/sessions-poller.js';

function initThemeToggle() {
  const btn = document.querySelector('[data-streamix-theme-toggle]');
  if (!btn) return;
  const use = btn.querySelector('use');
  const label = btn.querySelector('[data-streamix-theme-label]');

  function paint() {
    const isDark = document.documentElement.classList.contains('dark');
    if (use) use.setAttribute('href', spriteIcon(isDark ? 'sun' : 'moon'));
    if (label) label.textContent = Theme.current.toUpperCase();
    btn.dataset.theme = Theme.current;
    btn.setAttribute('aria-label', `테마: ${Theme.current}. 클릭하여 변경`);
  }

  btn.addEventListener('click', () => Theme.toggle());
  EventBus.on(EVENTS.THEME_CHANGED, paint);
  paint();
}

function initMobileNav() {
  const toggle = document.querySelector('[data-streamix-mobile-toggle]');
  const sidebar = document.querySelector('.streamix-sidebar');
  const overlay = document.querySelector('[data-streamix-mobile-overlay]');
  if (!toggle || !sidebar) return;

  toggle.addEventListener('click', () => {
    const open = sidebar.classList.toggle('is-open');
    if (overlay) overlay.classList.toggle('is-open', open);
  });

  if (overlay) {
    overlay.addEventListener('click', () => {
      sidebar.classList.remove('is-open');
      overlay.classList.remove('is-open');
    });
  }
}

function initCopyButtons() {
  document.querySelectorAll('[data-streamix-copy]').forEach((btn) => {
    btn.addEventListener('click', async () => {
      const targetId = btn.dataset.streamixCopy;
      const input = document.getElementById(targetId);
      if (!input) return;
      try {
        await navigator.clipboard.writeText(input.value);
        const original = btn.textContent;
        btn.classList.add('is-copied');
        btn.textContent = '복사됨';
        setTimeout(() => {
          btn.classList.remove('is-copied');
          btn.textContent = original;
        }, 1500);
      } catch {
        showToast('복사에 실패했습니다.', 'error');
      }
    });
  });
}

function initFormatters() {
  document.querySelectorAll('[data-format="filesize"]').forEach((el) => {
    el.textContent = formatFileSize(parseInt(el.dataset.value, 10));
  });
  document.querySelectorAll('[data-format="duration"]').forEach((el) => {
    el.textContent = formatDuration(parseInt(el.dataset.value, 10));
  });
  document.querySelectorAll('[data-format="datetime"]').forEach((el) => {
    el.textContent = formatDateTime(new Date(el.dataset.value));
  });
}

function initDeleteModal() {
  const modal = document.querySelector('[data-streamix-modal="delete"]');
  if (!modal) return;
  modal.addEventListener('streamix:modal:open', (e) => {
    const trigger = e.detail.trigger;
    if (!trigger) return;
    const fileId = trigger.dataset.fileId;
    const fileName = trigger.dataset.fileName;
    const nameEl = modal.querySelector('[data-streamix-delete-name]');
    const form = modal.querySelector('[data-streamix-delete-form]');
    if (nameEl) nameEl.textContent = fileName || '';
    if (form) {
      const base = form.dataset.basePath || '';
      form.action = `${base}/files/${fileId}/delete`;
    }
  });
}

function initAutoRefresh() {
  const toggle = document.querySelector('[data-streamix-auto-refresh]');
  if (!toggle) return;
  toggle.addEventListener('change', () => {
    if (toggle.checked) SessionsPoller.start();
    else SessionsPoller.stop();
  });
}

function initSessionsAutoUpdate() {
  if (!document.querySelector('[data-streamix-auto-refresh]')) return;
  let baseline = null;
  EventBus.on(EVENTS.SESSIONS_REFRESHED, (sessions) => {
    if (!Array.isArray(sessions)) return;
    const count = sessions.length;
    if (baseline === null) {
      baseline = count;
      return;
    }
    if (count !== baseline) {
      location.reload();
    }
  });
}

function initFlashDismiss() {
  document.querySelectorAll('[data-streamix-alert-close]').forEach((btn) => {
    btn.addEventListener('click', () => {
      const alert = btn.closest('.streamix-alert');
      if (alert) alert.remove();
    });
  });
}

function init() {
  Theme.init();
  initThemeToggle();
  initMobileNav();
  initCopyButtons();
  initFormatters();
  initDeleteModal();
  initAutoRefresh();
  initSessionsAutoUpdate();
  initFlashDismiss();
  initUpload();
}

init();

window.Streamix = {
  events: EventBus,
  EVENTS,
  store: Store,
  api: Api,
  ApiError,
  theme: Theme,
  toast: showToast,
  modal: Modal,
  sessionsPoller: SessionsPoller,
  format: {
    fileSize: formatFileSize,
    duration: formatDuration,
    dateTime: formatDateTime
  },
  detectFileType,
  spriteBase,
  spriteIcon
};
