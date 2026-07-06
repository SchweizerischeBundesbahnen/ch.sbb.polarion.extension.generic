/*
 * Shared breadcrumb bridge for React/SPA extensions.
 *
 * Polarion renders the app-header breadcrumb (`.polarion-ApplicationHeader-breadcrumb`) with GWT in
 * the TOP window. An extension SPA lives in an iframe, so to show its own breadcrumb it injects this
 * classic script into the parent document; the script then swaps the GWT breadcrumb for the app's
 * own while the app's URL is active. Previously every extension shipped a copy-pasted
 * `<ext>-breadcrumb-bridge.js`; this is the single shared implementation.
 *
 * NOTE: this is intentionally NOT an ES module (no import/export) — it is injected into the parent
 * page as a classic <script> so that `document.currentScript` is available to read its config.
 *
 * Two ways to use it:
 *   1. Classic-script auto-install — inject it with data-* attributes; it configures itself from the
 *      injecting <script>:
 *        data-marker (required) substring identifying the app in the URL/hash (e.g. "xml-repair")
 *        data-title  (required) breadcrumb title text
 *        data-icon   (optional) icon URL shown left of the title
 *   2. Direct call — `window.SbbBreadcrumbBridge.install({ marker, title, icon })`.
 *
 * install() is idempotent per marker (re-installing returns the existing instance) and returns an
 * { sync, destroy } handle.
 */
(function () {
  function installBreadcrumbBridge(config) {
    config = config || {};
    var marker = config.marker;
    var title = config.title;
    var iconSrc = config.icon || '';
    if (!marker || !title) {
      return null;
    }

    var installedFlag = '__sbbBreadcrumbBridge_' + marker;
    if (window[installedFlag]) {
      return window[installedFlag];
    }

    var customId = 'sbb-breadcrumb-' + marker;

    function show(el) {
      if (el && el.style.display === 'none') {
        el.style.display = '';
      }
    }

    function hide(el) {
      if (el && el.style.display !== 'none') {
        el.style.display = 'none';
      }
    }

    function findOriginal() {
      // Exclude our own replacement (built with the same GWT class, see buildBreadcrumb): otherwise,
      // while GWT has transiently removed its node, this would match the built one nested in `custom`
      // and sync() would hide it, blanking the breadcrumb.
      return document.querySelector('.polarion-ApplicationHeader-breadcrumb:not([data-sbb-bridge])');
    }

    // Build the replacement breadcrumb once, via the DOM (no innerHTML), so the app-supplied title
    // and icon can never be interpreted as markup.
    var builtBreadcrumb = null;

    function buildBreadcrumb() {
      var wrap = document.createElement('div');
      wrap.className = 'polarion-ApplicationHeader-breadcrumb';
      // Marks this as our replacement so findOriginal() never mistakes it for the GWT element.
      wrap.setAttribute('data-sbb-bridge', '');

      if (iconSrc) {
        var imagePanel = document.createElement('div');
        imagePanel.className = 'polarion-ApplicationHeader-imagePanel';
        var img = document.createElement('img');
        img.src = iconSrc;
        img.alt = '';
        img.className = 'gwt-Image';
        img.style.width = '30px';
        img.style.height = '30px';
        imagePanel.appendChild(img);
        wrap.appendChild(imagePanel);
      }

      var titlePanel = document.createElement('div');
      titlePanel.className = 'polarion-ApplicationHeader-breadcrumbTitlePanel';
      var titleEl = document.createElement('div');
      titleEl.className = 'polarion-ApplicationHeader-breadcrumbTitle';
      titleEl.title = title;
      titleEl.textContent = title;
      titlePanel.appendChild(titleEl);
      wrap.appendChild(titlePanel);

      return wrap;
    }

    function ensureCustom(original) {
      var custom = document.getElementById(customId);
      if (!custom) {
        if (!original || !original.parentNode) {
          return null;
        }
        custom = document.createElement('div');
        custom.id = customId;
        original.insertAdjacentElement('afterend', custom);
        custom.style.font = 'inherit';
        custom.style.whiteSpace = 'nowrap';
        custom.style.display = 'none';
      }

      // Match the original's flex order so the replacement sits in the same slot.
      var originalOrder = window.getComputedStyle(original).order;
      if (originalOrder && originalOrder !== '0') {
        custom.style.order = originalOrder;
      }

      return custom;
    }

    function isAppUrl() {
      var loc = window.location;
      return (
        (loc.hash && loc.hash.indexOf(marker) !== -1) ||
        loc.href.indexOf(marker) !== -1
      );
    }

    function sync() {
      var original = findOriginal();
      if (!original) {
        return;
      }
      var custom = ensureCustom(original);
      if (!custom) {
        return;
      }

      if (isAppUrl()) {
        hide(original);
        show(custom);
        if (!builtBreadcrumb) {
          builtBreadcrumb = buildBreadcrumb();
        }
        if (custom.firstChild !== builtBreadcrumb) {
          custom.textContent = '';
          custom.appendChild(builtBreadcrumb);
        }
      } else {
        show(original);
        hide(custom);
      }
    }

    window.addEventListener('popstate', sync);
    window.addEventListener('hashchange', sync);

    // The original breadcrumb is rendered by GWT and may not exist yet, and GWT can re-render it
    // later without a URL change. So keep the observer connected and re-sync on every mutation
    // (sync() is a cheap no-op once the state already matches, so this cannot loop). Falls back to
    // documentElement in case the script runs before <body> is parsed.
    var observer = new MutationObserver(function () {
      sync();
    });
    observer.observe(document.body || document.documentElement, { childList: true, subtree: true });

    sync();

    var api = {
      sync: sync,
      destroy: function () {
        window.removeEventListener('popstate', sync);
        window.removeEventListener('hashchange', sync);
        observer.disconnect();
        var custom = document.getElementById(customId);
        if (custom && custom.parentNode) {
          custom.parentNode.removeChild(custom);
        }
        delete window[installedFlag];
      }
    };
    window[installedFlag] = api;
    return api;
  }

  // Expose for direct callers (and unit tests). In the browser globalThis === window.
  globalThis.SbbBreadcrumbBridge = { install: installBreadcrumbBridge };

  // Auto-install when injected as a classic <script> carrying data-* config.
  var script = (typeof document !== 'undefined') ? document.currentScript : null;
  if (script && script.getAttribute('data-marker')) {
    installBreadcrumbBridge({
      marker: script.getAttribute('data-marker'),
      title: script.getAttribute('data-title'),
      icon: script.getAttribute('data-icon') || ''
    });
  }
})();
