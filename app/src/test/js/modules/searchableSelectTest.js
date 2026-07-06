import { expect } from 'chai';
import { JSDOM } from 'jsdom';
import { createSearchableSelect } from '../../../main/resources/js/modules/searchableSelect.js';

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
});
