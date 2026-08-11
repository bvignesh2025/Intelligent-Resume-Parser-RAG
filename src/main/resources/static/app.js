/* ============================================================
   app.js — ResumeAI Frontend Logic
   ============================================================ */

/* ── State ───────────────────────────────────────────────── */
let selectedFile = null;
let skillTags = [];
let allResults = [];
let recentUploads = [];

/* ── Init ────────────────────────────────────────────────── */
document.addEventListener('DOMContentLoaded', () => {
  setupSkillInput();
  fetchStats();

  // Allow triggering search by pressing Enter in the query field
  document.getElementById('search-query').addEventListener('keydown', e => {
    if (e.key === 'Enter') runSearch();
  });
});

/* ── Tab switching ───────────────────────────────────────── */
function switchTab(tab) {
  document.getElementById('panel-search').style.display = tab === 'search' ? '' : 'none';
  document.getElementById('panel-upload').style.display = tab === 'upload' ? '' : 'none';
  document.getElementById('tab-search').classList.toggle('active', tab === 'search');
  document.getElementById('tab-upload').classList.toggle('active',  tab === 'upload');
}

/* ── Fetch Stats ─────────────────────────────────────────── */
async function fetchStats() {
  try {
    const res = await fetch('/api/resumes/stats');
    if (!res.ok) return;
    const data = await res.json();
    document.getElementById('stat-indexed').textContent = data.indexed ?? '—';
    document.getElementById('stat-pending').textContent = data.pending ?? '—';
  } catch {
    // Stats endpoint may not exist – silently skip
  }
}

/* ── Experience slider ───────────────────────────────────── */
function updateExpLabel(val) {
  document.getElementById('exp-display').textContent =
    val == 0 ? 'Any' : `${val}+ yr${val > 1 ? 's' : ''}`;
}

/* ── Skills Tag Input ────────────────────────────────────── */
function setupSkillInput() {
  const input = document.getElementById('skill-input');
  input.addEventListener('keydown', e => {
    if ((e.key === 'Enter' || e.key === ',') && input.value.trim()) {
      e.preventDefault();
      addSkillTag(input.value.trim().replace(/,/g, ''));
      input.value = '';
    }
    if (e.key === 'Backspace' && !input.value && skillTags.length) {
      removeSkillTag(skillTags.length - 1);
    }
  });
}

function addSkillTag(skill) {
  if (!skill || skillTags.includes(skill.toUpperCase())) return;
  skillTags.push(skill.toUpperCase());
  renderSkillTags();
}

function removeSkillTag(idx) {
  skillTags.splice(idx, 1);
  renderSkillTags();
}

function renderSkillTags() {
  const wrap = document.getElementById('skills-tag-wrap');
  const input = document.getElementById('skill-input');
  // Clear existing tags
  [...wrap.querySelectorAll('.skill-tag')].forEach(el => el.remove());
  // Re-insert before the input
  skillTags.forEach((tag, i) => {
    const el = document.createElement('span');
    el.className = 'skill-tag';
    el.innerHTML = `${tag} <span class="skill-tag-remove" onclick="removeSkillTag(${i})">✕</span>`;
    wrap.insertBefore(el, input);
  });
}

/* ── Search ──────────────────────────────────────────────── */
async function runSearch() {
  const query = document.getElementById('search-query').value.trim();
  if (!query) { showToast('Please enter a search query.', 'error'); return; }

  const minExp = parseInt(document.getElementById('exp-slider').value);
  const grid = document.getElementById('results-grid');

  // Loading state
  grid.innerHTML = `<div class="state-container"><div class="loader"></div><div class="state-title" style="margin-top:16px">Searching with hybrid AI…</div><div class="state-sub">Computing semantic vectors and keyword rankings</div></div>`;
  document.getElementById('results-count').textContent = '';
  document.getElementById('sort-select').style.display = 'none';
  document.getElementById('btn-search').disabled = true;

  try {
    const params = new URLSearchParams({ query });
    if (minExp > 0) params.append('minExperience', minExp);
    skillTags.forEach(s => params.append('skills', s));

    const res = await fetch(`/api/resumes/search?${params}`);
    if (!res.ok) throw new Error(`Server responded ${res.status}`);

    allResults = await res.json();
    renderResults(allResults);

    if (allResults.length > 0) {
      document.getElementById('sort-select').style.display = '';
    }
  } catch (err) {
    grid.innerHTML = `<div class="state-container"><div class="state-icon">⚠️</div><div class="state-title">Search failed</div><div class="state-sub">${err.message}</div></div>`;
    showToast('Search request failed. Is the server running?', 'error');
  } finally {
    document.getElementById('btn-search').disabled = false;
  }
}

/* ── Render Results ──────────────────────────────────────── */
function renderResults(results) {
  const grid = document.getElementById('results-grid');
  const countEl = document.getElementById('results-count');

  countEl.innerHTML = results.length
    ? `Found <strong>${results.length}</strong> candidate${results.length !== 1 ? 's' : ''}`
    : '';

  if (!results.length) {
    grid.innerHTML = `<div class="state-container"><div class="state-icon">🤷</div><div class="state-title">No candidates found</div><div class="state-sub">Try broadening your query, lowering the experience threshold, or removing skill filters.</div></div>`;
    return;
  }

  grid.innerHTML = results.map((c, idx) => buildCandidateCard(c, idx)).join('');

  // Animate score rings after render
  results.forEach((c, idx) => {
    const score = c.hybridScore ?? c.keywordScore ?? 0;
    animateRing(`ring-${idx}`, score);
  });
}

function buildCandidateCard(c, idx) {
  const score      = c.hybridScore ?? c.keywordScore ?? 0;
  const scoreLabel = (score * 100).toFixed(0) + '%';
  const section    = (c.matchedSection ?? 'SUMMARY').toUpperCase();
  const sectionClass = section.toLowerCase();
  const skills     = c.skills ?? [];
  const visibleSkills = skills.slice(0, 5);
  const extraCount = skills.length - visibleSkills.length;

  const skillChips = visibleSkills.map(s =>
    `<span class="skill-chip">${escHtml(s)}</span>`
  ).join('');
  const moreChip = extraCount > 0
    ? `<span class="skill-chip more">+${extraCount} more</span>`
    : '';

  const expBadge = c.yearsOfExperience != null
    ? `<div class="exp-badge">🕐 ${c.yearsOfExperience} yr${c.yearsOfExperience !== 1 ? 's' : ''} experience</div>`
    : '';

  const simScore = c.similarityScore != null ? `${(c.similarityScore * 100).toFixed(1)}%` : 'N/A';
  const kwScore  = c.keywordScore    != null ? `${(c.keywordScore  * 100).toFixed(1)}%` : 'N/A';

  const snippet = escHtml(c.matchedSnippet ?? '').substring(0, 340);

  return `
  <div class="candidate-card" style="animation-delay: ${idx * 0.055}s">
    <div class="candidate-main">
      <div class="candidate-name-row">
        <span class="candidate-name">${escHtml(c.candidateName ?? 'Unknown')}</span>
        <span class="section-badge ${sectionClass}">${section}</span>
      </div>
      <div class="candidate-email">✉️ ${escHtml(c.email ?? '')}</div>
      ${expBadge}
      <div class="candidate-snippet">${snippet}${snippet.length === 340 ? '…' : ''}</div>
      <div class="candidate-skills-row">${skillChips}${moreChip}</div>
    </div>

    <div class="candidate-meta">
      <!-- Score ring -->
      <div class="score-ring-wrap">
        <svg class="score-svg" width="72" height="72" viewBox="0 0 72 72">
          <circle class="score-track" cx="36" cy="36" r="30" />
          <circle class="score-fill" id="ring-${idx}" cx="36" cy="36" r="30"
            stroke-dasharray="0 188.5" />
        </svg>
        <div class="score-text">
          <span>${scoreLabel}</span>
          <span class="score-sublabel">match</span>
        </div>
      </div>

      <div class="score-breakdown">
        <div class="score-row">
          <span class="score-dot semantic"></span>
          <span>Semantic ${simScore}</span>
        </div>
        <div class="score-row">
          <span class="score-dot lexical"></span>
          <span>Keyword ${kwScore}</span>
        </div>
      </div>
    </div>
  </div>`;
}

function animateRing(id, score) {
  const el = document.getElementById(id);
  if (!el) return;
  const circumference = 2 * Math.PI * 30; // r=30
  const dash = Math.min(Math.max(score, 0), 1) * circumference;
  requestAnimationFrame(() => {
    el.style.strokeDasharray = `${dash} ${circumference}`;
  });
}

/* ── Sort Results ────────────────────────────────────────── */
function sortResults() {
  const mode = document.getElementById('sort-select').value;
  const sorted = [...allResults].sort((a, b) => {
    if (mode === 'hybrid')   return (b.hybridScore   ?? 0) - (a.hybridScore   ?? 0);
    if (mode === 'semantic') return (b.similarityScore ?? 0) - (a.similarityScore ?? 0);
    if (mode === 'lexical')  return (b.keywordScore   ?? 0) - (a.keywordScore   ?? 0);
    if (mode === 'exp')      return (b.yearsOfExperience ?? 0) - (a.yearsOfExperience ?? 0);
    return 0;
  });
  renderResults(sorted);
}

/* ── File Drag & Drop ────────────────────────────────────── */
function onDragOver(e) {
  e.preventDefault();
  document.getElementById('upload-zone').classList.add('drag-over');
}
function onDragLeave(e) {
  document.getElementById('upload-zone').classList.remove('drag-over');
}
function onDrop(e) {
  e.preventDefault();
  document.getElementById('upload-zone').classList.remove('drag-over');
  const file = e.dataTransfer.files[0];
  if (file) applyFile(file);
}
function onFileSelected(e) {
  const file = e.target.files[0];
  if (file) applyFile(file);
}

function applyFile(file) {
  if (!file.name.toLowerCase().endsWith('.pdf')) {
    showToast('Only PDF files are supported.', 'error');
    return;
  }
  if (file.size > 10 * 1024 * 1024) {
    showToast('File size must be under 10 MB.', 'error');
    return;
  }
  selectedFile = file;
  document.getElementById('file-name').textContent = file.name;
  document.getElementById('file-size').textContent = formatBytes(file.size);
  document.getElementById('file-preview').classList.add('visible');
}

function clearFile() {
  selectedFile = null;
  document.getElementById('file-input').value = '';
  document.getElementById('file-preview').classList.remove('visible');
  document.getElementById('upload-progress-bar').classList.remove('visible');
  document.getElementById('upload-progress-fill').style.width = '0%';
}

/* ── Upload Resume ───────────────────────────────────────── */
async function uploadResume() {
  const name  = document.getElementById('candidate-name').value.trim();
  const email = document.getElementById('candidate-email').value.trim();

  if (!selectedFile) { showToast('Please select a PDF file first.', 'error'); return; }
  if (!name)         { showToast('Please enter the candidate name.', 'error'); return; }
  if (!email)        { showToast('Please enter a valid email address.', 'error'); return; }

  const btn = document.getElementById('btn-upload');
  btn.disabled = true;
  btn.textContent = '⏳ Uploading…';

  const progressBar  = document.getElementById('upload-progress-bar');
  const progressFill = document.getElementById('upload-progress-fill');
  progressBar.classList.add('visible');

  // Simulate progress while waiting for server
  let prog = 0;
  const ticker = setInterval(() => {
    prog = Math.min(prog + Math.random() * 8, 88);
    progressFill.style.width = prog + '%';
  }, 280);

  try {
    const form = new FormData();
    form.append('file', selectedFile);
    form.append('name', name);
    form.append('email', email);

    const res = await fetch('/api/resumes/upload', { method: 'POST', body: form });
    clearInterval(ticker);

    if (!res.ok) {
      const msg = await res.text();
      throw new Error(msg || `Server error ${res.status}`);
    }

    const data = await res.json();
    progressFill.style.width = '100%';

    // Add to recent uploads
    recentUploads.unshift({ name, email, id: data.id, status: data.processingStatus });
    renderRecentUploads();

    showToast(`✅ "${name}" uploaded! Embedding generation has started in the background.`, 'success');
    clearFile();
    document.getElementById('candidate-name').value = '';
    document.getElementById('candidate-email').value = '';
    fetchStats();
  } catch (err) {
    clearInterval(ticker);
    showToast(`Upload failed: ${err.message}`, 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = '🚀 Upload & Ingest';
    setTimeout(() => progressBar.classList.remove('visible'), 1200);
  }
}

/* ── Recent Uploads ──────────────────────────────────────── */
function renderRecentUploads() {
  const container = document.getElementById('recent-uploads');
  if (!recentUploads.length) return;
  container.innerHTML = recentUploads.map(r => `
    <div class="candidate-card" style="margin-bottom: 10px; grid-template-columns: 1fr auto; padding: 16px 20px;">
      <div>
        <div class="candidate-name-row">
          <span class="candidate-name" style="font-size:0.95rem">${escHtml(r.name)}</span>
          <span class="section-badge projects">${escHtml(r.status ?? 'STAGED')}</span>
        </div>
        <div class="candidate-email">✉️ ${escHtml(r.email)}</div>
      </div>
      <div style="font-size:0.75rem; color:var(--text-muted); align-self:center; text-align:right">
        ${r.id ? r.id.substring(0, 8) + '…' : ''}
      </div>
    </div>
  `).join('');
}

/* ── Toast ───────────────────────────────────────────────── */
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
  toast.innerHTML = `<span>${icon}</span><span>${message}</span>`;
  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('toast-fade-out');
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

/* ── Utilities ───────────────────────────────────────────── */
function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

function formatBytes(bytes) {
  if (bytes < 1024) return bytes + ' B';
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
  return (bytes / 1024 / 1024).toFixed(2) + ' MB';
}
