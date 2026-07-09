import ensureSharedStyles from './ensureSharedStyles.js';

export default class SearchableDropdown {
    static COOKIE_PREFIX = 'searchable_dropdown_';
    static COOKIE_EXPIRY_DAYS = 30;
    static _idCounter = 0;

    constructor({
                    element = null,
                    selectContainer = null,
                    label = null,
                    changeListener = null,
                    multiselect = false,
                    items = [],
                    placeholder = '',
                    searchable = true,
                    rememberSelection = true,
                    preserveOptionClasses = false,
                    allowEmpty = false,
                    clearable = false,
                    editable = false,
                    inputFilter = null
                }) {
        if (!element && !selectContainer) {
            throw new Error('SearchableDropdown: element or selectContainer is required');
        }

        // Guarantee our shared control CSS is on the page (versioned), so a pane/popup that uses this
        // component is styled without depending on any extension's scriptInjection/starter.js.
        ensureSharedStyles();

        // Two construction modes:
        //  - "select" mode: wrap an existing native <select> (element) — the <select> stays the
        //    source of truth, is visually hidden, and the dropdown mirrors it.
        //  - "build" mode: render into a provided container <div> (selectContainer) and let the
        //    consumer populate it via addOption()/selectValue()/... This makes SearchableDropdown a
        //    drop-in replacement for the legacy CustomSelect (same API surface), so a single
        //    component covers both the native-<select> case and the programmatically-built case,
        //    including multi-select.
        this.buildMode = !element && !!selectContainer;

        if (this.buildMode) {
            this.originalElement = null;
            this.container =
                typeof selectContainer === 'string'
                    ? document.querySelector(selectContainer)
                    : selectContainer;
            if (!this.container) {
                throw new Error('SearchableDropdown: selectContainer not found');
            }
            this.isSelect = false;
            // Items in build mode carry their own selection state.
            this.items = items.map(i => ({selected: false, className: '', ...i}));
        } else {
            this.originalElement =
                typeof element === 'string'
                    ? document.querySelector(element)
                    : element;
            if (!this.originalElement) {
                throw new Error('SearchableDropdown: element not found');
            }
            this.isSelect = this.originalElement.tagName === 'SELECT';
            // If this element was already wrapped (e.g. a pane re-runs its dropdown init on save),
            // tear the previous instance down first so we don't stack duplicate containers/portals.
            if (this.originalElement._searchableDropdown) {
                this.originalElement._searchableDropdown.destroy();
            }
            this.items = this.isSelect
                ? this._extractItemsFromSelect(this.originalElement)
                : items;
        }

        this.multiselect = multiselect;
        // When true, the single-select dropdown may stay unselected: it does not auto-select the
        // first option and shows the placeholder (e.g. "Select…") until the user picks. Otherwise it
        // behaves like a native <select> and always keeps an option selected.
        this.allowEmpty = allowEmpty;
        // Optional clear (×) button on a single-select trigger: lets the user reset the selection
        // back to the placeholder (most useful together with allowEmpty). Off by default.
        this.clearable = clearable;
        this.label = label;
        this.changeListener = changeListener;
        this.rememberSelection =
            rememberSelection && !this.buildMode && !!this.originalElement.id;

        this.placeholder = placeholder;
        this.searchable = searchable;
        // Editable (creatable) single-select: the trigger becomes a typeable input, the popup search
        // box is dropped (the trigger itself is the filter), and a free value not in the list can be
        // committed on Enter/blur. Meaningless for multi-select. `inputFilter(value) => value`
        // sanitises what the user types (e.g. digits-only, Latin-only).
        this.editable = editable && !multiselect;
        this.inputFilter = typeof inputFilter === 'function' ? inputFilter : null;
        // The dedicated popup search box exists only when searchable AND not editable (an editable
        // dropdown filters from the trigger instead).
        this._hasSearchBox = this.searchable && !this.editable;
        // Mirror each source option's CSS class onto the rendered option (and, in select mode, the
        // trigger when selected) — e.g. a global-scope config is marked with the `parent` class,
        // which renders a small italic "global" marker. Always on in build mode so classes added
        // via addOption(...).classList surface.
        this.preserveOptionClasses = preserveOptionClasses || this.buildMode;
        this.isOpen = false;
        this.activeIndex = -1;
        // Unique id prefix for ARIA wiring (listbox + option ids, aria-controls/activedescendant).
        this._uid = 'sd-' + (++SearchableDropdown._idCounter);

        this._createContainer();
        this._render();
        this._bindEvents();
        // Reflect the <select>'s initial disabled state + title (need the trigger from _render).
        this._syncDisabled();
        this._syncTitle();
    }

    _getCookieKey() {
        return SearchableDropdown.COOKIE_PREFIX + this.originalElement.id;
    }

    _saveSelection(value) {
        if (!this.rememberSelection) {
            return;
        }
        const key = this._getCookieKey();
        if (value === null || value === undefined || value === '') {
            document.cookie = `${key}=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/`;
        } else {
            const expires = new Date();
            expires.setDate(expires.getDate() + SearchableDropdown.COOKIE_EXPIRY_DAYS);
            document.cookie = `${key}=${encodeURIComponent(value)}; expires=${expires.toUTCString()}; path=/`;
        }
    }

    _loadSelection() {
        if (!this.rememberSelection) {
            return null;
        }
        const key = this._getCookieKey();
        const match = document.cookie.match(new RegExp(`(?:^|; )${key}=([^;]*)`));
        return match ? decodeURIComponent(match[1]) : null;
    }

    _extractItemsFromSelect(select) {
        return Array.from(select.options)
            .filter((option) => {
                const style = window.getComputedStyle(option);
                return style.display !== 'none'; // Skip not visible items
            }).map(option => ({
                value: option.value,
                label: option.text,
                className: option.className,
                selected: option.selected,
                disabled: option.disabled,
                // Optional per-option icon: <option data-icon="/polarion/…/icon.svg">Label</option>
                icon: option.getAttribute('data-icon') || '',
                // Optional coloured tile behind the icon: <option data-icon-bg="#1a3a5c">Label</option>
                iconBg: option.getAttribute('data-icon-bg') || ''
            }));
    }

    _createContainer() {
        if (this.buildMode) {
            // Render straight into the consumer-provided container.
            this.container.classList.add('searchable-dropdown');
            if (this.editable) {
                this.container.classList.add('editable');
            }
            return;
        }

        this.container = document.createElement('div');
        this.container.className = 'searchable-dropdown';
        // An editable trigger is a free-text field, not a click-to-open combobox — drop the chevron.
        if (this.editable) {
            this.container.classList.add('editable');
        }

        this.originalElement.parentNode.insertBefore(
            this.container,
            this.originalElement.nextSibling
        );

        // Let ExtensionContext.setSelector/setValue sync this dropdown on programmatic value changes
        this.originalElement._searchableDropdown = this;

        // Width is configurable from the consumer side: inherit an explicit width from the original
        // element (e.g. <select style="width:142px">, or width:100%). When none is set, the width
        // falls back to the CSS default (.searchable-dropdown), which consumers can override per context.
        if (this.originalElement.style.width) {
            this.container.style.width = this.originalElement.style.width;
        }

        if (this.isSelect) {
            // Remember the original selection so destroy() can restore it (allowEmpty below may
            // clear it to -1, which would otherwise leak to a later re-wrap of the same <select>).
            this._originalSelectedIndex = this.originalElement.selectedIndex;

            // allowEmpty: a native <select> always auto-selects its first option — clear that so the
            // control starts unselected (placeholder shown) unless an option is explicitly marked
            // selected in the markup. The user must then choose.
            if (this.allowEmpty
                && !Array.from(this.originalElement.options).some(o => o.defaultSelected)) {
                this.originalElement.selectedIndex = -1;
            }
        }

        // Mirror the element's current visibility onto the container...
        this.container.style.display = this.originalElement.style.display || '';
        this.container.style.visibility = this.originalElement.style.visibility || '';

        // ...then visually hide the wrapped element (a <select>, or an <input> in editable mode)
        // WITHOUT touching display/visibility, so consumer-driven display/visibility changes
        // (displayIf, visibleIf, inline onchange handlers) stay observable and can be mirrored onto
        // the container. Remember the original inline style so destroy() can restore the element.
        this._originalElementCssText = this.originalElement.style.cssText;
        this.originalElement.style.position = 'absolute';
        this.originalElement.style.width = '1px';
        this.originalElement.style.height = '1px';
        this.originalElement.style.overflow = 'hidden';
        this.originalElement.style.opacity = '0';
        this.originalElement.style.pointerEvents = 'none';

        this._visibilityObserver = new MutationObserver(() => {
            this.container.style.display = this.originalElement.style.display || '';
            this.container.style.visibility = this.originalElement.style.visibility || '';
            this._syncDisabled();
            this._syncTitle();
        });
        this._visibilityObserver.observe(this.originalElement, {
            attributes: true,
            attributeFilter: ['style', 'disabled', 'title']
        });

        if (this.isSelect) {
            // Keep in sync when the <select>'s options are (re)populated dynamically
            this._optionsObserver = new MutationObserver(() => {
                this.items = this._extractItemsFromSelect(this.originalElement);
                this.syncFromElement();
                if (this.isOpen) {
                    this._renderOptions(this.items);
                }
            });
            this._optionsObserver.observe(this.originalElement, { childList: true });
        }
    }

    // Mirror the wrapped element's disabled state onto the container (dimmed + non-interactive via
    // the .disabled CSS class). Driven by the visibility MutationObserver when the attribute toggles.
    // The select trigger is readonly (click-to-open), so the .disabled class is enough; the editable
    // trigger is a real text <input>, so it must also be natively disabled or the user could still
    // type into it while the wrapped <input disabled> is hidden.
    _syncDisabled() {
        // Build-mode dropdowns have no wrapped element to mirror from.
        if (!this.originalElement) {
            return;
        }
        const disabled = this.originalElement.disabled;
        this.container.classList.toggle('disabled', disabled);
        this.trigger.setAttribute('aria-disabled', disabled ? 'true' : 'false');
        if (this.editable) {
            this.trigger.disabled = disabled;
        }
        if (disabled && this.isOpen) {
            this._close();
        }
    }

    // Mirror the wrapped <select>'s title (tooltip) onto the visible trigger.
    _syncTitle() {
        if (!this.isSelect) {
            return;
        }
        const title = this.originalElement.getAttribute('title');
        if (title) {
            this.trigger.setAttribute('title', title);
        } else {
            this.trigger.removeAttribute('title');
        }
    }

    _render() {
        // Trigger — the closed combobox. Single-select uses a read-only input; multi-select uses a
        // div that renders the selected values as removable chips and grows in height to show them.
        if (this.multiselect) {
            this.trigger = document.createElement('div');
            this.trigger.className = 'sd-trigger sd-trigger-multi';
            this.trigger.tabIndex = 0;
        } else {
            this.trigger = document.createElement('input');
            this.trigger.type = 'text';
            this.trigger.className = 'sd-trigger';
            this.trigger.placeholder = this.placeholder;
            this.trigger.readOnly = !this.editable;
            if (this.editable) {
                this.trigger.setAttribute('autocomplete', 'off');
            }
        }
        if (this.buildMode && this.container.id) {
            this.trigger.id = this.container.id + '_sd-trigger';
            if (this.label && !this.multiselect) {
                this.label.htmlFor = this.trigger.id;
            }
        }

        // Accessibility: expose the custom control as a combobox with a listbox popup so screen
        // readers announce it like a native <select> (role, expanded state, label, active option).
        this._listboxId = this._uid + '-listbox';
        this.trigger.setAttribute('role', 'combobox');
        this.trigger.setAttribute('aria-haspopup', 'listbox');
        this.trigger.setAttribute('aria-expanded', 'false');
        this.trigger.setAttribute('aria-controls', this._listboxId);
        const labelText = this._resolveLabelText();
        if (labelText) {
            this.trigger.setAttribute('aria-label', labelText);
        }

        // Popup menu (opens on click)
        this.optionsEl = document.createElement('div');
        this.optionsEl.className = 'options';

        if (this._hasSearchBox) {
            // Search row on top of the popup: search box + erase icon
            // (mirrors Polarion's JComboBox-SearchBox / JComboBox-EraseIcon)
            const searchRow = document.createElement('div');
            searchRow.className = 'search-row';

            this.searchInput = document.createElement('input');
            this.searchInput.type = 'text';
            this.searchInput.className = 'search-box';
            this.searchInput.setAttribute('role', 'searchbox');
            this.searchInput.setAttribute('aria-label', 'Search');
            this.searchInput.setAttribute('aria-controls', this._listboxId);
            this.searchInput.setAttribute('autocomplete', 'off');
            // size=1 keeps the input's intrinsic width tiny so it doesn't blow up the
            // popup's max-content width; flex:1 then stretches it to the list width.
            this.searchInput.size = 1;

            this.eraseIcon = document.createElement('span');
            this.eraseIcon.className = 'erase';
            this.eraseIcon.addEventListener('mousedown', e => {
                e.preventDefault();
                this.searchInput.value = '';
                this.activeIndex = -1;
                this._renderOptions(this.items);
                this.searchInput.focus();
            });

            searchRow.appendChild(this.searchInput);
            searchRow.appendChild(this.eraseIcon);
            this.optionsEl.appendChild(searchRow);
        }

        // Scrollable items list — the listbox referenced by the trigger's aria-controls.
        this.itemsEl = document.createElement('div');
        this.itemsEl.className = 'items';
        this.itemsEl.id = this._listboxId;
        this.itemsEl.setAttribute('role', 'listbox');
        if (this.multiselect) {
            this.itemsEl.setAttribute('aria-multiselectable', 'true');
        }
        this.optionsEl.appendChild(this.itemsEl);

        this.container.appendChild(this.trigger);

        // Single-select trigger is a read-only <input> (can't hold markup), so the selected option's
        // icon is overlaid as an absolutely-positioned image at its left edge (with .has-icon adding
        // room for it). Multi-select shows icons inside its chips instead.
        if (!this.multiselect) {
            this.triggerIcon = document.createElement('img');
            this.triggerIcon.className = 'sd-trigger-icon';
            this.triggerIcon.alt = '';
            this.triggerIcon.style.display = 'none';
            this.container.appendChild(this.triggerIcon);
        }

        // Optional clear (×) button — reset the single-select back to its placeholder. It sits left
        // of the chevron and is only visible while a value is selected (toggled via .has-value).
        if (this.clearable && !this.multiselect) {
            this.container.classList.add('clearable');
            this.clearButton = document.createElement('span');
            this.clearButton.className = 'sd-clear';
            this.clearButton.textContent = '×';
            this.clearButton.setAttribute('role', 'button');
            this.clearButton.setAttribute('aria-label', 'Clear selection');
            this.clearButton.addEventListener('mousedown', e => {
                // Don't let the trigger's open/close handler fire; just clear the selection.
                e.preventDefault();
                e.stopPropagation();
                this.selectItem(null);
            });
            this.container.appendChild(this.clearButton);
        }

        // The popup is rendered into a portal appended to <body> (position:fixed) rather than
        // nested in the container. This lets it escape any ancestor overflow clipping (narrow side
        // panels, scrollable modals) and its width is driven in JS from the trigger, so it always
        // shows in full. Hidden until opened.
        this.portal = document.createElement('div');
        this.portal.className = 'sd-portal';
        this.portal.style.display = 'none';
        this.portal.appendChild(this.optionsEl);
        document.body.appendChild(this.portal);

        if (this.multiselect) {
            this._updateTriggerFromSelection();
        } else if (!this.buildMode && this.isSelect && this.originalElement.value) {
            const selected = this.items.find(
                item => item.value === this.originalElement.value
            );
            if (selected) {
                this.trigger.value = selected.label;
            }
        } else if (this.editable && !this.buildMode && this.originalElement) {
            // Editable mode wraps a plain <input>; seed the trigger with its current free value.
            this.trigger.value = this.originalElement.value || '';
        }
        this._applyTriggerClass();
        this._refreshTriggerIcon();
        // Initialize clear-button visibility to match the starting selection (e.g. a dropdown that
        // renders with a pre-selected value shows the × immediately, not only after interaction).
        this._updateClearButton();
    }

    // Show/hide the selected option's icon on the single-select trigger (overlaid on the <input>).
    _refreshTriggerIcon() {
        if (this.multiselect || !this.triggerIcon) {
            return;
        }
        const item = this.items.find(i => i.value === this.value);
        if (item && item.icon) {
            this.triggerIcon.src = item.icon;
            this.triggerIcon.style.display = '';
            this.trigger.classList.add('has-icon');
            this._applyIconBg(this.triggerIcon, item.iconBg);
        } else {
            this.triggerIcon.style.display = 'none';
            this.trigger.classList.remove('has-icon');
        }
    }

    _bindEvents() {
        if (this.editable) {
            // Editable trigger: focus surfaces the suggestion list only when the field is empty (a
            // fresh pick). Clicking into a field that already holds a value just places the caret to
            // edit it — no dropdown of the value the user already chose. Typing then sanitises +
            // re-filters, and blur commits the free text. Clicking an option preventDefaults its
            // mousedown, so blur does not fire on a pick — only on a genuine focus-out.
            this.trigger.addEventListener('focus', () => {
                if (!this.trigger.value) {
                    this._syncEditablePopup();
                }
            });
            this.trigger.addEventListener('input', () => {
                if (this.inputFilter) {
                    const clean = this.inputFilter(this.trigger.value);
                    if (clean !== this.trigger.value) {
                        this.trigger.value = clean;
                    }
                }
                this.activeIndex = -1;
                this._syncEditablePopup();
                this._updateClearButton();
            });
            this.trigger.addEventListener('blur', () => {
                this._commitEditableValue();
                this._close();
            });
        } else {
            // Clicking the trigger opens/closes the popup
            this.trigger.addEventListener('mousedown', e => {
                e.preventDefault(); // critical: prevents focus-triggered reopen
                if (this.isOpen) {
                    this._close();
                } else {
                    this._open();
                }
            });
        }

        // Keyboard: an open popup is driven by arrows/enter/escape; a closed one opens on ArrowDown
        // (and, for the non-editable trigger where those keys carry no text, on Enter/Space too).
        this.trigger.addEventListener('keydown', e => {
            if (this.isOpen) {
                this._handleKeydown(e);
            } else if (this.editable && e.key === 'Enter') {
                // Editable, popup closed (free text that matches no suggestion): _handleEnter only
                // runs while the popup is open, so commit the typed value here. preventDefault stops
                // an enclosing <form> from submitting with the stale wrapped-<input> value.
                e.preventDefault();
                this._commitEditableValue();
            } else if (e.key === 'ArrowDown' || (!this.editable && (e.key === 'Enter' || e.key === ' '))) {
                e.preventDefault();
                this._open();
            }
        });

        if (this._hasSearchBox) {
            this.searchInput.addEventListener('input', () => {
                this.activeIndex = -1;
                this._renderOptions(this._filterItems(this.searchInput.value));
            });

            this.searchInput.addEventListener('keydown', e => this._handleKeydown(e));
        }

        // Clicking outside of the component closes the popup. The popup lives in a body-level
        // portal, so it must be checked separately from the container. Stored on the instance so
        // destroy() can unbind it.
        this._outsideClickHandler = e => {
            if (!this.container.contains(e.target) && !this.portal.contains(e.target)) {
                this._close();
            }
        };
        document.addEventListener('mousedown', this._outsideClickHandler);

        // Keep the trigger in sync when the underlying <select> value is changed externally
        // (e.g. a consumer sets select.value and dispatches 'change'). syncFromElement never
        // dispatches, so this cannot loop.
        if (this.isSelect) {
            this.originalElement.addEventListener('change', () => this.syncFromElement());
        }
    }

    _handleKeydown(e) {
        if (e.key === 'ArrowDown') {
            e.preventDefault();
            this._handleArrowDown();
        } else if (e.key === 'ArrowUp') {
            e.preventDefault();
            this._handleArrowUp();
        } else if (e.key === 'Enter') {
            e.preventDefault();
            this._handleEnter();
        } else if (e.key === 'Escape') {
            this._close();
        }
    }

    // Items whose label contains the query (case-insensitive). Empty query → all items. Shared by
    // the popup search box and the editable trigger.
    _filterItems(query) {
        const q = (query || '').toLowerCase();
        if (!q) {
            return this.items;
        }
        return this.items.filter(item => item.label.toLowerCase().includes(q));
    }

    // Editable mode only: reconcile the suggestion popup with the current trigger text. If nothing
    // matches, the popup closes rather than showing an empty "No matches" box — free text is allowed,
    // so a non-matching entry (e.g. the column "AA" against an A–Z hint list) is valid, not an error,
    // and there is simply nothing to suggest. Otherwise it opens (or re-renders) with the matches.
    _syncEditablePopup() {
        const matches = this._filterItems(this.trigger.value);
        if (matches.length === 0) {
            this._close();
        } else if (this.isOpen) {
            this._renderOptions(matches);
        } else {
            this._open();
        }
    }

    // Editable mode only: mirror the free text currently in the trigger onto the wrapped <input>
    // (editable wraps an <input>, not a <select>) and fire change so consumers pick it up. No-op if
    // the value is unchanged.
    _commitEditableValue() {
        // Only reached from the editable trigger's blur/Enter. Build-mode editable has no <input> to
        // mirror onto, so there is nothing to commit.
        if (!this.originalElement) {
            return;
        }
        if (this.originalElement.value !== this.trigger.value) {
            this.originalElement.value = this.trigger.value;
            this.originalElement.dispatchEvent(new Event('change', { bubbles: true }));
            this._fireChangeListener();
        }
    }

    _renderOptions(list) {
        this.itemsEl.innerHTML = '';
        this._visibleItems = list;

        // Empty state: with no options the popup would otherwise be an invisible zero-height box
        // (there is no search row to give it height in editable mode), so it looks like nothing
        // dropped. Show a placeholder row instead.
        if (list.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'sd-empty';
            empty.textContent = 'No matches';
            this.itemsEl.appendChild(empty);
        }

        list.forEach((item, index) => {
            const option = document.createElement('div');
            option.className = 'option';
            option.id = this._uid + '-opt-' + index;
            option.setAttribute('role', 'option');

            if (this.multiselect) {
                option.classList.add('multiselect-option');
                const checkbox = document.createElement('input');
                checkbox.type = 'checkbox';
                checkbox.checked = !!item.selected;
                checkbox.tabIndex = -1;
                option.appendChild(checkbox);
                if (item.icon) {
                    option.appendChild(this._createOptionIcon(item.icon, item.iconBg));
                }
                const labelSpan = document.createElement('span');
                labelSpan.className = 'option-label';
                labelSpan.textContent = item.label;
                option.appendChild(labelSpan);
            } else if (item.icon) {
                option.classList.add('has-icon');
                option.appendChild(this._createOptionIcon(item.icon, item.iconBg));
                const labelSpan = document.createElement('span');
                labelSpan.className = 'option-label';
                labelSpan.textContent = item.label;
                option.appendChild(labelSpan);
            } else {
                option.textContent = item.label;
            }

            if (this.preserveOptionClasses && item.className) {
                option.classList.add(...item.className.split(/\s+/).filter(Boolean));
            }

            if (item.disabled) {
                // Dimmed + non-interactive (CSS pointer-events:none); keyboard nav skips it and
                // selectItem() ignores it.
                option.classList.add('disabled');
                option.setAttribute('aria-disabled', 'true');
            }

            const isSelected = (this.multiselect || this.buildMode)
                ? !!item.selected
                : item.value === this.value;
            option.setAttribute('aria-selected', isSelected ? 'true' : 'false');

            if (index === this.activeIndex) {
                option.classList.add('active');
            }

            // Highlight follows the mouse — only one option is highlighted at a time
            option.addEventListener('mouseover', () => {
                this.activeIndex = index;
                this._paintActive();
            });

            option.addEventListener('mousedown', e => {
                e.preventDefault();
                // Stop the event before it reaches the document-level outside-click handler:
                // selectItem() may re-render the list (multi-select), detaching this option, after
                // which portal.contains(e.target) would be false and wrongly close the popup.
                e.stopPropagation();
                this.selectItem(item);
            });

            this.itemsEl.appendChild(option);
        });
        this._updateActiveDescendant();
    }

    _paintActive() {
        const options = this.itemsEl.children;
        for (let i = 0; i < options.length; i++) {
            options[i].classList.toggle('active', i === this.activeIndex);
        }
        this._updateActiveDescendant();
    }

    _createOptionIcon(src, iconBg) {
        const icon = document.createElement('img');
        icon.className = 'option-icon';
        icon.src = src;
        icon.alt = '';
        this._applyIconBg(icon, iconBg);
        return icon;
    }

    // Optional coloured tile behind an icon. The CSS class (.has-icon-bg) supplies the padding and
    // radius; the colour is per-option so it is set inline. A falsy bg clears any previous tile.
    _applyIconBg(icon, iconBg) {
        if (iconBg) {
            icon.classList.add('has-icon-bg');
            icon.style.backgroundColor = iconBg;
        } else {
            icon.classList.remove('has-icon-bg');
            icon.style.backgroundColor = '';
        }
    }

    // Accessible name for the combobox — from the passed label, the <select>'s aria-label, or the
    // associated <label for="…">.
    _resolveLabelText() {
        if (this.label && this.label.textContent) {
            return this.label.textContent.trim();
        }
        if (this.isSelect) {
            const ariaLabel = this.originalElement.getAttribute('aria-label');
            if (ariaLabel) {
                return ariaLabel;
            }
            const id = this.originalElement.id;
            if (id) {
                try {
                    const labelEl = document.querySelector('label[for="' + id + '"]');
                    if (labelEl && labelEl.textContent) {
                        return labelEl.textContent.trim();
                    }
                } catch (e) {
                    // id not usable as a selector — ignore
                }
            }
        }
        return '';
    }

    // Point aria-activedescendant (on whichever element has focus) at the highlighted option so
    // screen readers announce it during arrow-key navigation.
    _updateActiveDescendant() {
        const target = this._hasSearchBox ? this.searchInput : this.trigger;
        const active = this.activeIndex >= 0 ? this.itemsEl.children[this.activeIndex] : null;
        if (active && active.id) {
            target.setAttribute('aria-activedescendant', active.id);
        } else {
            target.removeAttribute('aria-activedescendant');
        }
    }

    // Next selectable (non-disabled) index in the given direction, wrapping around. Returns the
    // current index if every option is disabled.
    _nextEnabledIndex(start, direction) {
        const count = this._visibleItems.length;
        // From "no selection" (start < 0) the first step must land on the first item (Arrow Down)
        // or the last item (Arrow Up), mirroring a native <select>; normalise the start so it does.
        const from = start >= 0 ? start : (direction > 0 ? -1 : 0);
        for (let step = 1; step <= count; step++) {
            const idx = (((from + direction * step) % count) + count) % count;
            if (!this._visibleItems[idx].disabled) {
                return idx;
            }
        }
        return this.activeIndex;
    }

    _handleArrowDown() {
        if (!this._visibleItems || this._visibleItems.length === 0) {
            return;
        }
        this.activeIndex = this._nextEnabledIndex(this.activeIndex, 1);
        this._refreshActive();
    }

    _handleArrowUp() {
        if (!this._visibleItems || this._visibleItems.length === 0) {
            return;
        }
        this.activeIndex = this._nextEnabledIndex(this.activeIndex, -1);
        this._refreshActive();
    }

    _handleEnter() {
        const item = this._visibleItems[this.activeIndex];
        if (item) {
            this.selectItem(item);
        } else if (this.editable) {
            // No option highlighted → commit the free text in the trigger and close.
            this._commitEditableValue();
            this._close();
        }
    }

    _refreshActive() {
        // Keyboard navigation only moves the highlight — repaint the .active class on the existing
        // option nodes instead of rebuilding the whole list on every Arrow key.
        this._paintActive();
        this._scrollActiveIntoView();
    }

    _scrollActiveIntoView() {
        const active = this.itemsEl.querySelector('.option.active');
        if (active) {
            active.scrollIntoView({ block: 'nearest' });
        }
    }

    _open() {
        if (this.isOpen) {
            return;
        }
        // A disabled control doesn't open (CSS also sets pointer-events:none; this guards the
        // programmatic/keyboard paths).
        if (this.isSelect && this.originalElement.disabled) {
            return;
        }
        // Editable mode with no suggestion matching the current free text: nothing to show, so stay
        // closed instead of opening an empty popup (keyboard ArrowDown path; _syncEditablePopup
        // guards the focus/input paths).
        // The list actually rendered below: the filtered subset in editable mode, all items
        // otherwise. Compute it once so the pre-highlight index and the render agree.
        const list = this.editable ? this._filterItems(this.trigger.value) : this.items;
        if (this.editable && list.length === 0) {
            // Nothing matches the free text — stay closed (free-text entry, nothing to suggest).
            return;
        }
        this.isOpen = true;
        this.container.classList.add('open');
        this.trigger.setAttribute('aria-expanded', 'true');
        // Highlight the currently selected item on open (single-select only, and only if something
        // is actually selected). The index must be resolved against `list` — the array that gets
        // rendered — not the full items array, or a filtered editable list highlights the wrong row.
        this.activeIndex = (!this.multiselect && this.trigger.value)
            ? list.findIndex(item => item.value === this.value)
            : -1;
        if (this._hasSearchBox) {
            this.searchInput.value = '';
        }
        this._renderOptions(list);
        this._show();
        // Reposition the portal while open if the page scrolls or resizes (capture phase catches
        // scrolling in nested containers such as the document side panel).
        if (!this._repositionHandler) {
            this._repositionHandler = () => {
                if (this.isOpen) {
                    this._position();
                }
            };
        }
        window.addEventListener('scroll', this._repositionHandler, true);
        window.addEventListener('resize', this._repositionHandler);
        // Move focus so keyboard navigation works: to the search box if present, otherwise to the
        // trigger itself (the trigger's mousedown preventDefault suppressed the click-focus).
        if (this._hasSearchBox) {
            this.searchInput.focus();
        } else {
            this.trigger.focus();
        }
    }

    _close() {
        if (!this.isOpen) {
            return;
        }
        this.isOpen = false;
        this.container.classList.remove('open');
        this.trigger.setAttribute('aria-expanded', 'false');
        this.trigger.removeAttribute('aria-activedescendant');
        if (this._hasSearchBox) {
            this.searchInput.removeAttribute('aria-activedescendant');
        }
        this._hide();
        if (this._repositionHandler) {
            window.removeEventListener('scroll', this._repositionHandler, true);
            window.removeEventListener('resize', this._repositionHandler);
        }
        this.activeIndex = -1;
    }

    _show() {
        this.portal.style.display = 'block';
        this._position();
    }

    // Position the body-level portal under (or above) the trigger, matching the trigger's width so
    // the popup's min-width (trigger width + 35px) resolves correctly.
    _position() {
        const gap = 4;
        const rect = this.trigger.getBoundingClientRect();
        this.portal.style.position = 'fixed';
        // Keep the portal the trigger's width so the popup's min-width (trigger + 35px) resolves.
        this.portal.style.width = rect.width + 'px';
        this.optionsEl.classList.remove('dropup');
        // Default: popup aligned to the trigger's left edge, opening downward.
        this.portal.style.left = rect.left + 'px';
        this.portal.style.top = rect.bottom + 'px';

        const popupWidth = this.optionsEl.offsetWidth;
        const popupHeight = this.optionsEl.offsetHeight;
        const viewportWidth = window.innerWidth || document.documentElement.clientWidth;
        const viewportHeight = window.innerHeight || document.documentElement.clientHeight;

        // Horizontal: the popup can be wider than the trigger; if it would overflow the right edge,
        // shift it left so its right edge fits the viewport (like Polarion's native combo).
        if (rect.left + popupWidth > viewportWidth - gap) {
            this.portal.style.left = Math.max(gap, viewportWidth - popupWidth - gap) + 'px';
        }

        // Vertical: flip above the trigger when it wouldn't fit below.
        if (rect.bottom + popupHeight > viewportHeight && rect.top - popupHeight > 0) {
            this.optionsEl.classList.add('dropup');
            this.portal.style.top = rect.top + 'px';
        }
    }

    _hide() {
        this.portal.style.display = 'none';
        this.optionsEl.classList.remove('dropup');
    }

    _fireChangeListener() {
        if (typeof this.changeListener === 'function') {
            this.changeListener(this);
        }
    }

    _updateTriggerFromSelection() {
        if (this.multiselect) {
            this._renderChips();
            return;
        }
        const selected = this.items.filter(i => i.selected);
        this.trigger.value = selected.length ? selected[0].label : '';
        this._applyTriggerClass();
        this._refreshTriggerIcon();
        this._updateClearButton();
    }

    // Show the clear (×) button only while a single-select value is selected (toggled via the
    // container's .has-value class). No-op when the dropdown isn't clearable.
    _updateClearButton() {
        if (!this.clearButton) {
            return;
        }
        this.container.classList.toggle('has-value', !this.multiselect && !!this.trigger.value);
    }

    // Multi-select trigger content: one removable chip per selected value (empty → placeholder).
    _renderChips() {
        this.trigger.innerHTML = '';
        const selected = this.items.filter(i => i.selected);
        if (selected.length === 0) {
            const placeholder = document.createElement('span');
            placeholder.className = 'sd-placeholder';
            placeholder.textContent = this.placeholder;
            this.trigger.appendChild(placeholder);
            return;
        }
        selected.forEach(item => {
            const chip = document.createElement('span');
            chip.className = 'sd-chip';

            const label = document.createElement('span');
            label.className = 'sd-chip-label';
            label.textContent = item.label;
            chip.appendChild(label);

            const remove = document.createElement('span');
            remove.className = 'sd-chip-remove';
            remove.textContent = '×';
            remove.title = 'Remove';
            remove.addEventListener('mousedown', e => {
                e.preventDefault();
                // Don't let the trigger's open/close handler fire when removing a chip.
                e.stopPropagation();
                this.selectItem(item);
            });
            chip.appendChild(remove);

            this.trigger.appendChild(chip);
        });
    }

    /* ---------- Public API ---------- */

    restoreSelection() {
        const savedValue = this._loadSelection();
        if (savedValue !== null) {
            const item = this.items.find(i => i.value === savedValue);
            this.selectItem(item);
        } else {
            this.selectItem(null);
        }
    }

    refresh() {
        if (!this.buildMode) {
            this.items = this.isSelect
                ? this._extractItemsFromSelect(this.originalElement)
                : this.items;
        }
        this.restoreSelection();
    }

    // Tear down the instance: remove the body-level portal, disconnect observers, and unbind the
    // global listeners. Call this when a consumer rebuilds/replaces a pane that owns dropdowns so
    // portals and document/window listeners don't accumulate.
    destroy() {
        this._close();
        if (this.portal) {
            this.portal.remove();
        }
        if (this._visibilityObserver) {
            this._visibilityObserver.disconnect();
        }
        if (this._optionsObserver) {
            this._optionsObserver.disconnect();
        }
        if (this._outsideClickHandler) {
            document.removeEventListener('mousedown', this._outsideClickHandler);
        }
        if (this._repositionHandler) {
            window.removeEventListener('scroll', this._repositionHandler, true);
            window.removeEventListener('resize', this._repositionHandler);
        }
        if (this.originalElement && this.originalElement._searchableDropdown === this) {
            delete this.originalElement._searchableDropdown;
        }
        // In element mode we created the container as a sibling of the <select> and hid the
        // <select>; remove the container and restore the <select> so nothing is left behind.
        if (!this.buildMode) {
            if (this.container && this.container.parentNode) {
                this.container.remove();
            }
            if (this.originalElement && this._originalElementCssText !== undefined) {
                this.originalElement.style.cssText = this._originalElementCssText;
            }
            if (this.originalElement && this.isSelect && this._originalSelectedIndex !== undefined) {
                this.originalElement.selectedIndex = this._originalSelectedIndex;
            }
        }
    }

    // Sync the trigger display to the underlying <select>'s current value (no change event fired).
    // Called by ExtensionContext.setSelector/setValue after a programmatic value change.
    syncFromElement() {
        if (this.buildMode) {
            return;
        }
        if (this.multiselect) {
            this.items = this._extractItemsFromSelect(this.originalElement);
            this._updateTriggerFromSelection();
            return;
        }
        if (this.editable) {
            // Editable wraps a free-text <input>: the element's value is authoritative (and may be
            // text matching no item), so mirror it onto the trigger verbatim. Reading originalElement
            // (not the trigger) reflects programmatic updates; skipping the label lookup avoids
            // blanking a free value that isn't in the item list.
            this.trigger.value = this.originalElement.value || '';
            this._applyTriggerClass();
            this._refreshTriggerIcon();
            return;
        }
        if (this.isSelect && !this.allowEmpty && this.originalElement.selectedIndex === -1 && this.originalElement.options.length > 0) {
            // Native single-select left blank (value cleared) — keep the first option selected
            // (unless allowEmpty lets it stay unselected to show the placeholder).
            this.originalElement.selectedIndex = 0;
        }
        const value = this.isSelect ? this.originalElement.value : this.trigger.value;
        const item = this.items.find(i => i.value === value);
        this.trigger.value = item ? item.label : '';
        this._applyTriggerClass();
        this._refreshTriggerIcon();
    }

    _applyTriggerClass() {
        // The multi-select trigger is a chip container, not a value input — don't rewrite its class.
        if (this.multiselect || !this.preserveOptionClasses) {
            return;
        }
        const selected = this.buildMode
            ? this.items.find(i => i.selected)
            : this.items.find(i => i.value === this.value);
        this.trigger.className = 'sd-trigger' + (selected && selected.className ? ' ' + selected.className : '');
    }

    selectItem(item, preventClosing = false) {
        if (item && item.disabled) {
            return;
        }
        if (this.multiselect) {
            if (item) {
                item.selected = !item.selected;
                // Mirror the toggle onto the native <select multiple> so it stays the source of
                // truth for consumers that read select.selectedOptions.
                if (this.isSelect) {
                    const option = Array.from(this.originalElement.options)
                        .find(o => o.value === item.value);
                    if (option) {
                        option.selected = item.selected;
                    }
                    this.originalElement.dispatchEvent(new Event('change', { bubbles: true }));
                }
            }
            this._updateTriggerFromSelection();
            // Keep the popup open and reflect the toggled checkbox.
            this._renderOptions(this._visibleItems || this.items);
            this._saveSelection(null);
            this._fireChangeListener();
            return;
        }

        if (this.buildMode) {
            this.items.forEach(i => i.selected = !!item && i.value === item.value);
            this._updateTriggerFromSelection();
        } else {
            // An editable trigger shows the committed value (e.g. a revision number), not the label
            // (which may be "12345 — description"); a plain dropdown shows the label.
            this.trigger.value = item ? (this.editable ? String(item.value) : item.label) : '';
            this._applyTriggerClass();
            this._refreshTriggerIcon();
        }

        if (!preventClosing) {
            this._close();
        }

        if (this.isSelect) {
            if (item) {
                this.originalElement.value = item.value;
            } else {
                // Explicitly deselect rather than assigning value = null: setting an unmatched value
                // doesn't reliably clear a native <select>'s selection, which would leave the old
                // value readable by consumers even though the trigger shows the placeholder.
                this.originalElement.selectedIndex = -1;
            }
            this.originalElement.dispatchEvent(
                new Event('change', { bubbles: true })
            );
        } else if (this.editable && this.originalElement) {
            // Editable wraps a plain <input>: mirror the picked value onto it and fire change.
            this.originalElement.value = item ? String(item.value) : '';
            this.originalElement.dispatchEvent(new Event('change', { bubbles: true }));
        }

        this._saveSelection(item ? item.value : null);
        this._fireChangeListener();
        this._updateClearButton();
    }

    /* ---------- Build-mode API (drop-in for CustomSelect) ---------- */

    // Append a selectable option. Returns lightweight handles whose classList mutations are
    // mirrored onto the rendered option — this preserves the CustomSelect contract where callers do
    // `addOption(...).label.classList.add('parent')` to mark a global-scope config.
    addOption(value, text, icon, iconBg) {
        const item = {
            value,
            label: text !== undefined && text !== null ? text : value,
            className: '',
            selected: false,
            icon: icon || '',
            iconBg: iconBg || ''
        };
        this.items.push(item);
        // Single-select defaults to the first option (no empty/placeholder state), matching a
        // native <select>. A later selectValue() overrides it. Skipped when allowEmpty is set.
        if (!this.multiselect && !this.allowEmpty && this.items.length === 1) {
            item.selected = true;
            this._updateTriggerFromSelection();
        }
        if (this.isOpen) {
            this._renderOptions(this.items);
        }
        const proxy = this._optionClassHandle(item);
        return { checkbox: proxy, label: proxy };
    }

    _optionClassHandle(item) {
        const mutate = (transform) => {
            const classes = new Set((item.className || '').split(/\s+/).filter(Boolean));
            transform(classes);
            item.className = [...classes].join(' ');
            if (this.isOpen) {
                this._renderOptions(this._visibleItems || this.items);
            }
            this._applyTriggerClass();
        };
        return {
            classList: {
                add: (cls) => mutate(classes => classes.add(cls)),
                remove: (cls) => mutate(classes => classes.delete(cls)),
                toggle: (cls) => mutate(classes => classes.has(cls) ? classes.delete(cls) : classes.add(cls))
            }
        };
    }

    empty() {
        this.items = [];
        this.activeIndex = -1;
        if (this.multiselect) {
            // The multi-select trigger is a chip container div — re-render it so stale chips (and
            // their mousedown closures over removed items) are cleared and the placeholder shows.
            this._updateTriggerFromSelection();
        } else {
            this.trigger.value = '';
            this._applyTriggerClass();
            this._refreshTriggerIcon();
        }
        if (this.isOpen) {
            this._renderOptions(this.items);
        }
    }

    containsOption(optionValue) {
        return this.items.some(i => i.value === optionValue);
    }

    getSelectedValue() {
        if (this.multiselect) {
            return this.items.filter(i => i.selected).map(i => i.value);
        }
        if (this.buildMode) {
            const selected = this.items.find(i => i.selected);
            return selected ? selected.value : '';
        }
        return this.isSelect ? this.originalElement.value : this.trigger.value;
    }

    getSelectedText() {
        if (this.multiselect) {
            return this.items.filter(i => i.selected).map(i => i.label);
        }
        if (this.buildMode) {
            const selected = this.items.find(i => i.selected);
            return selected ? selected.label : '';
        }
        // Element mode (single) doesn't maintain items[].selected — derive the label from the live
        // value so getSelectedText() and getSelectedValue() never disagree.
        const item = this.items.find(i => i.value === this.value);
        return item ? item.label : '';
    }

    selectValue(value) {
        if (this.buildMode) {
            this.items.forEach(i => i.selected = i.value === value);
            // Single-select never stays empty — if the value matched nothing, fall back to the
            // first option (like a native <select>). Skipped when allowEmpty is set.
            if (!this.multiselect && !this.allowEmpty && this.items.length > 0 && !this.items.some(i => i.selected)) {
                this.items[0].selected = true;
            }
            this._updateTriggerFromSelection();
            if (this.isOpen) {
                this._renderOptions(this._visibleItems || this.items);
            }
            this._fireChangeListener();
        } else if (this.isSelect) {
            this.originalElement.value = value;
            // Native single-select: if the value didn't match any option, keep the first selected
            // rather than leaving the control blank. Skipped when allowEmpty is set.
            if (!this.multiselect && !this.allowEmpty && this.originalElement.selectedIndex === -1 && this.originalElement.options.length > 0) {
                this.originalElement.selectedIndex = 0;
            }
            this.syncFromElement();
        }
    }

    selectMultipleValues(values) {
        this.items.forEach(i => i.selected = !!values && values.includes(i.value));
        this._updateTriggerFromSelection();
        if (this.isOpen) {
            this._renderOptions(this._visibleItems || this.items);
        }
        this._fireChangeListener();
    }

    get id() {
        return this.buildMode
            ? (this.container ? this.container.id : undefined)
            : this.originalElement.id;
    }

    get value() {
        if (this.buildMode || this.multiselect) {
            return this.getSelectedValue();
        }
        return this.isSelect
            ? this.originalElement.value
            : this.trigger.value;
    }

    set value(val) {
        if (this.buildMode) {
            this.selectValue(val);
            return;
        }
        if (this.isSelect) {
            const item = this.items.find(i => i.value === val);
            if (item) {
                this.originalElement.value = val;
                this.trigger.value = item.label;
            }
        } else {
            this.trigger.value = val;
            if (this.editable && this.originalElement) {
                // Editable wraps a live <input>: mirror the programmatic value onto it too, so the
                // backing element isn't left stale until the next blur (and a later syncFromElement
                // won't revert the trigger to the old element value).
                this.originalElement.value = val;
            }
        }
        this._refreshTriggerIcon();
    }
}
