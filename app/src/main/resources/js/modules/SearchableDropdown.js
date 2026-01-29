export default class SearchableDropdown {
    static COOKIE_PREFIX = 'searchable_dropdown_';
    static COOKIE_EXPIRY_DAYS = 30;

    constructor({
                    element,
                    items = [],
                    placeholder = 'Search...',
                    searchable = true,
                    rememberSelection = true
                }) {
        if (!element) {
            throw new Error('SearchableDropdown: element is required');
        }

        this.originalElement =
            typeof element === 'string'
                ? document.querySelector(element)
                : element;

        if (!this.originalElement) {
            throw new Error('SearchableDropdown: element not found');
        }

        this.isSelect = this.originalElement.tagName === 'SELECT';
        this.rememberSelection = rememberSelection && !!this.originalElement.id;

        this.items = this.isSelect
            ? this._extractItemsFromSelect(this.originalElement)
            : items;

        this.placeholder = placeholder;
        this.searchable = searchable;
        this.isOpen = false;
        this.activeIndex = -1;

        this._createContainer();
        this._render();
        this._bindEvents();
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
                label: option.text
            }));
    }

    _createContainer() {
        this.container = document.createElement('div');
        this.container.className = 'searchable-dropdown';

        this.originalElement.parentNode.insertBefore(
            this.container,
            this.originalElement.nextSibling
        );

        if (this.isSelect) {
            this.originalElement.style.display = 'none';
        }
    }

    _render() {
        this.input = document.createElement('input');
        this.input.type = 'text';
        this.input.placeholder = this.placeholder;

        if (!this.searchable) {
            this.input.readOnly = true;
            this.input.classList.add('non-searchable');
        }

        this.optionsEl = document.createElement('div');
        this.optionsEl.className = 'options';

        this.container.appendChild(this.input);
        this.container.appendChild(this.optionsEl);

        if (this.isSelect && this.originalElement.value) {
            const selected = this.items.find(
                item => item.value === this.originalElement.value
            );
            if (selected) {
                this.input.value = selected.label;
            }
        }
    }

    _bindEvents() {
        if (this.searchable) {
            // Add filtering logic, but only for dropdowns which have this logic enabled
            this.input.addEventListener('input', () => {
                const query = this.input.value.toLowerCase();
                const filtered = this.items.filter(item =>
                    item.label.toLowerCase().includes(query)
                );
                this._renderOptions(filtered);
                this._open();
            });
        }

        // Sequential clicks on input field should trigger open/close logic
        this.input.addEventListener('mousedown', e => {
            e.preventDefault(); // critical: prevents focus-triggered reopen

            if (this.isOpen) {
                this._close();
            } else {
                this._open();
                this.input.focus();
            }
        });

        // We should handle blur events to validate user input. If entered value doesn't correspond any available option,
        // dropdown selection will be reset
        this.input.addEventListener('blur', () => {
            // Should be timed out, otherwise disturbs item selection via mouse
            setTimeout(() => {
                if (this.searchable) {
                    const text = this.input.value.trim();
                    if (!text) {
                        this.selectItem(null);
                        return;
                    }
                    const match = this.items.find(
                        item => item.label === text
                    );
                    this.selectItem(match);
                }
                this._close();
            }, 100);
        });

        // Better UX - add possibility to navigate the list and select items via keyboard
        this.input.addEventListener('keydown', e => {
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                this._handleArrowDown();
            }

            if (e.key === 'ArrowUp') {
                e.preventDefault();
                this._handleArrowUp();
            }

            if (e.key === 'Enter') {
                e.preventDefault();
                this._handleEnter();
            }

            if (e.key === 'Escape') {
                this._close();
            }
        });

        // Focusing input element should automatically open dropdown's list
        this.input.addEventListener('focus', () => {
            this._open();
        });

        // Clicking outside of component should automatically close dropdown's list
        document.addEventListener('mousedown', e => {
            if (!this.container.contains(e.target)) {
                this._close();
            }
        });
    }

    _renderOptions(list) {
        this.optionsEl.innerHTML = '';
        this._visibleItems = list;

        list.forEach((item, index) => {
            const option = document.createElement('div');
            option.className = 'option';
            option.textContent = item.label;

            if (item.value === this.value) {
                option.classList.add('selected');
            }

            if (index === this.activeIndex) {
                option.classList.add('active');
            }

            option.addEventListener('click', () => {
                this.selectItem(item);
            });

            this.optionsEl.appendChild(option);
        });
    }

    _handleArrowDown() {
        if (!this.isOpen) {
            this._open();
            this.activeIndex = 0;
        } else {
            this.activeIndex = (this.activeIndex + 1) % this._visibleItems.length;
        }

        this._refreshActive();
    }

    _handleArrowUp() {
        if (!this.isOpen) {
            this._open();
            this.activeIndex = this._visibleItems.length - 1;
        } else {
            const visibleLength = this._visibleItems.length;
            if (this.activeIndex === -1) {
                this.activeIndex = visibleLength - 1;
            } else {
                this.activeIndex = (this.activeIndex - 1 + visibleLength) % visibleLength;
            }
        }

        this._refreshActive();
    }

    _handleEnter() {
        if (!this.isOpen) {
            return;
        }

        const item = this._visibleItems[this.activeIndex];
        if (item) {
            this.selectItem(item);
        }
    }

    _refreshActive() {
        this._renderOptions(this._visibleItems);
        this._scrollActiveIntoView();
    }

    _scrollActiveIntoView() {
        const active = this.optionsEl.querySelector('.option.active');
        if (active) {
            active.scrollIntoView({ block: 'nearest' });
        }
    }

    _open() {
        if (this.isOpen) {
            return;
        }

        this.isOpen = true;

        this._renderOptions(this.items);
        this.activeIndex = this._visibleItems.findIndex(item => item.value === this.value);
        this._show();
        this._scrollActiveIntoView();
    }

    _close() {
        if (!this.isOpen) {
            return;
        }

        this.isOpen = false;
        this._hide();
        this.activeIndex = -1;
    }

    _show() {
        this.optionsEl.style.display = 'block';

        let bounding = this.optionsEl.getBoundingClientRect();
        if (bounding.bottom > (window.innerHeight || document.documentElement.clientHeight)) {
            this.optionsEl.classList.add('dropup');
        }
    }

    _hide() {
        this.optionsEl.style.display = 'none';
        this.optionsEl.classList.remove('dropup');
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
        this.items = this.isSelect
            ? this._extractItemsFromSelect(this.originalElement)
            : this.items;
        this.restoreSelection();
    }

    selectItem(item, preventClosing = false) {
        this.input.value = item ? item.label : "";
        if (!preventClosing) {
            this._close();
        }

        if (this.isSelect) {
            this.originalElement.value = item ? item.value : null;
            this.originalElement.dispatchEvent(
                new Event('change', { bubbles: true })
            );
        }

        this._saveSelection(item ? item.value : null);
    }

    get id() {
        return this.originalElement.id;
    }

    get value() {
        return this.isSelect
            ? this.originalElement.value
            : this.input.value;
    }

    set value(val) {
        if (this.isSelect) {
            const item = this.items.find(i => i.value === val);
            if (item) {
                this.originalElement.value = val;
                this.input.value = item.label;
            }
        } else {
            this.input.value = val;
        }
    }
}
