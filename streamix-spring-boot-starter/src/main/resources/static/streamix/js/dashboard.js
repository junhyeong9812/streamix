/**
 * Streamix Dashboard JavaScript
 * 미디어 스트리밍 관리 대시보드 스크립트
 * @version 2.0.0
 */

(function() {
  'use strict';

  // ===== FileType 매핑 =====
  var FileTypes = {
    IMAGE: {
      mimeTypes: ['image/'],
      icon: 'bi-image',
      color: 'success',
      label: '이미지'
    },
    VIDEO: {
      mimeTypes: ['video/'],
      icon: 'bi-camera-video',
      color: 'info',
      label: '비디오'
    },
    AUDIO: {
      mimeTypes: ['audio/'],
      icon: 'bi-music-note-beamed',
      color: 'purple',
      label: '오디오'
    },
    DOCUMENT: {
      mimeTypes: [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument',
        'application/vnd.ms-excel',
        'application/vnd.ms-powerpoint',
        'text/plain',
        'text/html',
        'text/css',
        'text/javascript',
        'application/json',
        'application/xml'
      ],
      icon: 'bi-file-earmark-text',
      color: 'warning',
      label: '문서'
    },
    ARCHIVE: {
      mimeTypes: [
        'application/zip',
        'application/x-rar-compressed',
        'application/x-7z-compressed',
        'application/gzip',
        'application/x-tar',
        'application/x-bzip2'
      ],
      icon: 'bi-file-earmark-zip',
      color: 'primary',
      label: '압축파일'
    },
    OTHER: {
      mimeTypes: [],
      icon: 'bi-file-earmark',
      color: 'secondary',
      label: '기타'
    }
  };

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
        var targetId = this.dataset.target;
        var input = document.getElementById(targetId);

        if (input) {
          copyToClipboard(input.value, this);
        }
      });
    });
  }

  function copyToClipboard(text, button) {
    navigator.clipboard.writeText(text).then(function() {
      var originalText = button.innerHTML;
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
    var deleteModal = document.getElementById('deleteModal');
    if (!deleteModal) return;

    deleteModal.addEventListener('show.bs.modal', function(event) {
      var button = event.relatedTarget;
      var fileId = button.dataset.fileId;
      var fileName = button.dataset.fileName;

      var modalFileName = deleteModal.querySelector('#deleteFileName');
      var modalForm = deleteModal.querySelector('#deleteForm');

      if (modalFileName) {
        modalFileName.textContent = fileName;
      }

      if (modalForm) {
        var basePath = modalForm.dataset.basePath || '';
        modalForm.action = basePath + '/files/' + fileId + '/delete';
      }
    });
  }

  // ===== 툴팁 초기화 =====
  function initTooltips() {
    var tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggerList.forEach(function(el) {
      new bootstrap.Tooltip(el);
    });
  }

  // ===== 파일 업로드 (드래그 앤 드롭) =====
  function initFileUpload() {
    var uploadArea = document.getElementById('uploadArea');
    var fileInput = document.getElementById('fileInput');

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

      var files = e.dataTransfer.files;
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

  /**
   * 파일 타입 감지
   */
  function detectFileType(mimeType) {
    if (!mimeType) return 'OTHER';

    for (var typeName in FileTypes) {
      if (typeName === 'OTHER') continue;

      var typeConfig = FileTypes[typeName];
      for (var i = 0; i < typeConfig.mimeTypes.length; i++) {
        if (mimeType.startsWith(typeConfig.mimeTypes[i]) ||
            mimeType === typeConfig.mimeTypes[i]) {
          return typeName;
        }
      }
    }
    return 'OTHER';
  }

  /**
   * 허용된 타입인지 확인
   */
  function isAllowedType(mimeType, allowedTypes) {
    // allowedTypes가 비어있으면 모든 타입 허용
    if (!allowedTypes || allowedTypes.length === 0) {
      return true;
    }

    var detectedType = detectFileType(mimeType);
    return allowedTypes.indexOf(detectedType) !== -1;
  }

  /**
   * 허용 타입 문자열 생성
   */
  function getAllowedTypesString(allowedTypes) {
    if (!allowedTypes || allowedTypes.length === 0) {
      return '모든 파일';
    }

    return allowedTypes.map(function(type) {
      return FileTypes[type] ? FileTypes[type].label : type;
    }).join(', ');
  }

  function handleFileUpload(file) {
    var uploadArea = document.getElementById('uploadArea');
    var progressBar = document.getElementById('uploadProgress');
    var progressBarInner = progressBar ? progressBar.querySelector('.progress-bar') : null;

    // 허용 타입 가져오기 (body data-attribute에서)
    var allowedTypesAttr = document.body.dataset.allowedTypes;
    var allowedTypes = [];
    if (allowedTypesAttr && allowedTypesAttr.trim() !== '' && allowedTypesAttr !== '[]') {
      try {
        // "[IMAGE, VIDEO]" 형태 파싱
        allowedTypes = allowedTypesAttr
        .replace(/[\[\]]/g, '')
        .split(',')
        .map(function(s) { return s.trim(); })
        .filter(function(s) { return s.length > 0; });
      } catch (e) {
        console.warn('Failed to parse allowed types:', e);
      }
    }

    // 파일 타입 검증
    if (!isAllowedType(file.type, allowedTypes)) {
      var allowedStr = getAllowedTypesString(allowedTypes);
      showToast('허용되지 않는 파일 타입입니다. 허용: ' + allowedStr, 'error');
      return;
    }

    // 파일 크기 제한 (500MB - 기본값, 실제로는 서버 설정 따름)
    var maxSize = 500 * 1024 * 1024;
    if (file.size > maxSize) {
      showToast('파일 크기는 500MB를 초과할 수 없습니다.', 'error');
      return;
    }

    // FormData 생성
    var formData = new FormData();
    formData.append('file', file);

    // 프로그레스 바 표시
    if (progressBar) {
      progressBar.classList.remove('d-none');
    }

    uploadArea.classList.add('uploading');

    // AJAX 업로드
    var xhr = new XMLHttpRequest();
    var apiBasePath = document.body.dataset.apiBasePath || '/api/streamix';

    xhr.open('POST', apiBasePath + '/files', true);

    // 업로드 진행률
    xhr.upload.onprogress = function(e) {
      if (e.lengthComputable && progressBarInner) {
        var percent = Math.round((e.loaded / e.total) * 100);
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
        var error = {};
        try {
          error = JSON.parse(xhr.responseText);
        } catch (e) {
          error.message = '업로드에 실패했습니다.';
        }
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
    var autoRefreshToggle = document.getElementById('autoRefresh');
    if (!autoRefreshToggle) return;

    var refreshInterval = null;

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
    var sessionsTable = document.getElementById('activeSessionsTable');
    if (!sessionsTable) return;

    // 실제 구현에서는 AJAX로 세션 데이터를 가져와 테이블 업데이트
    // 여기서는 간단히 시간만 업데이트
    var timeElements = sessionsTable.querySelectorAll('.session-duration');
    timeElements.forEach(function(el) {
      var startTime = new Date(el.dataset.startTime);
      var duration = formatDuration(Date.now() - startTime.getTime());
      el.textContent = duration;
    });
  }

  // ===== 사이드바 토글 (모바일) =====
  function initSidebarToggle() {
    var toggleBtn = document.getElementById('sidebarToggle');
    var sidebar = document.querySelector('.sidebar');
    var overlay = document.getElementById('sidebarOverlay');

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
      var bytes = parseInt(el.dataset.value, 10);
      el.textContent = formatFileSize(bytes);
    });

    // 날짜 포맷
    document.querySelectorAll('[data-format="datetime"]').forEach(function(el) {
      var date = new Date(el.dataset.value);
      el.textContent = formatDateTime(date);
    });

    // 시간 포맷
    document.querySelectorAll('[data-format="duration"]').forEach(function(el) {
      var ms = parseInt(el.dataset.value, 10);
      el.textContent = formatDuration(ms);
    });
  }

  // ===== 유틸리티 함수 =====
  function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';

    var units = ['B', 'KB', 'MB', 'GB', 'TB'];
    var k = 1024;
    var i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + units[i];
  }

  function formatDateTime(date) {
    if (!date || isNaN(date.getTime())) return '-';

    var year = date.getFullYear();
    var month = String(date.getMonth() + 1).padStart(2, '0');
    var day = String(date.getDate()).padStart(2, '0');
    var hours = String(date.getHours()).padStart(2, '0');
    var minutes = String(date.getMinutes()).padStart(2, '0');

    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
  }

  function formatDuration(ms) {
    if (!ms || ms < 0) return '-';

    var seconds = Math.floor(ms / 1000);
    var minutes = Math.floor(seconds / 60);
    var hours = Math.floor(minutes / 60);

    if (hours > 0) {
      return hours + '시간 ' + (minutes % 60) + '분';
    } else if (minutes > 0) {
      return minutes + '분 ' + (seconds % 60) + '초';
    } else {
      return seconds + '초';
    }
  }

  /**
   * 파일 타입 아이콘 클래스 반환
   */
  function getFileTypeIcon(fileType) {
    var config = FileTypes[fileType] || FileTypes.OTHER;
    return config.icon;
  }

  /**
   * 파일 타입 색상 클래스 반환
   */
  function getFileTypeColor(fileType) {
    var config = FileTypes[fileType] || FileTypes.OTHER;
    return config.color;
  }

  // ===== 토스트 메시지 =====
  function showToast(message, type) {
    var toastContainer = getOrCreateToastContainer();

    var toastEl = document.createElement('div');
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

    var toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    toastEl.addEventListener('hidden.bs.toast', function() {
      toastEl.remove();
    });
  }

  function getOrCreateToastContainer() {
    var container = document.getElementById('toastContainer');

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
    copyToClipboard: copyToClipboard,
    FileTypes: FileTypes,
    detectFileType: detectFileType,
    getFileTypeIcon: getFileTypeIcon,
    getFileTypeColor: getFileTypeColor
  };

})();