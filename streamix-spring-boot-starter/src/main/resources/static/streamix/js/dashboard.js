/**
 * Streamix Dashboard JavaScript
 * 미디어 스트리밍 관리 대시보드 스크립트
 */

(function() {
  'use strict';

  // ===== 초기화 =====
  document.addEventListener('DOMContentLoaded', function() {
    initCopyButtons();
    initDeleteConfirmation();
    initTooltips();
    initFileUpload();
    initAutoRefresh();
    initSidebarToggle();
    initFormatters();
  });

  // ===== URL 복사 기능 =====
  function initCopyButtons() {
    document.querySelectorAll('.btn-copy').forEach(function(btn) {
      btn.addEventListener('click', function() {
        const targetId = this.dataset.target;
        const input = document.getElementById(targetId);

        if (input) {
          copyToClipboard(input.value, this);
        }
      });
    });
  }

  function copyToClipboard(text, button) {
    navigator.clipboard.writeText(text).then(function() {
      const originalText = button.innerHTML;
      button.innerHTML = '<i class="bi bi-check"></i> 복사됨';
      button.classList.add('btn-success');
      button.classList.remove('btn-outline-secondary');

      setTimeout(function() {
        button.innerHTML = originalText;
        button.classList.remove('btn-success');
        button.classList.add('btn-outline-secondary');
      }, 2000);
    }).catch(function(err) {
      console.error('복사 실패:', err);
      showToast('복사에 실패했습니다.', 'error');
    });
  }

  // ===== 삭제 확인 =====
  function initDeleteConfirmation() {
    const deleteModal = document.getElementById('deleteModal');
    if (!deleteModal) return;

    deleteModal.addEventListener('show.bs.modal', function(event) {
      const button = event.relatedTarget;
      const fileId = button.dataset.fileId;
      const fileName = button.dataset.fileName;

      const modalFileName = deleteModal.querySelector('#deleteFileName');
      const modalForm = deleteModal.querySelector('#deleteForm');

      if (modalFileName) {
        modalFileName.textContent = fileName;
      }

      if (modalForm) {
        const basePath = modalForm.dataset.basePath || '';
        modalForm.action = basePath + '/files/' + fileId + '/delete';
      }
    });
  }

  // ===== 툴팁 초기화 =====
  function initTooltips() {
    const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function(el) {
      new bootstrap.Tooltip(el);
    });
  }

  // ===== 파일 업로드 (드래그 앤 드롭) =====
  function initFileUpload() {
    const uploadArea = document.getElementById('uploadArea');
    const fileInput = document.getElementById('fileInput');

    if (!uploadArea || !fileInput) return;

    // 클릭으로 파일 선택
    uploadArea.addEventListener('click', function() {
      fileInput.click();
    });

    // 드래그 앤 드롭
    uploadArea.addEventListener('dragover', function(e) {
      e.preventDefault();
      uploadArea.classList.add('drag-over');
    });

    uploadArea.addEventListener('dragleave', function(e) {
      e.preventDefault();
      uploadArea.classList.remove('drag-over');
    });

    uploadArea.addEventListener('drop', function(e) {
      e.preventDefault();
      uploadArea.classList.remove('drag-over');

      const files = e.dataTransfer.files;
      if (files.length > 0) {
        handleFileUpload(files[0]);
      }
    });

    // 파일 선택 변경
    fileInput.addEventListener('change', function() {
      if (this.files.length > 0) {
        handleFileUpload(this.files[0]);
      }
    });
  }

  function handleFileUpload(file) {
    const uploadArea = document.getElementById('uploadArea');
    const progressBar = document.getElementById('uploadProgress');
    const progressBarInner = progressBar?.querySelector('.progress-bar');

    // 파일 타입 검증
    const allowedTypes = ['image/', 'video/'];
    const isAllowed = allowedTypes.some(type => file.type.startsWith(type));

    if (!isAllowed) {
      showToast('이미지 또는 비디오 파일만 업로드할 수 있습니다.', 'error');
      return;
    }

    // 파일 크기 제한 (500MB)
    const maxSize = 500 * 1024 * 1024;
    if (file.size > maxSize) {
      showToast('파일 크기는 500MB를 초과할 수 없습니다.', 'error');
      return;
    }

    // FormData 생성
    const formData = new FormData();
    formData.append('file', file);

    // 프로그레스 바 표시
    if (progressBar) {
      progressBar.classList.remove('d-none');
    }

    uploadArea.classList.add('uploading');

    // AJAX 업로드
    const xhr = new XMLHttpRequest();
    const apiBasePath = document.body.dataset.apiBasePath || '/api/streamix';

    xhr.open('POST', apiBasePath + '/files', true);

    // 업로드 진행률
    xhr.upload.onprogress = function(e) {
      if (e.lengthComputable && progressBarInner) {
        const percent = Math.round((e.loaded / e.total) * 100);
        progressBarInner.style.width = percent + '%';
        progressBarInner.textContent = percent + '%';
      }
    };

    xhr.onload = function() {
      uploadArea.classList.remove('uploading');

      if (xhr.status === 201) {
        showToast('파일이 업로드되었습니다.', 'success');
        setTimeout(function() {
          location.reload();
        }, 1000);
      } else {
        const error = JSON.parse(xhr.responseText);
        showToast(error.message || '업로드에 실패했습니다.', 'error');
        if (progressBar) {
          progressBar.classList.add('d-none');
        }
      }
    };

    xhr.onerror = function() {
      uploadArea.classList.remove('uploading');
      showToast('네트워크 오류가 발생했습니다.', 'error');
      if (progressBar) {
        progressBar.classList.add('d-none');
      }
    };

    xhr.send(formData);
  }

  // ===== 자동 새로고침 (세션 페이지) =====
  function initAutoRefresh() {
    const autoRefreshToggle = document.getElementById('autoRefresh');
    if (!autoRefreshToggle) return;

    let refreshInterval = null;

    autoRefreshToggle.addEventListener('change', function() {
      if (this.checked) {
        refreshInterval = setInterval(function() {
          refreshActiveSessions();
        }, 5000);
      } else {
        if (refreshInterval) {
          clearInterval(refreshInterval);
          refreshInterval = null;
        }
      }
    });
  }

  function refreshActiveSessions() {
    const sessionsTable = document.getElementById('activeSessionsTable');
    if (!sessionsTable) return;

    // 실제 구현에서는 AJAX로 세션 데이터를 가져와 테이블 업데이트
    // 여기서는 간단히 시간만 업데이트
    const timeElements = sessionsTable.querySelectorAll('.session-duration');
    timeElements.forEach(function(el) {
      const startTime = new Date(el.dataset.startTime);
      const duration = formatDuration(Date.now() - startTime.getTime());
      el.textContent = duration;
    });
  }

  // ===== 사이드바 토글 (모바일) =====
  function initSidebarToggle() {
    const toggleBtn = document.getElementById('sidebarToggle');
    const sidebar = document.querySelector('.sidebar');
    const overlay = document.getElementById('sidebarOverlay');

    if (!toggleBtn || !sidebar) return;

    toggleBtn.addEventListener('click', function() {
      sidebar.classList.toggle('show');
      if (overlay) {
        overlay.classList.toggle('show');
      }
    });

    if (overlay) {
      overlay.addEventListener('click', function() {
        sidebar.classList.remove('show');
        overlay.classList.remove('show');
      });
    }
  }

  // ===== 데이터 포맷터 =====
  function initFormatters() {
    // 파일 크기 포맷
    document.querySelectorAll('[data-format="filesize"]').forEach(function(el) {
      const bytes = parseInt(el.dataset.value, 10);
      el.textContent = formatFileSize(bytes);
    });

    // 날짜 포맷
    document.querySelectorAll('[data-format="datetime"]').forEach(function(el) {
      const date = new Date(el.dataset.value);
      el.textContent = formatDateTime(date);
    });

    // 시간 포맷
    document.querySelectorAll('[data-format="duration"]').forEach(function(el) {
      const ms = parseInt(el.dataset.value, 10);
      el.textContent = formatDuration(ms);
    });
  }

  // ===== 유틸리티 함수 =====
  function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const k = 1024;
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + units[i];
  }

  function formatDateTime(date) {
    if (!date || isNaN(date.getTime())) return '-';

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');

    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
  }

  function formatDuration(ms) {
    if (!ms || ms < 0) return '-';

    const seconds = Math.floor(ms / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (hours > 0) {
      return hours + '시간 ' + (minutes % 60) + '분';
    } else if (minutes > 0) {
      return minutes + '분 ' + (seconds % 60) + '초';
    } else {
      return seconds + '초';
    }
  }

  // ===== 토스트 메시지 =====
  function showToast(message, type) {
    const toastContainer = getOrCreateToastContainer();

    const toastEl = document.createElement('div');
    toastEl.className = 'toast align-items-center border-0';
    toastEl.classList.add(type === 'error' ? 'bg-danger' : 'bg-success');
    toastEl.classList.add('text-white');
    toastEl.setAttribute('role', 'alert');

    toastEl.innerHTML =
        '<div class="d-flex">' +
        '<div class="toast-body">' +
        '<i class="bi ' + (type === 'error' ? 'bi-exclamation-circle' : 'bi-check-circle') + ' me-2"></i>' +
        message +
        '</div>' +
        '<button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>' +
        '</div>';

    toastContainer.appendChild(toastEl);

    const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', function() {
      toastEl.remove();
    });
  }

  function getOrCreateToastContainer() {
    let container = document.getElementById('toastContainer');

    if (!container) {
      container = document.createElement('div');
      container.id = 'toastContainer';
      container.className = 'toast-container position-fixed top-0 end-0 p-3';
      container.style.zIndex = '1100';
      document.body.appendChild(container);
    }

    return container;
  }

  // ===== 전역 함수 노출 =====
  window.Streamix = {
    formatFileSize: formatFileSize,
    formatDateTime: formatDateTime,
    formatDuration: formatDuration,
    showToast: showToast,
    copyToClipboard: copyToClipboard
  };

})();