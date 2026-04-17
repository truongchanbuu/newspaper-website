(() => {
  const form = document.querySelector<HTMLFormElement>('[data-sp-form]');
  if (!form) return;

  const input        = form.querySelector<HTMLInputElement>('[data-sp-input]');
  const categorySel  = form.querySelector<HTMLSelectElement>('[data-sp-category]');
  const presetSel    = form.querySelector<HTMLSelectElement>('[data-sp-preset-select]');
  const dateToggle   = form.querySelector<HTMLButtonElement>('[data-sp-date-toggle]');
  const dateRange    = form.querySelector<HTMLElement>('[data-sp-date-range]');
  const dateInputs   = dateRange?.querySelectorAll<HTMLInputElement>('input[type="date"]');

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

  // ── Auto-focus on page load ───────────────────────────────────────────────
  if (input && document.activeElement === document.body) {
    input.focus();
  }

  // ── 400ms debounce on text input → submit ─────────────────────────────────
  let debounceTimer: ReturnType<typeof setTimeout>;
  input?.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => form.submit(), 400);
  });

  // ── Category change → immediate submit ────────────────────────────────────
  categorySel?.addEventListener('change', () => form.submit());

  // ── Preset select change ──────────────────────────────────────────────────
  presetSel?.addEventListener('change', () => {
    if (presetSel.value !== '') {
      // Named preset chosen: collapse custom range, clear date inputs, submit
      collapseRange();
      dateInputs?.forEach(i => { i.value = ''; });
      form.submit();
    } else {
      // "Any time" chosen: reveal custom range so user can enter dates manually
      expandRange();
    }
  });

  // ── Custom range toggle button ────────────────────────────────────────────
  dateToggle?.addEventListener('click', () => {
    if (isRangeOpen()) {
      // Close: hide range, clear dates, reset preset to "Any time", submit to clear filter
      collapseRange();
      dateInputs?.forEach(i => { i.value = ''; });
      if (presetSel) presetSel.value = '';
      form.submit();
    } else {
      // Open: show range, clear any active preset (user will enter dates manually)
      if (presetSel) presetSel.value = '';
      expandRange();
      // Focus the first date input for quick entry
      (dateRange?.querySelector<HTMLInputElement>('input[type="date"]'))?.focus();
    }
  });
})();
