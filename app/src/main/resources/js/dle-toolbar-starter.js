/*
 * Universal self-healing DLE-toolbar button injector — single source for all extensions
 * that inject a button into Polarion's document (DLE) editor toolbar via
 * `scriptInjection.dleEditorHead`.
 *
 * Polarion (GWT) re-renders the toolbar sub-tree on actions like Save, which wipes out a
 * one-time injected element. This engine injects idempotently and re-injects via a
 * MutationObserver whenever the toolbar is re-rendered and the button disappears.
 *
 * The DLE toolbar selectors below are Polarion's own DOM and identical for every extension.
 * Extension-specific parts (button HTML, marker id) come in via create(config).
 *
 * Usage (thin extension starter.js loads this, then):
 *   const starter = window.GenericDleToolbarStarter.create({ markerId, alternateHtml, defaultHtml });
 *   starter.injectToolbar({ alternate: true });
 */
(function () {
    function injectStyles(id, href) {
        if (!top.document.getElementById(id)) {
            const link = top.document.createElement("link");
            link.id = id;
            link.rel = "stylesheet";
            link.type = "text/css";
            link.href = href;
            top.document.head.appendChild(link);
        }
    }

    function injectScript(id, src, type = "text/javascript") {
        if (!top.document.getElementById(id)) {
            const script = top.document.createElement("script");
            script.id = id;
            script.setAttribute("src", src);
            script.setAttribute("type", type);
            top.document.head.appendChild(script);
        }
    }

    // Polarion DLE toolbar DOM — same for all extensions.
    const ALTERNATE_TOOLBAR_SELECTOR = 'div.polarion-content-container div.polarion-Container div.polarion-dle-Container > div.polarion-dle-Wrapper > div.polarion-dle-RpcPanel > div.polarion-dle-MainDockPanel div.polarion-rte-ToolbarPanelWrapper table.polarion-dle-ToolbarPanel tr';
    const RICH_TEXT_AREA_SELECTOR = 'div.polarion-content-container div.polarion-Container div.polarion-dle-Container>div.polarion-dle-Wrapper>div.polarion-dle-RpcPanel>div.polarion-dle-MainDockPanel div.polarion-dle-SplitPanel:last-child .polarion-dle-RichTextArea';
    // Stable ancestor that survives the toolbar re-render — observed for re-injection.
    const STABLE_ANCESTOR_SELECTOR = 'div.polarion-content-container div.polarion-Container div.polarion-dle-Container';

    window.GenericDleToolbarStarter = {
        injectStyles: injectStyles,
        injectScript: injectScript,

        /**
         * @param config {{ markerId: string, alternateHtml: string, defaultHtml: string }}
         * @returns {{ injectToolbar: function }}
         */
        create: function (config) {
            // Idempotent: only inject if the toolbar exists and our button isn't already there.
            function inject(params) {
                if (top.document.getElementById(config.markerId)) {
                    return; // already present
                }
                if (params && params.alternate) {
                    const toolbarParent = top.document.querySelector(ALTERNATE_TOOLBAR_SELECTOR);
                    if (!toolbarParent) {
                        return; // toolbar not rendered (yet)
                    }
                    const toolbarContainer = top.document.createElement('td');
                    toolbarContainer.id = config.markerId;
                    toolbarContainer.innerHTML = config.alternateHtml;
                    toolbarParent.insertBefore(toolbarContainer, toolbarParent.querySelector('td[width="100%"]'));
                } else {
                    const documentFrame = top.document.querySelector(RICH_TEXT_AREA_SELECTOR);
                    if (!documentFrame) {
                        return;
                    }
                    const toolbarContainer = top.document.createElement('div');
                    toolbarContainer.id = config.markerId;
                    toolbarContainer.classList.add("dleToolBarContainer");
                    toolbarContainer.style.marginRight = "14px";
                    toolbarContainer.innerHTML = config.defaultHtml;
                    documentFrame.parentNode.parentNode.prepend(toolbarContainer);
                }
            }

            let observerSetUp = false;

            return {
                injectToolbar: function (params) {
                    inject(params);

                    // Re-inject when Polarion re-renders the toolbar (e.g. after Save) and our button disappears.
                    if (observerSetUp) {
                        return;
                    }
                    const anchor = top.document.querySelector(STABLE_ANCESTOR_SELECTOR) || top.document.body;
                    if (!anchor) {
                        return;
                    }
                    observerSetUp = true;
                    let scheduled = false;
                    const observer = new MutationObserver(function () {
                        // Cheap fast-path: button still present (or a re-inject already queued) → do nothing.
                        if (top.document.getElementById(config.markerId) || scheduled) {
                            return;
                        }
                        scheduled = true;
                        // Coalesce the burst of mutations during a re-render into a single re-inject.
                        requestAnimationFrame(function () {
                            scheduled = false;
                            inject(params);
                        });
                    });
                    observer.observe(anchor, { childList: true, subtree: true });
                }
            };
        }
    };
})();
