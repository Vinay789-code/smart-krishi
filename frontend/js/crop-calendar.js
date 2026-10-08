/**
 * SMART KRISHI - Crop Growth Calendar & Advisory
 * Phenological Timeline Controller
 */

document.addEventListener('DOMContentLoaded', () => {
  UI.renderNavbar('crop-calendar');
  UI.renderFooter();

  // Set default sowing date to today
  const sowDateInput = document.getElementById('cal-sow-date');
  if (sowDateInput && !sowDateInput.value) {
    const today = new Date();
    sowDateInput.value = today.toISOString().split('T')[0];
  }

  const form = document.getElementById('calendar-filter-form');
  if (form) {
    form.addEventListener('submit', handleCalendarSubmit);
  }

  // Load default calendar on startup
  loadCropCalendar('Wheat', 'All');
});

function selectCropCalendar(crop, state) {
  const cropEl = document.getElementById('cal-crop');
  const stateEl = document.getElementById('cal-state');
  if (cropEl) cropEl.value = crop;
  if (stateEl) stateEl.value = state || 'All';

  loadCropCalendar(crop, state);
}

function handleCalendarSubmit(e) {
  if (e && e.preventDefault) e.preventDefault();

  const crop = document.getElementById('cal-crop').value;
  const state = document.getElementById('cal-state').value;
  loadCropCalendar(crop, state);
}

async function loadCropCalendar(crop, state) {
  const loadingEl = document.getElementById('cal-loading');
  const contentEl = document.getElementById('cal-content');
  const btn = document.getElementById('btn-load-calendar');

  try {
    if (loadingEl) loadingEl.classList.remove('d-none');
    if (contentEl) contentEl.classList.add('d-none');
    if (btn) {
      btn.disabled = true;
      btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Loading...';
    }

    const params = (state && state !== 'All') ? { state } : null;
    const response = await API.get(`/api/crop-calendar/${encodeURIComponent(crop)}`, params);

    if (loadingEl) loadingEl.classList.add('d-none');
    if (contentEl) contentEl.classList.remove('d-none');

    const data = (response && response.data) ? response.data : response;
    renderCalendar(data);
  } catch (error) {
    console.error('Error fetching crop calendar:', error);
    if (loadingEl) loadingEl.classList.add('d-none');
    UI.showToast(error.message || 'Unable to fetch crop calendar.', 'danger');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '<i class="bi bi-calendar-event me-1"></i>View Calendar';
    }
  }
}

/**
 * Safely normalizes text and decodes any mojibake characters,
 * ensuring clean rendering of degree symbols, dashes, and quotes.
 */
function cleanText(str) {
  if (str === null || str === undefined) return '';
  if (typeof str !== 'string') return String(str);
  return str
    .replace(/â€“|â€”|\u00e2\u20ac\u201c|\u00e2\u20ac\u201d|\u2013|\u2014/g, '–')
    .replace(/Â°C|\u00c2\u00b0C/g, '°C')
    .replace(/Â|\u00c2/g, '')
    .replace(/â€˜|â€™|\u00e2\u20ac\u02dc|\u00e2\u20ac\u2122/g, "'")
    .replace(/â€œ|â€\x9d|\u00e2\u20ac\u0153|\u00e2\u20ac\u009d/g, '"')
    .replace(/(\d+|[a-zA-Z]+)\s*\?\?\s*(\d+|[a-zA-Z]+)/g, '$1 – $2')
    .trim();
}

function renderCalendar(data) {
  if (!data) return;

  // Header Details
  const cropEl = document.getElementById('cal-crop-name');
  if (cropEl) cropEl.textContent = cleanText(data.crop) || 'Crop';

  const seasonBadge = document.getElementById('cal-season-badge');
  if (seasonBadge) seasonBadge.textContent = `${cleanText(data.season) || 'Annual'} Season`;

  const botNameEl = document.getElementById('cal-botanical-name');
  if (botNameEl) botNameEl.textContent = cleanText(data.scientificName || data.botanicalName) || 'Scientific classification';

  const durationEl = document.getElementById('cal-duration-days');
  if (durationEl) {
    const rawDuration = cleanText(data.totalDurationDays);
    durationEl.textContent = rawDuration || (data.durationDays ? `${data.durationDays} Days` : 'N/A');
  }

  const stagesCountEl = document.getElementById('cal-total-stages');
  if (stagesCountEl) stagesCountEl.textContent = `${data.stages ? data.stages.length : 0} Stages`;

  // Agronomic parameters
  const tempEl = document.getElementById('cal-ideal-temp');
  if (tempEl) tempEl.textContent = cleanText(data.idealTemperature) || 'N/A';

  const rainEl = document.getElementById('cal-rainfall');
  if (rainEl) rainEl.textContent = cleanText(data.rainfallRequirement) || 'N/A';

  const soilEl = document.getElementById('cal-soil');
  if (soilEl) soilEl.textContent = cleanText(data.soilSuitability) || 'N/A';

  // Regional Notice Alert / General Notice
  const alertEl = document.getElementById('cal-state-alert');
  const noticeEl = document.getElementById('cal-state-notice');
  const noticeText = cleanText(data.stateSpecificNotice || data.notice);
  if (noticeText && noticeEl) {
    noticeEl.textContent = noticeText;
    alertEl?.classList.remove('d-none');
  } else {
    alertEl?.classList.add('d-none');
  }

  // General Advisories
  const advCard = document.getElementById('cal-advisories-card');
  const advList = document.getElementById('cal-advisories-list');
  if (advCard && advList) {
    const advisories = Array.isArray(data.generalAdvisories) ? data.generalAdvisories : [];
    if (advisories.length > 0) {
      advList.innerHTML = advisories.map(adv => `
        <li class="d-flex align-items-start gap-2">
          <i class="bi bi-chevron-right text-warning mt-1 flex-shrink-0"></i>
          <span>${cleanText(adv)}</span>
        </li>
      `).join('');
      advCard.classList.remove('d-none');
    } else {
      advCard.classList.add('d-none');
    }
  }

  // Render Stages
  const stepsContainer = document.getElementById('crop-timeline-steps');
  if (!stepsContainer || !data.stages) return;

  const totalStages = data.stages.length;

  stepsContainer.innerHTML = data.stages.map((stage, idx) => {
    const stageNum = stage.stageOrder || (idx + 1);
    const stageTitle = cleanText(stage.stageName) || `Stage ${stageNum}`;
    const timeWindow = cleanText(stage.timeWindow) || 'Standard timeline';
    const irrigation = cleanText(stage.irrigationGuidance) || 'Standard moisture maintenance.';
    const fertilizer = cleanText(stage.fertilizerGuidance) || 'No fertilizer required during this stage.';

    const activities = Array.isArray(stage.farmingActivities)
      ? stage.farmingActivities
      : (stage.farmingActivities ? [stage.farmingActivities] : []);

    const precautions = Array.isArray(stage.precautions)
      ? stage.precautions
      : (stage.precautions ? [stage.precautions] : []);

    return `
      <div class="timeline-step">
        <div class="timeline-icon">${stageNum}</div>
        <div class="timeline-card">
          <div class="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-2">
            <div>
              <h5 class="fw-bold text-dark mb-0">${stageTitle}</h5>
              <div class="small text-muted">Stage ${stageNum} of ${totalStages}</div>
            </div>
            <div class="badge bg-light text-dark border px-3 py-2 fs-6">
              <i class="bi bi-clock-history text-success me-1"></i>Time: ${timeWindow}
            </div>
          </div>

          <!-- Irrigation & Fertilizer Highlights -->
          <div class="row g-2 my-2">
            <div class="col-md-6">
              <div class="p-2 rounded bg-light border h-100">
                <div class="small fw-bold text-primary mb-1">
                  <i class="bi bi-droplet-half me-1"></i>Irrigation Window:
                </div>
                <div class="small text-secondary">${irrigation}</div>
              </div>
            </div>
            <div class="col-md-6">
              <div class="p-2 rounded bg-light border h-100">
                <div class="small fw-bold text-success mb-1">
                  <i class="bi bi-box-seam me-1"></i>Nutrient Application:
                </div>
                <div class="small text-secondary">${fertilizer}</div>
              </div>
            </div>
          </div>

          <!-- Activities & Precautions Accordion-style layout -->
          <div class="row g-3 mt-1">
            <!-- Recommended Activities -->
            <div class="col-md-6">
              <div class="fw-semibold small text-dark mb-1"><i class="bi bi-list-task text-primary me-1"></i>Key Activities:</div>
              <ul class="list-unstyled small d-flex flex-column gap-1 mb-0 text-secondary">
                ${activities.length > 0 ? activities.map(act => `
                  <li class="d-flex align-items-start gap-2">
                    <i class="bi bi-check2 text-success mt-1 flex-shrink-0"></i>
                    <span>${cleanText(act)}</span>
                  </li>
                `).join('') : '<li class="text-muted small">No specific activities listed for this stage.</li>'}
              </ul>
            </div>

            <!-- Precautions & Warnings -->
            <div class="col-md-6">
              <div class="fw-semibold small text-dark mb-1"><i class="bi bi-shield-exclamation text-danger me-1"></i>Field Precautions:</div>
              <ul class="list-unstyled small d-flex flex-column gap-1 mb-0 text-secondary">
                ${precautions.length > 0 ? precautions.map(prec => `
                  <li class="d-flex align-items-start gap-2">
                    <i class="bi bi-exclamation-circle text-danger mt-1 flex-shrink-0"></i>
                    <span>${cleanText(prec)}</span>
                  </li>
                `).join('') : '<li class="text-muted small">Standard field precautions apply.</li>'}
              </ul>
            </div>
          </div>
        </div>
      </div>
    `;
  }).join('');
}
