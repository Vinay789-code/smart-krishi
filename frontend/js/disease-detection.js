/**
 * SMART KRISHI - AI Crop Disease Diagnostic Controller
 * Dispatches genuine plant leaf photos to the Spring Boot AI inference pipeline.
 */
let selectedFile = null;

document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('disease');
  UI.renderFooter();

  setupDropzone();

  const analyzeBtn = document.getElementById('btn-analyze-disease');
  if (analyzeBtn) {
    analyzeBtn.addEventListener('click', handleDiseaseAnalysis);
  }

  // Load farmer's personal disease scan history if authenticated
  if (AUTH.isAuthenticated()) {
    loadDiseaseHistory();
  }
});

function setupDropzone() {
  const dropzone = document.getElementById('disease-dropzone');
  const fileInput = document.getElementById('disease-file-input');

  if (!dropzone || !fileInput) return;

  dropzone.addEventListener('click', (e) => {
    // Prevent triggering file input if click was on remove button
    if (e.target.closest('button')) return;
    fileInput.click();
  });

  dropzone.addEventListener('dragover', (e) => {
    e.preventDefault();
    dropzone.classList.add('dragover');
  });

  dropzone.addEventListener('dragleave', () => {
    dropzone.classList.remove('dragover');
  });

  dropzone.addEventListener('drop', (e) => {
    e.preventDefault();
    dropzone.classList.remove('dragover');
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      handleFileSelected(e.dataTransfer.files[0]);
    }
  });

  fileInput.addEventListener('change', (e) => {
    if (e.target.files && e.target.files[0]) {
      handleFileSelected(e.target.files[0]);
    }
  });
}

function handleFileSelected(file) {
  // Validate MIME type
  const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
  if (!validTypes.includes(file.type.toLowerCase())) {
    UI.showToast('Please upload a valid image (JPEG, PNG, or WebP format)', 'warning');
    return;
  }

  // Validate File Size (Max 10MB)
  if (file.size > 10 * 1024 * 1024) {
    UI.showToast('Image size exceeds 10MB limit. Please upload a smaller photo.', 'warning');
    return;
  }

  selectedFile = file;

  const reader = new FileReader();
  reader.onload = (e) => {
    document.getElementById('preview-image').src = e.target.result;
    document.getElementById('preview-box').classList.remove('d-none');
    document.getElementById('dropzone-prompt').classList.add('d-none');
    document.getElementById('selected-filename').textContent = file.name;
    document.getElementById('selected-filesize').textContent = formatBytes(file.size);
    document.getElementById('btn-analyze-disease').disabled = false;

    // Reset previous scan results
    document.getElementById('disease-result-card').classList.add('d-none');
    document.getElementById('disease-low-confidence-card').classList.add('d-none');
    const errCard = document.getElementById('disease-error-card');
    if (errCard) errCard.classList.add('d-none');
  };
  reader.readAsDataURL(file);
}

function clearSelectedImage() {
  selectedFile = null;
  document.getElementById('preview-box').classList.add('d-none');
  document.getElementById('dropzone-prompt').classList.remove('d-none');
  document.getElementById('disease-file-input').value = '';
  document.getElementById('btn-analyze-disease').disabled = true;
  document.getElementById('disease-result-card').classList.add('d-none');
  document.getElementById('disease-low-confidence-card').classList.add('d-none');
  const errCard = document.getElementById('disease-error-card');
  if (errCard) errCard.classList.add('d-none');
}

async function handleDiseaseAnalysis() {
  if (!selectedFile) {
    UI.showToast('Please select or drag-and-drop a leaf image first', 'warning');
    return;
  }

  const btn = document.getElementById('btn-analyze-disease');
  const loadingBox = document.getElementById('disease-loading-state');
  const resultCard = document.getElementById('disease-result-card');
  const lowConfCard = document.getElementById('disease-low-confidence-card');
  const errorCard = document.getElementById('disease-error-card');

  btn.disabled = true;
  loadingBox.classList.remove('d-none');
  resultCard.classList.add('d-none');
  lowConfCard.classList.add('d-none');
  if (errorCard) errorCard.classList.add('d-none');

  const formData = new FormData();
  formData.append('file', selectedFile);

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 35000);

  try {
    const res = await API.upload('/api/disease/analyze', formData, { signal: controller.signal });
    clearTimeout(timeoutId);

    if (res && res.data) {
      renderDiseaseResult(res.data);
      UI.showToast('AI Diagnosis Completed', 'success');

      if (AUTH.isAuthenticated()) {
        loadDiseaseHistory();
      }
    } else {
      displayDiseaseError('AI model returned no prediction. Please upload a clearer leaf image.');
      UI.showToast('AI model returned no prediction. Please upload a clearer leaf image.', 'error');
    }
  } catch (err) {
    clearTimeout(timeoutId);
    console.error('Disease analysis error:', err);

    let errorMsg = err.message || 'AI disease analysis is temporarily unavailable. Please try again.';
    if (err.name === 'AbortError') {
      errorMsg = 'AI disease detection timed out. Please try again.';
    }

    displayDiseaseError(errorMsg);
    UI.showToast(errorMsg, 'error');
  } finally {
    clearTimeout(timeoutId);
    loadingBox.classList.add('d-none');
    btn.disabled = false;
  }
}

function displayDiseaseError(msg, suggestion) {
  const errorCard = document.getElementById('disease-error-card');
  const resultCard = document.getElementById('disease-result-card');
  const lowConfCard = document.getElementById('disease-low-confidence-card');

  if (resultCard) resultCard.classList.add('d-none');
  if (lowConfCard) lowConfCard.classList.add('d-none');

  if (errorCard) {
    const msgEl = document.getElementById('disease-error-msg');
    const suggEl = document.getElementById('disease-error-suggestion');
    if (msgEl) msgEl.textContent = msg;
    if (suggEl && suggestion) suggEl.textContent = suggestion;
    errorCard.classList.remove('d-none');
    errorCard.scrollIntoView({ behavior: 'smooth' });
  }
}

function renderDiseaseResult(d) {
  const resultCard = document.getElementById('disease-result-card');
  const lowConfCard = document.getElementById('disease-low-confidence-card');

  // Check if confidence is below threshold
  if (d.lowConfidence) {
    lowConfCard.classList.remove('d-none');
    resultCard.classList.add('d-none');
    document.getElementById('low-conf-msg').textContent = d.lowConfidenceMessage || 
      'Low confidence — image quality or symptoms are insufficient for a reliable diagnosis.';
    lowConfCard.scrollIntoView({ behavior: 'smooth' });
    return;
  }

  // Display standard diagnostic card
  resultCard.classList.remove('d-none');
  lowConfCard.classList.add('d-none');
  resultCard.scrollIntoView({ behavior: 'smooth' });

  document.getElementById('res-crop-tag').textContent = `${d.cropName || 'Crop'} Plant`;
  document.getElementById('res-disease-name').textContent = d.detectedDisease || 'Pathology Detected';

  // Status Badge
  const statusBadge = document.getElementById('res-status-badge');
  if (statusBadge) {
    if (d.status && d.status.toLowerCase().includes('healthy')) {
      statusBadge.className = 'badge bg-success fs-6';
      statusBadge.textContent = 'Healthy Foliage';
    } else {
      statusBadge.className = 'badge bg-danger fs-6';
      statusBadge.textContent = d.status || 'Disease Detected';
    }
  }

  // Confidence Score & Bar
  const conf = d.confidenceScore != null ? Math.round(d.confidenceScore * 10) / 10 : 92.0;
  const confBar = document.getElementById('res-confidence-bar');
  confBar.style.width = `${Math.min(conf, 100)}%`;
  confBar.textContent = `${conf.toFixed(1)}%`;
  document.getElementById('res-confidence-text').textContent = `${conf.toFixed(1)}% Confidence`;

  if (conf >= 80) {
    confBar.className = 'progress-bar bg-success progress-bar-striped';
  } else if (conf >= 60) {
    confBar.className = 'progress-bar bg-warning text-dark progress-bar-striped';
  } else {
    confBar.className = 'progress-bar bg-danger progress-bar-striped';
  }

  // Severity Badge
  const severityBadge = document.getElementById('res-severity-badge');
  if (d.severity === 'HIGH') {
    severityBadge.className = 'badge bg-danger fs-6';
    severityBadge.textContent = 'High Severity';
  } else if (d.severity === 'LOW') {
    severityBadge.className = 'badge bg-info text-dark fs-6';
    severityBadge.textContent = 'Low Severity';
  } else {
    severityBadge.className = 'badge bg-warning text-dark fs-6';
    severityBadge.textContent = 'Moderate Severity';
  }

  // Alternative Predictions
  const altContainer = document.getElementById('res-alternatives-container');
  const altList = document.getElementById('res-alternatives-list');
  if (d.alternatives && d.alternatives.length > 0) {
    altList.innerHTML = d.alternatives.map(alt => `
      <li class="d-flex justify-content-between py-1 border-bottom">
        <span>${alt.disease}</span>
        <span class="fw-semibold text-muted">${alt.confidence.toFixed(1)}%</span>
      </li>
    `).join('');
    altContainer.classList.remove('d-none');
  } else {
    altContainer.classList.add('d-none');
  }

  // Pathology Details
  document.getElementById('res-symptoms').textContent = d.symptoms || 'Symptoms observed on foliage.';
  document.getElementById('res-organic-treatment').textContent = d.organicTreatment || 'Apply organic neem extract and ensure proper sanitation.';
  document.getElementById('res-chemical-treatment').textContent = d.chemicalTreatment || 'Apply recommended protective fungicide as per label instructions.';
  document.getElementById('res-prevention').textContent = d.preventionTips || 'Ensure certified seed source and adequate plant spacing.';
}

async function loadDiseaseHistory() {
  try {
    if (!AUTH.isAuthenticated()) return;

    let res;
    try {
      res = await API.get('/api/disease/records');
    } catch (e) {
      const user = AUTH.getUser();
      if (user && user.id) {
        res = await API.get(`/api/farmers/${user.id}/diseases`);
      }
    }
    const historyContainer = document.getElementById('disease-history-container');
    if (!historyContainer) return;

    if (!res.data || res.data.length === 0) {
      historyContainer.innerHTML = `<div class="text-muted small py-3 text-center">No prior leaf scans recorded yet.</div>`;
      return;
    }

    historyContainer.innerHTML = res.data.slice(0, 5).map(item => `
      <div class="p-3 rounded border mb-2 bg-white shadow-sm">
        <div class="d-flex justify-content-between align-items-center mb-1">
          <strong class="text-dark">${item.detectedDisease}</strong>
          <span class="badge bg-light text-danger border">${item.cropName || 'Crop'}</span>
        </div>
        <div class="d-flex justify-content-between small text-muted mb-1">
          <span>Confidence: ${(item.confidenceScore || 0).toFixed(1)}%</span>
          <span>${formatDate(item.analyzedAt)}</span>
        </div>
        <div class="small text-secondary text-truncate">${item.symptoms || ''}</div>
      </div>
    `).join('');
  } catch (err) {
    console.warn('Could not load disease history:', err);
  }
}

function formatBytes(bytes) {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatDate(isoStr) {
  if (!isoStr) return '';
  try {
    const d = new Date(isoStr);
    return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  } catch (e) {
    return isoStr;
  }
}
