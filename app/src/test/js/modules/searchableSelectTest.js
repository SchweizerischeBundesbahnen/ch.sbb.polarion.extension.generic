import { expect } from 'chai';
import { JSDOM } from 'jsdom';
import { createSearchableSelect, createEditableSelect, initSearchableDropdowns } from '../../../main/resources/js/modules/searchableSelect.js';

describe('createSearchableSelect', function () {
  let dom;

  beforeEach(function () {
    dom = new JSDOM('<!DOCTYPE html><html lang="en"><body></body></html>');
    global.window = dom.window;
    global.document = dom.window.document;
    global.MutationObserver = dom.window.MutationObserver;
  });

  afterEach(function () {
    delete global.window;
    delete global.document;
    delete global.MutationObserver;
  });

  function selectWith(values) {
    const sel = document.createElement('select');
    values.forEach((v) => {
      const o = document.createElement('option');
      o.value = v;
      o.textContent = v;
      sel.appendChild(o);
    });
    document.body.appendChild(sel);
    return sel;
  }

  it('wraps a <select> with the shared SearchableDropdown and shared defaults', function () {
    const sel = selectWith(['a', 'b']);
    const sd = createSearchableSelect(sel);
    expect(sd.originalElement).to.equal(sel);
    expect(sd.searchable).to.be.true;
    expect(sd.preserveOptionClasses).to.be.true;
    sd.destroy();
  });

  it('lets callers override the defaults', function () {
    const sel = selectWith(['x']);
    const sd = createSearchableSelect(sel, { allowEmpty: true, placeholder: 'Pick…', searchable: false });
    expect(sd.searchable).to.be.false;
    expect(sd.allowEmpty).to.be.true;
    expect(sd.placeholder).to.equal('Pick…');
    sd.destroy();
  });

  it('initSearchableDropdowns upgrades the single-select ids and the optional multi-select', function () {
    const s1 = selectWith(['a']); s1.id = 's1';
    const s2 = selectWith(['b']); s2.id = 's2';
    const m = selectWith(['c']); m.id = 'roles'; m.multiple = true;
    const ctx = { getElementById: (id) => document.getElementById(id) };

    initSearchableDropdowns(ctx, ['s1', 's2', 'missing'], 'roles');

    // Each wrapped <select> gets a sibling `.searchable-dropdown` container; a missing id is skipped.
    expect(s1.nextElementSibling.classList.contains('searchable-dropdown')).to.be.true;
    expect(s2.nextElementSibling.classList.contains('searchable-dropdown')).to.be.true;
    expect(m.nextElementSibling.classList.contains('searchable-dropdown')).to.be.true;
    expect(m.nextElementSibling.querySelector('.sd-trigger-multi')).to.exist; // rendered as multi-select
  });

  it('createEditableSelect wraps an <input> as an editable free-text dropdown', function () {
    const input = document.createElement('input');
    input.type = 'text';
    input.value = '';
    document.body.appendChild(input);
    const sd = createEditableSelect(input, {
      inputFilter: (v) => v.replace(/\D/g, ''),
      items: [{ value: '1', label: 'One' }]
    });
    expect(sd.editable).to.be.true;
    expect(sd.originalElement).to.equal(input);
    expect(typeof sd.inputFilter).to.equal('function');
    sd.destroy();
  });

  it('createEditableSelect mirrors the wrapped input disabled state onto the editable trigger', function () {
    const input = document.createElement('input');
    input.type = 'text';
    input.disabled = true;
    document.body.appendChild(input);
    const sd = createEditableSelect(input, { items: [{ value: '1', label: 'One' }] });
    // The editable trigger is a real text input, so a disabled wrapped <input> must disable it too
    // (otherwise the hidden input is disabled but the visible trigger stays typeable).
    expect(sd.trigger.disabled).to.be.true;
    expect(sd.container.classList.contains('disabled')).to.be.true;
    // Re-enabling the wrapped input propagates through _syncDisabled (the observer's handler).
    input.disabled = false;
    sd._syncDisabled();
    expect(sd.trigger.disabled).to.be.false;
    expect(sd.container.classList.contains('disabled')).to.be.false;
    sd.destroy();
  });

  it('initSearchableDropdowns inherits the shared defaults and passes options through', function () {
    const s = selectWith(['a', 'b']); s.id = 's';
    const ctx = { getElementById: (id) => document.getElementById(id) };
    initSearchableDropdowns(ctx, ['s'], null, { allowEmpty: true });
    const sd = s._searchableDropdown;
    expect(sd.preserveOptionClasses).to.be.true; // inherited from createSearchableSelect (not dropped)
    expect(sd.searchable).to.be.true;
    expect(sd.allowEmpty).to.be.true;            // passed through via the options arg
  });
});
