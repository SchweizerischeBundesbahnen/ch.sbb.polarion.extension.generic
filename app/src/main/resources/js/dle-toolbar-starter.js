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
 *   // optionally, when the caller tears down: starter.destroy();
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

    // Registry of live observers keyed by markerId, kept on the top window so it survives this
    // script being re-loaded each time the DLE editor is (re-)opened in Polarion's GWT SPA.
    // Re-using the key lets us disconnect the previous observer instead of accumulating them.
    const observerRegistry = top.__genericDleToolbarObservers || (top.__genericDleToolbarObservers = {});

    window.GenericDleToolbarStarter = {
        injectStyles: injectStyles,
        injectScript: injectScript,

        /**
         * @param config {{ markerId: string, alternateHtml: string, defaultHtml: string }}
         *   markerId      unique id set on the injected element; also the idempotency/dedup key.
         *   alternateHtml markup injected into the toolbar row when injectToolbar({alternate: true}).
         *   defaultHtml   markup injected above the rich-text area otherwise.
         *
         *   SECURITY: alternateHtml / defaultHtml are written via innerHTML into the top Polarion
         *   frame, so they MUST be static, trusted markup. Never interpolate user-controlled data
         *   (document fields, work-item attributes, ...) into them without sanitizing it first.
         *
         * @returns {{ injectToolbar: function, destroy: function }}
         */
        create: function (config) {
            // Stable left-to-right order across re-renders. Each button keeps the order it was
            // registered with (config.order — the config-execution order); re-injection inserts
            // before the first already-present button with a *higher* order. Buttons with distinct
            // orders keep their position regardless of which extension's observer re-fires first;
            // buttons sharing an order (e.g. callers that omit it → default 0) tie-break by
            // observer-fire order, so distinct orders are required for full determinism.
            const myOrder = (typeof config.order === 'number') ? config.order : 0;
            const orderByMarker = top.__genericDleToolbarOrder || (top.__genericDleToolbarOrder = {});
            orderByMarker[config.markerId] = myOrder;

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
                    const spacer = toolbarParent.querySelector('td[width="100%"]');
                    if (!spacer) {
                        // Polarion DOM changed (e.g. after an upgrade) — fall back to appending at the
                        // end of the row, but warn so the mislayout is diagnosable.
                        console.warn(`GenericDleToolbarStarter: reference cell td[width="100%"] not found for '${config.markerId}'; appending button at the end of the toolbar row.`);
                    }
                    // Keep a stable order: insert before the first already-present button whose order
                    // is higher than ours, otherwise before the spacer cell.
                    let reference = spacer;
                    for (const cell of toolbarParent.children) {
                        const cellOrder = orderByMarker[cell.id];
                        if (cellOrder !== undefined && cellOrder > myOrder) {
                            reference = cell;
                            break;
                        }
                    }
                    toolbarParent.insertBefore(toolbarContainer, reference);
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
            // The observer re-injects with the params of the latest injectToolbar() call.
            let lastParams;

            return {
                injectToolbar: function (params) {
                    lastParams = params;
                    inject(params);

                    // Set up the self-healing observer once per starter instance.
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
                            inject(lastParams);
                        });
                    });
                    // Disconnect any observer left over from a previous editor open for this markerId
                    // so observers don't accumulate across the SPA's editor open/close cycles.
                    if (observerRegistry[config.markerId]) {
                        observerRegistry[config.markerId].disconnect();
                    }
                    observerRegistry[config.markerId] = observer;
                    observer.observe(anchor, { childList: true, subtree: true });
                },

                // Stop self-healing and release the observer (for callers that have a teardown hook).
                destroy: function () {
                    if (observerRegistry[config.markerId]) {
                        observerRegistry[config.markerId].disconnect();
                        delete observerRegistry[config.markerId];
                    }
                    observerSetUp = false;
                }
            };
        }
    };
})();
