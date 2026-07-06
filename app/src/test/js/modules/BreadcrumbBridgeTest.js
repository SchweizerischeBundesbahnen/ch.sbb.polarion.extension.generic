import { expect } from 'chai';
import { JSDOM } from 'jsdom';

// The module is a classic (non-module) script that self-registers on globalThis; importing it for
// its side effect exposes window.SbbBreadcrumbBridge.install for the tests.
import '../../../main/resources/js/modules/BreadcrumbBridge.js';

const install = globalThis.SbbBreadcrumbBridge.install;

const ORIGINAL_CLASS = 'polarion-ApplicationHeader-breadcrumb';
const MARKER = 'xml-repair';
const CONFIG = { marker: MARKER, title: 'XML-Repair', icon: '/polarion/xml-repair-admin/icon.svg' };

describe('BreadcrumbBridge', function () {
  let dom;
  let handle;

  function boot(url) {
    dom = new JSDOM('<!DOCTYPE html><html lang="en"><head></head><body></body></html>', { url });
    global.window = dom.window;
    global.document = dom.window.document;
    global.MutationObserver = dom.window.MutationObserver;
  }

  function addOriginal() {
    const original = document.createElement('div');
    original.className = ORIGINAL_CLASS;
    original.textContent = 'Polarion';
    document.body.appendChild(original);
    return original;
  }

  afterEach(function () {
    if (handle) {
      handle.destroy();
      handle = null;
    }
    delete global.window;
    delete global.document;
    delete global.MutationObserver;
  });

  it('does nothing without a marker or title', function () {
    boot('https://host/polarion/xml-repair-app/');
    expect(install({})).to.equal(null);
    expect(install({ marker: MARKER })).to.equal(null);
    expect(install({ title: 'X' })).to.equal(null);
  });

  it('replaces the GWT breadcrumb while the app URL is active', function () {
    boot('https://host/polarion/xml-repair-app/');
    const original = addOriginal();
    handle = install(CONFIG);

    const custom = document.getElementById('sbb-breadcrumb-' + MARKER);
    expect(custom).to.exist;
    expect(original.previousElementSibling === null || custom.previousElementSibling).to.exist; // custom sits after original
    expect(custom.previousElementSibling).to.equal(original);
    expect(original.style.display).to.equal('none');
    expect(custom.style.display).to.not.equal('none');

    const titleEl = custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle');
    expect(titleEl.textContent).to.equal('XML-Repair');
    expect(titleEl.title).to.equal('XML-Repair');
    const img = custom.querySelector('img');
    expect(img).to.exist;
    expect(img.getAttribute('src')).to.equal('/polarion/xml-repair-admin/icon.svg');
  });

  it('keeps the GWT breadcrumb when the app URL is not active', function () {
    boot('https://host/polarion/some-other-app/');
    const original = addOriginal();
    handle = install(CONFIG);

    const custom = document.getElementById('sbb-breadcrumb-' + MARKER);
    expect(original.style.display).to.not.equal('none');
    expect(custom.style.display).to.equal('none');
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle')).to.equal(null);
  });

  it('omits the icon when none is configured', function () {
    boot('https://host/polarion/xml-repair-app/');
    addOriginal();
    handle = install({ marker: MARKER, title: 'XML-Repair' });
    const custom = document.getElementById('sbb-breadcrumb-' + MARKER);
    expect(custom.querySelector('img')).to.equal(null);
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle').textContent).to.equal('XML-Repair');
  });

  it('is idempotent per marker (re-install returns the same handle)', function () {
    boot('https://host/polarion/xml-repair-app/');
    addOriginal();
    handle = install(CONFIG);
    const again = install(CONFIG);
    expect(again).to.equal(handle);
  });

  it('waits for a late-rendered GWT breadcrumb via MutationObserver', async function () {
    boot('https://host/polarion/xml-repair-app/');
    handle = install(CONFIG);
    // Original not there yet → nothing injected.
    expect(document.getElementById('sbb-breadcrumb-' + MARKER)).to.equal(null);

    addOriginal();
    await new Promise((resolve) => setTimeout(resolve, 0)); // flush the observer callback

    const custom = document.getElementById('sbb-breadcrumb-' + MARKER);
    expect(custom).to.exist;
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle').textContent).to.equal('XML-Repair');
  });

  it('re-syncs on hashchange', function () {
    boot('https://host/polarion/shell/'); // no marker in the URL yet
    const original = addOriginal();
    handle = install(CONFIG);
    expect(original.style.display).to.not.equal('none');

    dom.window.location.hash = '#/xml-repair/scan';
    dom.window.dispatchEvent(new dom.window.Event('hashchange'));

    const custom = document.getElementById('sbb-breadcrumb-' + MARKER);
    expect(original.style.display).to.equal('none');
    expect(custom.style.display).to.not.equal('none');
  });

  it('re-applies the swap when GWT re-renders the breadcrumb (observer stays connected)', async function () {
    boot('https://host/polarion/xml-repair-app/');
    const original = addOriginal();
    handle = install(CONFIG);
    expect(original.style.display).to.equal('none');

    // GWT re-render: the old node is replaced by a fresh one that defaults to visible.
    original.remove();
    const fresh = addOriginal();
    await new Promise((resolve) => setTimeout(resolve, 0)); // flush the observer

    expect(fresh.style.display).to.equal('none');
    const custom = document.getElementById('sbb-breadcrumb-' + MARKER);
    expect(custom.style.display).to.not.equal('none');
  });

  it('never mistakes its own built breadcrumb for the GWT element', function () {
    boot('https://host/polarion/xml-repair-app/');
    const original = addOriginal();
    handle = install(CONFIG);
    const custom = document.getElementById('sbb-breadcrumb-' + MARKER);
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle').textContent).to.equal('XML-Repair');

    // GWT momentarily removes its node; a sync fires meanwhile. The built breadcrumb carries the
    // same class but is excluded via [data-sbb-bridge], so findOriginal() returns null and sync()
    // leaves the custom breadcrumb untouched instead of hiding its own content.
    original.remove();
    handle.sync();

    expect(custom.style.display).to.not.equal('none');
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle')).to.exist;
  });

  it('destroy() removes the custom breadcrumb and lets a fresh install run again', function () {
    boot('https://host/polarion/xml-repair-app/');
    addOriginal();
    const first = install(CONFIG);
    expect(document.getElementById('sbb-breadcrumb-' + MARKER)).to.exist;

    first.destroy();
    expect(document.getElementById('sbb-breadcrumb-' + MARKER)).to.equal(null);

    // Flag cleared → a new install is a fresh instance, not the destroyed one.
    handle = install(CONFIG);
    expect(handle).to.not.equal(first);
    expect(document.getElementById('sbb-breadcrumb-' + MARKER)).to.exist;
  });
});
