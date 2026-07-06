import { expect } from 'chai';
import { JSDOM } from 'jsdom';

// The module is a classic (non-module) script that self-registers on globalThis; importing it for
// its side effect exposes window.SbbBreadcrumbBridge.install for the tests.
import '../../../main/resources/js/modules/BreadcrumbBridge.js';

const install = globalThis.SbbBreadcrumbBridge.install;

const ORIGINAL_CLASS = 'polarion-ApplicationHeader-breadcrumb';
const MARKER = 'xml-repair';
const CONFIG = { marker: MARKER, title: 'XML-Repair', icon: '/polarion/xml-repair-admin/icon.svg' };
const STYLE_ID = 'sbb-breadcrumb-style-' + MARKER;
const CUSTOM_ID = 'sbb-breadcrumb-' + MARKER;

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

  const styleTag = () => document.getElementById(STYLE_ID);
  const customEl = () => document.getElementById(CUSTOM_ID);

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

  it('hides the GWT breadcrumb via a stylesheet rule and mounts its own while active', function () {
    boot('https://host/polarion/xml-repair-app/');
    const original = addOriginal();
    handle = install(CONFIG);

    // The real breadcrumb is hidden by a !important rule (survives GWT resetting its inline style),
    // not by touching the element itself.
    expect(styleTag()).to.exist;
    expect(styleTag().textContent).to.contain(':not([data-sbb-bridge])');
    expect(styleTag().textContent).to.contain('display:none !important');
    expect(original.style.display).to.equal(''); // we never touch the GWT element's inline style

    const custom = customEl();
    expect(custom).to.exist;
    expect(custom.previousElementSibling).to.equal(original);
    expect(custom.style.display).to.not.equal('none');

    const titleEl = custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle');
    expect(titleEl.textContent).to.equal('XML-Repair');
    expect(titleEl.title).to.equal('XML-Repair');
    expect(custom.querySelector('img').getAttribute('src')).to.equal('/polarion/xml-repair-admin/icon.svg');
    // The built breadcrumb carries data-sbb-bridge so the hide rule never matches it.
    expect(custom.querySelector('[data-sbb-bridge]')).to.exist;
  });

  it('does not hide anything when the app URL is not active', function () {
    boot('https://host/polarion/some-other-app/');
    addOriginal();
    handle = install(CONFIG);

    expect(styleTag()).to.equal(null);
    expect(customEl()).to.equal(null);
  });

  it('omits the icon when none is configured', function () {
    boot('https://host/polarion/xml-repair-app/');
    addOriginal();
    handle = install({ marker: MARKER, title: 'XML-Repair' });
    const custom = customEl();
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

  it('starts hiding immediately and mounts once a late GWT breadcrumb appears', async function () {
    boot('https://host/polarion/xml-repair-app/');
    handle = install(CONFIG);
    // Hiding rule is applied even before GWT renders (no flash); the custom waits for an anchor.
    expect(styleTag()).to.exist;
    expect(customEl()).to.equal(null);

    addOriginal();
    await new Promise((resolve) => setTimeout(resolve, 0)); // flush the observer

    const custom = customEl();
    expect(custom).to.exist;
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbTitle').textContent).to.equal('XML-Repair');
  });

  it('re-syncs on hashchange', function () {
    boot('https://host/polarion/shell/'); // no marker in the URL yet
    addOriginal();
    handle = install(CONFIG);
    expect(styleTag()).to.equal(null);

    dom.window.location.hash = '#/xml-repair/scan';
    dom.window.dispatchEvent(new dom.window.Event('hashchange'));

    expect(styleTag()).to.exist;
    expect(customEl().style.display).to.not.equal('none');
  });

  it('keeps hiding across a GWT re-render (observer stays connected)', async function () {
    boot('https://host/polarion/xml-repair-app/');
    const original = addOriginal();
    handle = install(CONFIG);
    expect(styleTag()).to.exist;

    // GWT re-render: the old node is replaced by a fresh one. The stylesheet rule keeps hiding any
    // real breadcrumb regardless, and the custom stays mounted.
    original.remove();
    addOriginal();
    await new Promise((resolve) => setTimeout(resolve, 0)); // flush the observer

    expect(styleTag()).to.exist;
    expect(customEl().style.display).to.not.equal('none');
  });

  it('never mistakes its own built breadcrumb for the GWT element', function () {
    boot('https://host/polarion/xml-repair-app/');
    const original = addOriginal();
    handle = install(CONFIG);
    expect(customEl().querySelector('.polarion-ApplicationHeader-breadcrumbTitle').textContent).to.equal('XML-Repair');

    // GWT momentarily removes its node; a sync fires meanwhile. The built breadcrumb carries the
    // same class but is excluded via [data-sbb-bridge], so findOriginal() returns null and the
    // custom breadcrumb is left intact.
    original.remove();
    handle.sync();

    expect(styleTag()).to.exist;
    expect(customEl().style.display).to.not.equal('none');
    expect(customEl().querySelector('.polarion-ApplicationHeader-breadcrumbTitle')).to.exist;
  });

  it('does not activate on Polarion Administration pages', function () {
    // The admin URL is `#/administration/<ext>/...`, which also contains the marker — but Polarion
    // renders the correct breadcrumb there, so the bridge must stay out.
    boot('https://host/polarion/#/administration/xml-repair/settings');
    addOriginal();
    handle = install(CONFIG);
    expect(styleTag()).to.equal(null);
    expect(customEl()).to.equal(null);
  });

  it('renders a sub-topic as "parent › small-icon title"', function () {
    boot('https://host/polarion/#/project/x/xml-repair/details');
    addOriginal();
    handle = install({ marker: MARKER, title: 'Details', parent: 'XML-Repair', icon: '/i.svg' });

    const custom = customEl();
    const titles = custom.querySelectorAll('.polarion-ApplicationHeader-breadcrumbTitle');
    expect(titles.length).to.equal(2);
    expect(titles[0].textContent).to.equal('XML-Repair'); // parent segment
    expect(titles[1].textContent).to.equal('Details');    // current topic
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbSeparator')).to.exist;
    expect(custom.querySelector('img').style.width).to.equal('17px'); // small sub icon
  });

  it('root topics use the large icon and no parent segment', function () {
    boot('https://host/polarion/xml-repair-app/');
    addOriginal();
    handle = install(CONFIG);
    const custom = customEl();
    expect(custom.querySelectorAll('.polarion-ApplicationHeader-breadcrumbTitle').length).to.equal(1);
    expect(custom.querySelector('.polarion-ApplicationHeader-breadcrumbSeparator')).to.equal(null);
    expect(custom.querySelector('img').style.width).to.equal('30px');
  });

  it('re-installing with the same marker re-labels (sub-topic switch) and reuses the instance', function () {
    boot('https://host/polarion/xml-repair-app/');
    addOriginal();
    handle = install({ marker: MARKER, title: 'Root', icon: '/i.svg' });
    let titles = customEl().querySelectorAll('.polarion-ApplicationHeader-breadcrumbTitle');
    expect(titles.length).to.equal(1);
    expect(titles[0].textContent).to.equal('Root');

    const same = install({ marker: MARKER, title: 'Child', parent: 'Root', icon: '/i.svg' });
    expect(same).to.equal(handle); // same instance, updated in place
    titles = customEl().querySelectorAll('.polarion-ApplicationHeader-breadcrumbTitle');
    expect(titles.length).to.equal(2);
    expect(titles[0].textContent).to.equal('Root');  // parent
    expect(titles[1].textContent).to.equal('Child'); // new current topic
  });

  it('destroy() restores the GWT breadcrumb and lets a fresh install run again', function () {
    boot('https://host/polarion/xml-repair-app/');
    addOriginal();
    const first = install(CONFIG);
    expect(styleTag()).to.exist;
    expect(customEl()).to.exist;

    first.destroy();
    // Hiding rule gone (GWT breadcrumb visible again) and our element removed — no blank slot.
    expect(styleTag()).to.equal(null);
    expect(customEl()).to.equal(null);

    handle = install(CONFIG);
    expect(handle).to.not.equal(first);
    expect(styleTag()).to.exist;
    expect(customEl()).to.exist;
  });
});
