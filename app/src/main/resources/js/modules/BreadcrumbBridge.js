/*
 * Shared breadcrumb bridge for SPA / topic-page extensions.
 *
 * Polarion renders the app-header breadcrumb (`.polarion-ApplicationHeader-breadcrumb`) with GWT in
 * the parent window, but for an extension topic opened in the project navigation it shows a generic
 * "home" instead of the extension's own name. This script, injected into the parent document, swaps
 * that breadcrumb for the extension's own while the extension's URL is active. It replaces what used
 * to be a copy-pasted `<ext>-breadcrumb-bridge.js` per extension.
 *
 * It mirrors Polarion's own breadcrumb shape:
 *   - a ROOT topic   ->  [icon] Title                       (30px icon)
 *   - a SUB topic     ->  Parent  ›  [small icon] Title       (17px icon)
 *
 * NOTE: this is intentionally NOT an ES module (no import/export) — it is injected into the parent
 * page as a classic <script> so that `document.currentScript` is available to read its config.
 *
 * Two ways to use it:
 *   1. Classic-script auto-install — inject it with data-* attributes:
 *        data-marker (required) substring identifying the app in the URL/hash (e.g. "diff-tool")
 *        data-title  (required) breadcrumb title text
 *        data-parent (optional) parent topic name; when set, renders "parent › [icon] title"
 *        data-icon   (optional) icon URL shown left of the title
 *   2. Direct call — `window.SbbBreadcrumbBridge.install({ marker, title, parent, icon })`.
 *
 * Re-installing/re-injecting with the same marker UPDATES the title/parent/icon (so navigating
 * between an extension's sub-topics re-labels the breadcrumb) and returns the existing instance.
 * install() returns an { sync, update, destroy } handle. It never activates on Polarion's own
 * Administration pages (URL under `#/administration`), which render their breadcrumb correctly.
 */
(function () {
  function installBreadcrumbBridge(config) {
    config = config || {};
    var marker = config.marker;
    if (!marker || !config.title) {
      return null;
    }

    var installedFlag = '__sbbBreadcrumbBridge_' + marker;
    if (window[installedFlag]) {
      window[installedFlag].update(config);
      return window[installedFlag];
    }

    // Mutable config so re-injection (e.g. navigating to a sub-topic) can re-label without a fresh
    // install.
    var current = {
      title: config.title,
      parent: config.parent || '',
      icon: config.icon || ''
    };

    var customId = 'sbb-breadcrumb-' + marker;
    var styleId = 'sbb-breadcrumb-style-' + marker;

    // Hide the real GWT breadcrumb(s) with a stylesheet rule rather than a per-element inline style:
    // GWT re-renders and resets its own element's inline style, which would revive a JS display:none
    // and leave both breadcrumbs showing. A `!important` rule survives that. Our own replacement
    // carries data-sbb-bridge and is excluded from the rule.
    function enableHiding() {
      if (document.getElementById(styleId)) {
        return;
      }
      var style = document.createElement('style');
      style.id = styleId;
      style.textContent = '.polarion-ApplicationHeader-breadcrumb:not([data-sbb-bridge]){display:none !important;}';
      (document.head || document.documentElement).appendChild(style);
    }

    function disableHiding() {
      var style = document.getElementById(styleId);
      if (style && style.parentNode) {
        style.parentNode.removeChild(style);
      }
    }

    function findOriginal() {
      // Exclude our own replacement (built with the same GWT class, see buildBreadcrumb): otherwise,
      // while GWT has transiently removed its node, this would match the built one nested in `custom`
      // and sync() would hide it, blanking the breadcrumb.
      return document.querySelector('.polarion-ApplicationHeader-breadcrumb:not([data-sbb-bridge])');
    }

    // Build the replacement breadcrumb, via the DOM (no innerHTML), so the app-supplied title and
    // icon can never be interpreted as markup. Rebuilt whenever the config changes.
    var builtBreadcrumb = null;

    function appendIconAndTitle(wrap, iconSize) {
      if (current.icon) {
        var imagePanel = document.createElement('div');
        imagePanel.className = 'polarion-ApplicationHeader-imagePanel';
        var img = document.createElement('img');
        img.src = current.icon;
        img.alt = '';
        img.className = 'gwt-Image';
        img.style.width = iconSize;
        img.style.height = iconSize;
        imagePanel.appendChild(img);
        wrap.appendChild(imagePanel);
      }
      var titlePanel = document.createElement('div');
      titlePanel.className = 'polarion-ApplicationHeader-breadcrumbTitlePanel';
      var titleEl = document.createElement('div');
      titleEl.className = 'polarion-ApplicationHeader-breadcrumbTitle';
      titleEl.title = current.title;
      titleEl.textContent = current.title;
      titlePanel.appendChild(titleEl);
      wrap.appendChild(titlePanel);
    }

    function buildBreadcrumb() {
      var wrap = document.createElement('div');
      wrap.className = 'polarion-ApplicationHeader-breadcrumb';
      // Marks this as our replacement so findOriginal() never mistakes it for the GWT element.
      wrap.setAttribute('data-sbb-bridge', '');

      var isSub = !!current.parent;
      if (isSub) {
        // Parent name, then a separator, matching Polarion's "Parent › child" sub-topic breadcrumb.
        var parentPanel = document.createElement('div');
        parentPanel.className = 'polarion-ApplicationHeader-breadcrumbTitlePanel';
        var parentTitle = document.createElement('div');
        parentTitle.className = 'polarion-ApplicationHeader-breadcrumbTitle';
        parentTitle.title = current.parent;
        parentTitle.textContent = current.parent;
        parentPanel.appendChild(parentTitle);
        wrap.appendChild(parentPanel);

        var sep = document.createElement('div');
        sep.className = 'polarion-ApplicationHeader-breadcrumbSeparator';
        sep.textContent = '›'; // ›
        sep.style.margin = '0 8px';
        sep.style.opacity = '0.75';
        wrap.appendChild(sep);
      }

      // Sub-topics use a small icon (like Polarion's topic icons); roots use the large one.
      appendIconAndTitle(wrap, isSub ? '17px' : '30px');
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
      var hash = loc.hash || '';
      // Polarion's Administration renders the correct breadcrumb itself — never override it there
      // (the admin URL is `#/administration/<ext>/...`, which also contains our marker).
      if (hash.indexOf('/administration') !== -1 || loc.href.indexOf('/#/administration') !== -1) {
        return false;
      }
      return hash.indexOf(marker) !== -1 || loc.href.indexOf(marker) !== -1;
    }

    function sync() {
      if (!isAppUrl()) {
        // Not our page: stop hiding the GWT breadcrumb and tuck our replacement away.
        disableHiding();
        var existing = document.getElementById(customId);
        if (existing) {
          existing.style.display = 'none';
        }
        return;
      }

      // Our page: hide the real breadcrumb (the rule applies even before GWT renders it, so there is
      // no flash) and mount our replacement once there is a real breadcrumb to anchor it to.
      enableHiding();
      var original = findOriginal();
      if (!original) {
        return; // GWT hasn't rendered the breadcrumb yet — the observer re-runs sync when it does.
      }
      var custom = ensureCustom(original);
      if (!custom) {
        return;
      }
      if (!builtBreadcrumb) {
        builtBreadcrumb = buildBreadcrumb();
      }
      if (custom.firstChild !== builtBreadcrumb) {
        custom.textContent = '';
        custom.appendChild(builtBreadcrumb);
      }
      custom.style.display = '';
    }

    function update(cfg) {
      cfg = cfg || {};
      if (cfg.title) {
        current.title = cfg.title;
      }
      current.parent = cfg.parent || '';
      current.icon = cfg.icon || '';
      builtBreadcrumb = null; // force a rebuild with the new config on the next sync
      sync();
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
      update: update,
      destroy: function () {
        window.removeEventListener('popstate', sync);
        window.removeEventListener('hashchange', sync);
        observer.disconnect();
        // Remove the hiding rule so the GWT breadcrumb comes back (rather than leaving a blank slot).
        disableHiding();
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
      parent: script.getAttribute('data-parent') || '',
      icon: script.getAttribute('data-icon') || ''
    });
  }
})();
