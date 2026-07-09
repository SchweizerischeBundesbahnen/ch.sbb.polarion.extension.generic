/*
 * NumericSpinner — replaces the native <input type="number"> spinner with two stacked caret
 * buttons matching the combobox chevron (see .sbb-number rules in inputs.css). The up caret is the
 * chevron flipped; the down caret is the chevron as-is. Buttons drive input.stepUp()/stepDown().
 *
 *   import { initNumericSpinners } from '../ui/generic/js/modules/NumericSpinner.js';
 *   initNumericSpinners(panelRoot);   // enhances every input[type=number] under panelRoot
 */
const CARET_UP = '<svg viewBox="4.4 6.27 7.2 4.2" aria-hidden="true"><path d="M4.4 10.47 L11.6 10.47 L8 6.27 Z" fill="currentColor"/></svg>';
const CARET_DOWN = '<svg viewBox="4.4 6.27 7.2 4.2" aria-hidden="true"><path d="M4.4 6.27 L11.6 6.27 L8 10.47 Z" fill="currentColor"/></svg>';

function step(input, dir) {
    if (input.disabled || input.readOnly) {
        return;
    }
    if (dir > 0) {
        input.stepUp();
    } else {
        input.stepDown();
    }
    input.dispatchEvent(new Event('input', { bubbles: true }));
    input.dispatchEvent(new Event('change', { bubbles: true }));
    input.focus();
}

export function enhanceNumericInput(input) {
    if (!input || input.dataset.sbbSpinner) {
        return;
    }
    input.dataset.sbbSpinner = '1';
    const wrap = document.createElement('span');
    wrap.className = 'sbb-number';
    input.parentNode.insertBefore(wrap, input);
    wrap.appendChild(input);

    const spin = document.createElement('span');
    spin.className = 'sbb-number-spin';
    const up = document.createElement('button');
    up.type = 'button';
    up.tabIndex = -1;
    up.setAttribute('aria-label', 'Increment');
    up.innerHTML = CARET_UP;
    const down = document.createElement('button');
    down.type = 'button';
    down.tabIndex = -1;
    down.setAttribute('aria-label', 'Decrement');
    down.innerHTML = CARET_DOWN;
    up.addEventListener('click', () => step(input, 1));
    down.addEventListener('click', () => step(input, -1));
    spin.appendChild(up);
    spin.appendChild(down);
    wrap.appendChild(spin);
}

export function initNumericSpinners(root = document) {
    root.querySelectorAll('input[type="number"]:not([data-sbb-spinner])').forEach(enhanceNumericInput);
}

export default initNumericSpinners;
