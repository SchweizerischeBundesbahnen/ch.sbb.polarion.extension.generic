import { GENERIC_BUILD_TIMESTAMP } from './generic-build-info.js';

/*
 * Guarantees the shared Polarion-styled control CSS (checkboxes, radios, inputs, searchable-dropdown)
 * is present on the page, as versioned <link>s in <head> — so any surface that renders our components
 * (document-properties form-extension side panels, export popups, admin pages) looks right on its own,
 * without depending on an exporter's `scriptInjection.*Head` → `starter.js` → `injectStyles(...)`.
 *
 * The <link> ids match the ones dle-toolbar-starter's `injectStyles` uses, so the two dedupe (only one
 * copy ever lands). Each <link> carries `data-generic-version` = this bundle's build timestamp; on a
 * clash we keep the copy with the HIGHER version and treat an unversioned copy (e.g. injected by an
 * older `starter.js`) as the lowest. Result: across a page assembled from extensions on different
 * generic versions, the NEWEST generic's CSS always wins — deterministically, regardless of load order.
 */

const STYLES = [
    ['generic-checkbox-styles', 'checkboxes.css'],
    ['generic-radios-styles', 'radios.css'],
    ['generic-inputs-styles', 'inputs.css'],
    ['generic-searchable-dropdown-styles', 'searchable-dropdown.css'],
    ['generic-buttons-styles', 'buttons.css'],
];

export default function ensureSharedStyles() {
    // Idempotent: on a repeat call our own links already carry our version, so the check below skips
    // them (mine <= existing). No extra guard needed.
    const cssBase = new URL('../../css/', import.meta.url).href;
    const mine = Number(GENERIC_BUILD_TIMESTAMP) || 0;

    STYLES.forEach(([id, file]) => {
        const existing = document.getElementById(id);
        if (existing) {
            const attr = existing.getAttribute('data-generic-version');
            // Keep what is already there only if it is versioned AND at least as new as us.
            // A versioned copy beats an unversioned one; among versioned, the higher timestamp wins.
            if (attr !== null && mine <= Number(attr)) {
                return;
            }
            existing.remove();
        }
        const link = document.createElement('link');
        link.id = id;
        link.rel = 'stylesheet';
        link.href = cssBase + file;
        link.setAttribute('data-generic-version', String(mine));
        document.head.appendChild(link);
    });
}
