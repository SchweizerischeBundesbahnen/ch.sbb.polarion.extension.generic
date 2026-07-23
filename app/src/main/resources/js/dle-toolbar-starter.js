/*
 * Universal self-healing Polarion-toolbar button injector — single source for all extensions
 * that inject a button into a native Polarion toolbar via `scriptInjection.*` configuration.
 *
 * Supported toolbars (the `target` config, see TARGETS below):
 *   - 'dleEditor' (default)      the document (DLE) editor toolbar, configured via
 *                                `scriptInjection.dleEditorHead`;
 *   - 'richPagePreview'          the Rich Page / Live Report toolbar in view mode (the one behind
 *                                the "Expand Tools" handle), configured via `scriptInjection.mainHead`.
 *
 * Polarion (GWT) re-renders the toolbar sub-tree on actions like Save, which wipes out a
 * one-time injected element. This engine injects idempotently and re-injects via a
 * MutationObserver whenever the toolbar is re-rendered and the button disappears. The Rich Page
 * toolbar additionally does not exist in the DOM at all until the user expands it — the same
 * observer picks it up the moment it is rendered.
 *
 * The toolbar selectors below are Polarion's own DOM and identical for every extension.
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

    // Polarion toolbar DOM per supported target — same for all extensions.
    //   rowSelector          the toolbar <tr> buttons are injected into (alternate/row mode);
    //   richTextAreaSelector (dleEditor only) anchor for the default above-the-editor mode;
    //   guardSelector        when set, injection only happens while this selector matches — used
    //                        to keep the Rich Page button out of the page's edit mode;
    //   stableAncestorSelector  ancestor that survives the toolbar re-render — observer anchor.
    const TARGETS = {
        dleEditor: {
            rowSelector: 'div.polarion-content-container div.polarion-Container div.polarion-dle-Container > div.polarion-dle-Wrapper > div.polarion-dle-RpcPanel > div.polarion-dle-MainDockPanel div.polarion-rte-ToolbarPanelWrapper table.polarion-dle-ToolbarPanel tr',
            richTextAreaSelector: 'div.polarion-content-container div.polarion-Container div.polarion-dle-Container>div.polarion-dle-Wrapper>div.polarion-dle-RpcPanel>div.polarion-dle-MainDockPanel div.polarion-dle-SplitPanel:last-child .polarion-dle-RichTextArea',
            stableAncestorSelector: 'div.polarion-content-container div.polarion-Container div.polarion-dle-Container'
        },
        richPagePreview: {
            rowSelector: 'div.polarion-rpe-MainPanel table.polarion-dle-ToolbarPanel tr',
            // Only the preview (view) mode of a Rich Page — never the page's edit-mode toolbar.
            guardSelector: 'div.polarion-rpe-MainPanel div.polarion-rpe-view',
            stableAncestorSelector: 'div.polarion-content-container'
        }
    };

    // The "Expand Tools" handle of a collapsed Rich Page toolbar (see autoExpandRichPageTools).
    const EXPAND_TOOLS_SELECTOR = 'div.polarion-rpe-expandTools';

    // Registry of live observers keyed by markerId, kept on the top window so it survives this
    // script being re-loaded each time the DLE editor is (re-)opened in Polarion's GWT SPA.
    // Re-using the key lets us disconnect the previous observer instead of accumulating them.
    const observerRegistry = top.__genericDleToolbarObservers || (top.__genericDleToolbarObservers = {});

    window.GenericDleToolbarStarter = {
        injectStyles: injectStyles,
        injectScript: injectScript,

        /**
         * @param config {{ markerId: string, alternateHtml: string, defaultHtml: string, target: string|undefined, order: number|undefined }}
         *   markerId      unique id set on the injected element; also the idempotency/dedup key.
         *   alternateHtml markup injected into the toolbar row when injectToolbar({alternate: true}).
         *   defaultHtml   markup injected above the rich-text area otherwise ('dleEditor' target only).
         *   target        which Polarion toolbar to inject into: 'dleEditor' (default) or
         *                 'richPagePreview'. The 'richPagePreview' target always injects into the
         *                 toolbar row (alternateHtml), regardless of the alternate flag.
         *
         *   SECURITY: alternateHtml / defaultHtml are written via innerHTML into the top Polarion
         *   frame, so they MUST be static, trusted markup. Never interpolate user-controlled data
         *   (document fields, work-item attributes, ...) into them without sanitizing it first.
         *
         * @returns {{ injectToolbar: function, destroy: function }}
         */
        create: function (config) {
            const target = TARGETS[config.target || 'dleEditor'];
            if (!target) {
                throw new Error(`GenericDleToolbarStarter: unknown target '${config.target}'.`);
            }

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
                if (target.guardSelector && !top.document.querySelector(target.guardSelector)) {
                    return; // guarded off (e.g. Rich Page is in edit mode)
                }
                if ((params && params.alternate) || !target.richTextAreaSelector) {
                    const toolbarParent = top.document.querySelector(target.rowSelector);
                    if (!toolbarParent) {
                        return; // toolbar not rendered (yet)
                    }
                    const toolbarContainer = top.document.createElement('td');
                    toolbarContainer.id = config.markerId;
                    // Polarion's own toolbar cells carry vertical-align: middle inline — match them
                    // so injected buttons line up with the native ones.
                    toolbarContainer.style.verticalAlign = 'middle';
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
                    const documentFrame = top.document.querySelector(target.richTextAreaSelector);
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
                    const anchor = top.document.querySelector(target.stableAncestorSelector) || top.document.body;
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
        },

        /**
         * Keep the Rich Page (Live Report) tools toolbar always expanded. Polarion renders it
         * collapsed behind an "Expand Tools" handle on every page open and does not persist the
         * expanded state, so this clicks the handle whenever it (re-)appears — on the initial page
         * load and on SPA navigation between pages.
         *
         * Idempotent across callers: a single shared observer per top window (several extensions
         * calling this results in one observer). There is no opposite-direction fighting to worry
         * about — Polarion offers no collapse control once the toolbar is expanded.
         */
        autoExpandRichPageTools: function () {
            if (top.__genericRpeAutoExpandObserver) {
                return;
            }
            function expand() {
                const handle = top.document.querySelector(EXPAND_TOOLS_SELECTOR);
                // GWT hides widgets with inline display:none — don't click an invisible handle.
                if (handle && handle.style.display !== 'none') {
                    handle.click();
                }
            }
            let scheduled = false;
            const observer = new MutationObserver(function () {
                if (scheduled) {
                    return;
                }
                scheduled = true;
                // Coalesce the burst of mutations during a page render into a single check.
                requestAnimationFrame(function () {
                    scheduled = false;
                    expand();
                });
            });
            top.__genericRpeAutoExpandObserver = observer;
            observer.observe(top.document.body, { childList: true, subtree: true });
            expand();
        }
    };
})();
