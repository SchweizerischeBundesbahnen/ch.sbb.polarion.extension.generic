import SearchableDropdown from './SearchableDropdown.js';

/*
 * Framework-agnostic core of the per-extension `SearchableSelect` React/Next wrappers.
 *
 * Every SPA duplicated the same tiny wrapper that upgrades a native <select> to the shared
 * SearchableDropdown; the only real difference between the copies was a hardcoded
 * `/<ext>-app/ui/generic/...` URL. This module owns the "how a <select> becomes the standard
 * Polarion combobox" contract (the SearchableDropdown options every extension uses), so the wrappers
 * shrink to a thin framework shim.
 *
 * It is served next to SearchableDropdown.js, so it resolves it via a plain relative import — no
 * `/<ext>-app/` segment to hardcode. Consumers load THIS module by deriving its URL from their own
 * runtime location (also dropping the hardcoded segment), e.g. from a React effect:
 *
 *   const base = window.location.pathname.replace(/\/ui\/.*$/, '/ui/generic/js/modules/');
 *   const { createSearchableSelect } = await import(base + 'searchableSelect.js');
 *   const sd = createSearchableSelect(selectEl, { allowEmpty: true, placeholder: 'Pick…' });
 *   // …later: sd.selectValue(value) to sync, sd.destroy() on unmount.
 */
export function createSearchableSelect(selectElement, options = {}) {
  return new SearchableDropdown({
    element: selectElement,
    // Defaults shared by every extension's combobox; callers override per case.
    searchable: true,
    rememberSelection: false,
    preserveOptionClasses: true,
    ...options
  });
}

/*
 * Editable (free-text / creatable) sibling of createSearchableSelect: wraps a text <input> so the
 * user can type a free value OR pick from a filtered suggestion list. The framework-agnostic core of
 * the React `SearchableInput` and the vanilla excel `ColumnInput`. Callers pass an `inputFilter`
 * (value => sanitised value) and `items`/`placeholder` as needed.
 */
export function createEditableSelect(inputElement, options = {}) {
  return new SearchableDropdown({
    element: inputElement,
    editable: true,
    rememberSelection: false,
    ...options
  });
}

/*
 * Convenience for the exporters (pdf / docx / strictdoc), which upgrade a fixed set of <select>s by
 * id — a batch of single-selects plus one optional `<select multiple>`. Shared here so the exporters
 * drop their copy-pasted (and drifted) `dropdown-utils.js`. `ctx` is the extension's ExtensionContext
 * (only `getElementById` is used).
 */
export function initSearchableDropdowns(ctx, singleIds, multiSelectId, options = {}) {
  (singleIds || []).forEach((id) => {
    const element = ctx.getElementById(id);
    if (element) {
      createSearchableSelect(element, { placeholder: '', ...options });
    }
  });
  if (multiSelectId) {
    const element = ctx.getElementById(multiSelectId);
    if (element) {
      createSearchableSelect(element, { placeholder: '', ...options, multiselect: true });
    }
  }
}
