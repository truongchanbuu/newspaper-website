(() => {
  const form = document.querySelector<HTMLFormElement>('[data-sp-form]');
  if (!form) return;

  const input = form.querySelector<HTMLInputElement>('[data-sp-input]');

  // Auto-focus on page load (non-intrusive: skip if user already focused elsewhere)
  if (input && document.activeElement === document.body) {
    input.focus();
  }

  // 400ms debounce on text input → submit
  let debounceTimer: ReturnType<typeof setTimeout>;
  input?.addEventListener('input', () => {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => form.submit(), 400);
  });

  // Immediate submit when category changes
  form.querySelectorAll<HTMLSelectElement>('[data-sp-category]').forEach(el => {
    el.addEventListener('change', () => form.submit());
  });

  // Immediate submit when date preset changes
  form.querySelectorAll<HTMLInputElement>('[data-sp-preset]').forEach(el => {
    el.addEventListener('change', () => form.submit());
  });
})();
