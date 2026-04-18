(() => {
  const form = document.querySelector<HTMLFormElement>('[data-sp-form]');
  if (!form) return;

  const input        = form.querySelector<HTMLInputElement>('[data-sp-input]');
  const categorySel  = form.querySelector<HTMLSelectElement>('[data-sp-category]');
  const presetSel    = form.querySelector<HTMLSelectElement>('[data-sp-preset-select]');
  const dateToggle   = form.querySelector<HTMLButtonElement>('[data-sp-date-toggle]');
  const dateRange    = form.querySelector<HTMLElement>('[data-sp-date-range]');
  const dateInputs   = dateRange?.querySelectorAll<HTMLInputElement>('input[type="date"]');

  // Lives outside the form; updated after each DOM swap
  let resultsEl = document.querySelector<HTMLElement>('[data-sp-results]');

  // ── Helpers ───────────────────────────────────────────────────────────────

  function collapseRange() {
    dateRange?.setAttribute('data-sp-collapsed', '');
    dateToggle?.setAttribute('aria-pressed', 'false');
  }

  function expandRange() {
    dateRange?.removeAttribute('data-sp-collapsed');
    dateToggle?.setAttribute('aria-pressed', 'true');
  }

  function isRangeOpen(): boolean {
    return dateRange ? !dateRange.hasAttribute('data-sp-collapsed') : false;
  }

  function buildUrl(): string {
    const data   = new FormData(form);
    const params = new URLSearchParams();
    data.forEach((v, k) => { if (String(v).trim()) params.set(k, String(v)); });
    const base = form.getAttribute('action') || window.location.pathname;
    const qs   = params.toString();
    return qs ? `${base}?${qs}` : base;
  }

  // ── AJAX result swap ──────────────────────────────────────────────────────

  let abortCtrl: AbortController | null = null;

  function showErrorBanner(message: string): void {
    if (!resultsEl) return;
    resultsEl.querySelector('.search-page__error')?.remove();
    const banner = document.createElement('p');
    banner.className = 'search-page__error';
    banner.setAttribute('role', 'alert');
    banner.textContent = message;
    resultsEl.prepend(banner);
  }

  async function loadResults(url: string): Promise<void> {
    if (!resultsEl) return;

    abortCtrl?.abort();
    abortCtrl = new AbortController();

    resultsEl.classList.add('search-page__results--loading');
    resultsEl.querySelector('.search-page__error')?.remove();
    try {
      const resp = await fetch(url, {
        headers: { Accept: 'text/html' },
        signal:  abortCtrl.signal,
      });
      if (!resp.ok) throw new Error('non-2xx');

      const html  = await resp.text();
      const doc   = new DOMParser().parseFromString(html, 'text/html');
      const fresh = doc.querySelector<HTMLElement>('[data-sp-results]');

      if (fresh && resultsEl.parentNode) {
        resultsEl.parentNode.replaceChild(fresh, resultsEl);
        resultsEl = fresh;
      }
      try {
        history.pushState(null, '', url);
      } catch {
        // iframe or cross-origin context (e.g. AEM author) — URL update not critical
      }
    } catch (e) {
      if (e instanceof Error && e.name === 'AbortError') return;
      showErrorBanner('Search failed. Please check your connection and try again.');
    } finally {
      resultsEl?.classList.remove('search-page__results--loading');
    }
  }

  function submitAsync(): void {
    loadResults(buildUrl());
  }

  // ── Auto-focus on page load ───────────────────────────────────────────────
  if (input && document.activeElement === document.body) {
    input.focus();
  }

  // ── Intercept form submit (search button / Enter key) ────────────────────
  form.addEventListener('submit', e => {
    e.preventDefault();
    submitAsync();
  });

  // ── 400ms debounce on text input ──────────────────────────────────────────
  let debounceTimer: ReturnType<typeof setTimeout>;
  input?.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(submitAsync, 400);
  });

  // ── Category change → immediate async update ──────────────────────────────
  categorySel?.addEventListener('change', submitAsync);

  // ── Preset select change ──────────────────────────────────────────────────
  presetSel?.addEventListener('change', () => {
    if (presetSel.value !== '') {
      collapseRange();
      dateInputs?.forEach(i => { i.value = ''; });
      submitAsync();
    } else {
      // "Any time" → reveal custom range, no submit until user enters dates
      expandRange();
    }
  });

  // ── Date input change → immediate async update ───────────────────────────
  // Mirrors category/preset auto-submit; clears any active preset so the
  // manual date takes precedence (mirrors server-side applyDatePreset logic).
  dateInputs?.forEach(dateInput => {
    dateInput.addEventListener('change', () => {
      if (presetSel) presetSel.value = '';
      submitAsync();
    });
  });

  // ── Custom range toggle button ────────────────────────────────────────────
  dateToggle?.addEventListener('click', () => {
    if (isRangeOpen()) {
      collapseRange();
      dateInputs?.forEach(i => { i.value = ''; });
      if (presetSel) presetSel.value = '';
      submitAsync();
    } else {
      if (presetSel) presetSel.value = '';
      expandRange();
      (dateRange?.querySelector<HTMLInputElement>('input[type="date"]'))?.focus();
    }
  });
})();
