import { showToast } from './toast.js';
import { EventBus, EVENTS } from '../event-bus.js';
import { formatFileSize } from '../utils.js';

const FILE_TYPES = {
  IMAGE:    ['image/'],
  VIDEO:    ['video/'],
  AUDIO:    ['audio/'],
  DOCUMENT: [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument',
    'application/vnd.ms-excel',
    'application/vnd.ms-powerpoint',
    'text/plain', 'text/html', 'text/css', 'text/javascript',
    'application/json', 'application/xml'
  ],
  ARCHIVE: [
    'application/zip',
    'application/x-rar-compressed',
    'application/x-7z-compressed',
    'application/gzip',
    'application/x-tar',
    'application/x-bzip2'
  ]
};

const TYPE_LABEL = {
  IMAGE: '이미지', VIDEO: '비디오', AUDIO: '오디오',
  DOCUMENT: '문서', ARCHIVE: '압축파일', OTHER: '기타'
};

export function detectFileType(mime) {
  if (!mime) return 'OTHER';
  for (const [name, prefixes] of Object.entries(FILE_TYPES)) {
    for (const p of prefixes) {
      if (mime.startsWith(p) || mime === p) return name;
    }
  }
  return 'OTHER';
}

function parseAllowed(value) {
  if (!value || value.trim() === '' || value === '[]') return [];
  return value.replace(/[\[\]]/g, '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);
}

export function initUpload(root = document) {
  const area     = root.querySelector('[data-streamix-upload]');
  const input    = root.querySelector('[data-streamix-upload-input]');
  if (!area || !input) return;

  const progress = root.querySelector('[data-streamix-upload-progress]');
  const progressBar = progress ? progress.querySelector('.streamix-progress-bar') : null;

  area.addEventListener('click', () => input.click());
  area.addEventListener('dragover', (e) => {
    e.preventDefault();
    area.classList.add('is-drag');
  });
  area.addEventListener('dragleave', (e) => {
    e.preventDefault();
    area.classList.remove('is-drag');
  });
  area.addEventListener('drop', (e) => {
    e.preventDefault();
    area.classList.remove('is-drag');
    if (e.dataTransfer.files.length > 0) upload(e.dataTransfer.files[0]);
  });
  input.addEventListener('change', () => {
    if (input.files.length > 0) upload(input.files[0]);
  });

  function upload(file) {
    const allowed = parseAllowed(document.body.dataset.allowedTypes);
    if (allowed.length > 0) {
      const t = detectFileType(file.type);
      if (!allowed.includes(t)) {
        const label = allowed.map((a) => TYPE_LABEL[a] || a).join(', ');
        showToast(`허용되지 않는 파일 타입입니다. 허용: ${label}`, 'error');
        return;
      }
    }

    const maxSize = parseInt(document.body.dataset.maxFileSize || '0', 10);
    if (maxSize > 0 && file.size > maxSize) {
      showToast(`파일 크기는 ${formatFileSize(maxSize)}를 초과할 수 없습니다.`, 'error');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    if (progress) progress.classList.remove('streamix-hidden');
    if (progressBar) progressBar.style.width = '0%';
    area.classList.add('is-uploading');

    EventBus.emit(EVENTS.FILE_UPLOAD_STARTED, { file });

    const xhr = new XMLHttpRequest();
    const apiBase = document.body.dataset.apiBasePath || '/api/streamix';
    xhr.open('POST', `${apiBase}/files`, true);

    xhr.upload.onprogress = (e) => {
      if (!e.lengthComputable) return;
      const pct = Math.round((e.loaded / e.total) * 100);
      if (progressBar) progressBar.style.width = pct + '%';
      EventBus.emit(EVENTS.FILE_UPLOAD_PROGRESS, { progress: pct });
    };

    xhr.onload = () => {
      area.classList.remove('is-uploading');
      if (xhr.status === 201) {
        EventBus.emit(EVENTS.FILE_UPLOAD_SUCCESS, {});
        showToast('파일이 업로드되었습니다.', 'success');
        setTimeout(() => location.reload(), 800);
      } else {
        let msg = '업로드에 실패했습니다.';
        let parsed = null;
        try { parsed = JSON.parse(xhr.responseText); } catch { /* responseText not JSON */ }
        if (parsed && parsed.message) msg = parsed.message;
        else if (xhr.statusText) msg = `${xhr.status} ${xhr.statusText}`;
        EventBus.emit(EVENTS.FILE_UPLOAD_ERROR, { message: msg, status: xhr.status });
        showToast(msg, 'error');
        if (progress) progress.classList.add('streamix-hidden');
        if (progressBar) progressBar.style.width = '0%';
      }
    };

    xhr.onerror = () => {
      area.classList.remove('is-uploading');
      EventBus.emit(EVENTS.FILE_UPLOAD_ERROR, { message: 'network', status: 0 });
      showToast('네트워크 오류가 발생했습니다.', 'error');
      if (progress) progress.classList.add('streamix-hidden');
    };

    xhr.send(formData);
  }
}
