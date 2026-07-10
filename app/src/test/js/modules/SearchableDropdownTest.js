import SearchableDropdown from '../../../main/resources/js/modules/SearchableDropdown.js';
import { expect } from 'chai';
import { JSDOM } from 'jsdom';
import sinon from 'sinon';

// MutationObserver callbacks fire as microtasks — let them run.
const flush = () => new Promise(resolve => setTimeout(resolve, 0));

describe('SearchableDropdown', function () {
    let dom, window, document;

    beforeEach(function () {
        dom = new JSDOM(`<!DOCTYPE html>
        <html lang="en">
        <body>
            <div id="build-container"></div>
            <select id="single">
                <option value="a">A</option>
                <option value="b">B</option>
                <option value="c" disabled>C</option>
            </select>
            <select id="multi" multiple>
                <option value="a">A</option>
                <option value="b">B</option>
                <option value="c">C</option>
            </select>
        </body>
        </html>`, { url: 'http://localhost/' });

        window = dom.window;
        document = window.document;

        global.window = window;
        global.document = document;
        global.Event = window.Event;
        global.MouseEvent = window.MouseEvent;
        global.KeyboardEvent = window.KeyboardEvent;
        global.MutationObserver = window.MutationObserver;

        // jsdom has no layout: scrollIntoView is unimplemented. Stub it so keyboard-nav tests
        // (which call _scrollActiveIntoView) don't throw. The window is fresh per test.
        window.Element.prototype.scrollIntoView = sinon.stub();
    });

    afterEach(function () {
        delete global.window;
        delete global.document;
        delete global.Event;
        delete global.MouseEvent;
        delete global.KeyboardEvent;
        delete global.MutationObserver;
    });

    // Convenience: fire a bubbling mousedown at a node.
    function mousedown(node) {
        node.dispatchEvent(new window.MouseEvent('mousedown', { bubbles: true, cancelable: true }));
    }

    // Convenience: fire a bubbling keydown with the given key at a node.
    function keydown(node, key) {
        node.dispatchEvent(new window.KeyboardEvent('keydown', { key, bubbles: true, cancelable: true }));
    }

    describe('clearable (× reset button)', function () {
        let select, dropdown;

        beforeEach(function () {
            // Clearable pairs with allowEmpty; start on a real value (option B is pre-selected).
            select = document.createElement('select');
            select.id = 'clearable-single';
            select.innerHTML = '<option value="a">A</option><option value="b" selected>B</option>';
            document.body.appendChild(select);
            dropdown = new SearchableDropdown({ element: select, clearable: true, allowEmpty: true });
        });

        it('shows the clear button on initial render when a value is pre-selected', function () {
            // Regression: _updateClearButton() must run in _render(), so the × is visible from the
            // start (via the container's .has-value class), not only after the user interacts.
            expect(dropdown.container.classList.contains('clearable')).to.be.true;
            expect(dropdown.container.querySelector('.sd-clear')).to.exist;
            expect(dropdown.container.classList.contains('has-value')).to.be.true;
        });

        it('clearing resets the selection to the placeholder and hides the clear button', function () {
            dropdown.selectItem(null);
            expect(select.selectedIndex).to.equal(-1);
            expect(dropdown.container.classList.contains('has-value')).to.be.false;
        });
    });

    describe('build mode (CustomSelect-compatible API)', function () {
        let dropdown;

        beforeEach(function () {
            dropdown = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                rememberSelection: false
            });
        });

        afterEach(function () {
            dropdown.destroy();
        });

        it('defaults the selection to the first added option', function () {
            dropdown.addOption('a', 'Apple');
            dropdown.addOption('b', 'Banana');
            expect(dropdown.getSelectedValue()).to.equal('a');
            expect(dropdown.getSelectedText()).to.equal('Apple');
        });

        it('selectValue selects a matching option', function () {
            dropdown.addOption('a', 'Apple');
            dropdown.addOption('b', 'Banana');
            dropdown.selectValue('b');
            expect(dropdown.getSelectedValue()).to.equal('b');
            expect(dropdown.getSelectedText()).to.equal('Banana');
        });

        it('falls back to the first option when the value matches nothing', function () {
            dropdown.addOption('a', 'Apple');
            dropdown.addOption('b', 'Banana');
            dropdown.selectValue('does-not-exist');
            expect(dropdown.getSelectedValue()).to.equal('a');
        });

        it('containsOption and empty() behave as expected', function () {
            dropdown.addOption('a', 'Apple');
            expect(dropdown.containsOption('a')).to.be.true;
            expect(dropdown.containsOption('x')).to.be.false;
            dropdown.empty();
            expect(dropdown.containsOption('a')).to.be.false;
            expect(dropdown.getSelectedValue()).to.equal('');
        });
    });

    describe('multi-select (native <select multiple>)', function () {
        let dropdown, select;

        beforeEach(function () {
            select = document.getElementById('multi');
            dropdown = new SearchableDropdown({
                element: select,
                multiselect: true,
                rememberSelection: false
            });
        });

        afterEach(function () {
            dropdown.destroy();
        });

        it('starts with nothing selected', function () {
            expect(dropdown.getSelectedValue()).to.deep.equal([]);
        });

        it('toggling an option mirrors onto the native <select multiple>', function () {
            dropdown.selectItem(dropdown.items.find(i => i.value === 'b'));
            expect(dropdown.getSelectedValue()).to.deep.equal(['b']);
            expect(select.querySelector("option[value='b']").selected).to.be.true;
        });

        it('keeps multiple selected values', function () {
            dropdown.selectItem(dropdown.items.find(i => i.value === 'a'));
            dropdown.selectItem(dropdown.items.find(i => i.value === 'c'));
            expect(dropdown.getSelectedValue()).to.deep.equal(['a', 'c']);
        });

        it('toggling the same option twice deselects it', function () {
            const item = dropdown.items.find(i => i.value === 'a');
            dropdown.selectItem(item);
            dropdown.selectItem(item);
            expect(dropdown.getSelectedValue()).to.deep.equal([]);
            expect(select.querySelector("option[value='a']").selected).to.be.false;
        });
    });

    it('empty() clears rendered chips in multi-select build mode', function () {
        const dropdown = new SearchableDropdown({
            selectContainer: document.getElementById('build-container'),
            multiselect: true,
            rememberSelection: false
        });
        dropdown.addOption('a', 'A');
        dropdown.addOption('b', 'B');
        dropdown.selectMultipleValues(['a', 'b']);
        expect(dropdown.trigger.querySelectorAll('.sd-chip').length).to.equal(2);

        dropdown.empty();
        expect(dropdown.trigger.querySelectorAll('.sd-chip').length).to.equal(0);
        expect(dropdown.getSelectedValue()).to.deep.equal([]);
        dropdown.destroy();
    });

    it('single-select keeps the first option by default', function () {
        const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
        expect(dropdown.getSelectedValue()).to.equal('a');
        dropdown.destroy();
    });

    it('allowEmpty leaves a native single-select unselected (shows placeholder)', function () {
        const select = document.getElementById('single');
        const dropdown = new SearchableDropdown({ element: select, allowEmpty: true, placeholder: 'Select...', rememberSelection: false });
        expect(select.selectedIndex).to.equal(-1);
        expect(dropdown.getSelectedValue()).to.equal('');
        dropdown.destroy();
    });

    it('destroy() restores selectedIndex so allowEmpty does not leak to a re-wrap', function () {
        const select = document.getElementById('single');
        // First instance clears the selection (allowEmpty); re-wrapping without allowEmpty must see
        // the first option again, not the -1 left behind.
        new SearchableDropdown({ element: select, allowEmpty: true, placeholder: 'Select...', rememberSelection: false });
        expect(select.selectedIndex).to.equal(-1);
        const second = new SearchableDropdown({ element: select, rememberSelection: false });
        expect(second.getSelectedValue()).to.equal('a');
        second.destroy();
    });

    it('mirrors the <select> title tooltip onto the trigger', function () {
        const select = document.getElementById('single');
        select.setAttribute('title', 'Choose a size');
        const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
        expect(dropdown.trigger.getAttribute('title')).to.equal('Choose a size');
        dropdown.destroy();
        select.removeAttribute('title');
    });

    it('exposes ARIA combobox/listbox semantics', function () {
        const label = document.createElement('label');
        label.setAttribute('for', 'single');
        label.textContent = 'Paper size';
        document.body.appendChild(label);

        const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
        expect(dropdown.trigger.getAttribute('role')).to.equal('combobox');
        expect(dropdown.trigger.getAttribute('aria-haspopup')).to.equal('listbox');
        expect(dropdown.trigger.getAttribute('aria-expanded')).to.equal('false');
        expect(dropdown.trigger.getAttribute('aria-label')).to.equal('Paper size');
        expect(dropdown.trigger.getAttribute('aria-controls')).to.equal(dropdown.itemsEl.id);
        expect(dropdown.itemsEl.getAttribute('role')).to.equal('listbox');

        dropdown._renderOptions(dropdown.items);
        const first = dropdown.itemsEl.children[0];
        expect(first.getAttribute('role')).to.equal('option');
        expect(first.getAttribute('aria-selected')).to.equal('true'); // 'a' is the selected first option
        const disabledOpt = dropdown.itemsEl.children[2];
        expect(disabledOpt.getAttribute('aria-disabled')).to.equal('true');

        dropdown._open();
        expect(dropdown.trigger.getAttribute('aria-expanded')).to.equal('true');
        dropdown._close();
        expect(dropdown.trigger.getAttribute('aria-expanded')).to.equal('false');

        dropdown.destroy();
        label.remove();
    });

    it('ignores a disabled option and skips it in keyboard navigation', function () {
        const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
        const disabledItem = dropdown.items.find(i => i.value === 'c');
        expect(disabledItem.disabled).to.be.true;

        // Selecting a disabled option is a no-op (stays on the first option).
        dropdown.selectItem(disabledItem);
        expect(dropdown.getSelectedValue()).to.equal('a');

        // Keyboard navigation skips the disabled option (a=0, b=1, c=2 disabled → wraps to a).
        dropdown._visibleItems = dropdown.items;
        expect(dropdown._nextEnabledIndex(1, 1)).to.equal(0);
        // From no selection (index -1): Down → first (a=0), Up → last enabled (c=2 is disabled → b=1).
        expect(dropdown._nextEnabledIndex(-1, 1)).to.equal(0);
        expect(dropdown._nextEnabledIndex(-1, -1)).to.equal(1);
        dropdown.destroy();
    });

    it('reflects the wrapped <select> disabled state onto the container', function () {
        const select = document.getElementById('single');
        select.disabled = true;
        const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
        expect(dropdown.container.classList.contains('disabled')).to.be.true;

        select.disabled = false;
        dropdown._syncDisabled();
        expect(dropdown.container.classList.contains('disabled')).to.be.false;
        dropdown.destroy();
    });

    it('re-wrapping the same <select> does not stack duplicate containers/portals', function () {
        const select = document.getElementById('multi');
        new SearchableDropdown({ element: select, rememberSelection: false });
        new SearchableDropdown({ element: select, rememberSelection: false });
        new SearchableDropdown({ element: select, rememberSelection: false });
        expect(document.querySelectorAll('.searchable-dropdown').length).to.equal(1);
        expect(document.querySelectorAll('.sd-portal').length).to.equal(1);
        select._searchableDropdown.destroy();
    });

    it('destroy() removes the body-level portal', function () {
        const dropdown = new SearchableDropdown({
            selectContainer: document.getElementById('build-container'),
            rememberSelection: false
        });
        expect(document.querySelectorAll('.sd-portal').length).to.equal(1);
        dropdown.destroy();
        expect(document.querySelectorAll('.sd-portal').length).to.equal(0);
    });

    it('scopes the body-level portal with .sbb-ui so its tokens match the trigger', function () {
        // The popup is appended to <body>, outside the trigger's scoped wrapper; without .sbb-ui it
        // would inherit --sbb-* from :root and could render with a foreign extension's tokens on a
        // shared multi-version page. .sbb-ui keeps it on the same design tokens as the control.
        const dropdown = new SearchableDropdown({
            selectContainer: document.getElementById('build-container'),
            rememberSelection: false
        });
        const portal = document.querySelector('.sd-portal');
        expect(portal.classList.contains('sbb-ui'), '.sd-portal must carry .sbb-ui').to.be.true;
        dropdown.destroy();
    });

    describe('constructor validation', function () {
        it('throws when neither element nor selectContainer is given', function () {
            expect(() => new SearchableDropdown({})).to.throw('element or selectContainer is required');
        });

        it('throws when the selectContainer selector matches nothing', function () {
            expect(() => new SearchableDropdown({ selectContainer: '#no-such-container' }))
                .to.throw('selectContainer not found');
        });

        it('throws when the element selector matches nothing', function () {
            expect(() => new SearchableDropdown({ element: '#no-such-element' }))
                .to.throw('element not found');
        });

        it('resolves element and selectContainer passed as string selectors', function () {
            const byString = new SearchableDropdown({ element: '#single', rememberSelection: false });
            expect(byString.originalElement).to.equal(document.getElementById('single'));
            byString.destroy();

            const buildByString = new SearchableDropdown({ selectContainer: '#build-container', rememberSelection: false });
            expect(buildByString.container).to.equal(document.getElementById('build-container'));
            buildByString.destroy();
        });
    });

    describe('width inheritance', function () {
        it('mirrors an explicit <select> width onto the container', function () {
            const select = document.getElementById('single');
            select.style.width = '142px';
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            expect(dropdown.container.style.width).to.equal('142px');
            dropdown.destroy();
        });
    });

    describe('rememberSelection cookie save + load', function () {
        it('saves the chosen value to a cookie and reloads it', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select }); // rememberSelection default true, has id
            expect(dropdown.rememberSelection).to.be.true;

            dropdown.selectItem(dropdown.items.find(i => i.value === 'b'));
            const key = SearchableDropdown.COOKIE_PREFIX + 'single';
            expect(document.cookie).to.contain(key + '=b');
            expect(dropdown._loadSelection()).to.equal('b');
            dropdown.destroy();
        });

        it('_loadSelection returns null when rememberSelection is off', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            expect(dropdown._loadSelection()).to.equal(null);
            dropdown.destroy();
        });

        it('restoreSelection applies a previously saved cookie value', function () {
            const key = SearchableDropdown.COOKIE_PREFIX + 'single';
            document.cookie = `${key}=b; path=/`;
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select });
            dropdown.restoreSelection();
            expect(dropdown.getSelectedValue()).to.equal('b');
            dropdown.destroy();
        });

        it('restoreSelection clears the selection when no cookie is saved', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, allowEmpty: true });
            dropdown.restoreSelection();
            expect(select.selectedIndex).to.equal(-1);
            dropdown.destroy();
        });
    });

    describe('MutationObserver-driven sync', function () {
        it('mirrors the <select> display/visibility onto the container when style changes', async function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            select.style.display = 'none';
            await flush();
            expect(dropdown.container.style.display).to.equal('none');
            dropdown.destroy();
        });

        it('mirrors the disabled attribute onto the container via the observer', async function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            select.disabled = true;
            await flush();
            expect(dropdown.container.classList.contains('disabled')).to.be.true;
            dropdown.destroy();
        });

        it('re-extracts items when the <select> options change', async function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            const opt = document.createElement('option');
            opt.value = 'd';
            opt.text = 'D';
            select.appendChild(opt);
            await flush();
            expect(dropdown.items.some(i => i.value === 'd')).to.be.true;
            dropdown.destroy();
        });

        it('re-renders open options when the <select> options change while open', async function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            dropdown._open();
            const opt = document.createElement('option');
            opt.value = 'd';
            opt.text = 'D';
            select.appendChild(opt);
            await flush();
            const labels = Array.from(dropdown.itemsEl.children).map(o => o.textContent);
            expect(labels).to.include('D');
            dropdown.destroy();
        });
    });

    describe('_syncDisabled closes an open popup', function () {
        it('closes the popup when the control becomes disabled', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            dropdown._open();
            expect(dropdown.isOpen).to.be.true;
            select.disabled = true;
            dropdown._syncDisabled();
            expect(dropdown.isOpen).to.be.false;
            dropdown.destroy();
        });
    });

    describe('build mode label + trigger id wiring', function () {
        it('assigns a trigger id derived from the container id and wires <label for>', function () {
            const container = document.createElement('div');
            container.id = 'my-build';
            document.body.appendChild(container);
            const label = document.createElement('label');
            const dropdown = new SearchableDropdown({ selectContainer: container, label, rememberSelection: false });
            expect(dropdown.trigger.id).to.equal('my-build_sd-trigger');
            expect(label.htmlFor).to.equal('my-build_sd-trigger');
            dropdown.destroy();
        });
    });

    describe('open/close via pointer + outside click', function () {
        it('toggles open and closed on trigger mousedown', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            mousedown(dropdown.trigger);
            expect(dropdown.isOpen).to.be.true;
            expect(dropdown.container.classList.contains('open')).to.be.true;
            mousedown(dropdown.trigger);
            expect(dropdown.isOpen).to.be.false;
            dropdown.destroy();
        });

        it('closes on a mousedown outside the container and portal', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown._open();
            const outside = document.createElement('div');
            document.body.appendChild(outside);
            mousedown(outside);
            expect(dropdown.isOpen).to.be.false;
            dropdown.destroy();
        });

        it('stays open on a mousedown inside the portal', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown._open();
            mousedown(dropdown.portal);
            expect(dropdown.isOpen).to.be.true;
            dropdown.destroy();
        });

        it('_open is a no-op when already open, and does not open a disabled control', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            dropdown._open();
            const listRef = dropdown.itemsEl.innerHTML;
            dropdown._open(); // early return
            expect(dropdown.itemsEl.innerHTML).to.equal(listRef);
            dropdown._close();

            select.disabled = true;
            dropdown._open();
            expect(dropdown.isOpen).to.be.false;
            dropdown.destroy();
        });
    });

    describe('focus handling on selection (no lingering focus ring)', function () {
        it('blurs the trigger after a mouse pick that closes the popup (single-select)', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), searchable: false, rememberSelection: false });
            dropdown._open();
            expect(document.activeElement, 'trigger focused on open (no search box)').to.equal(dropdown.trigger);
            mousedown(dropdown.itemsEl.children[0]); // pick option A with the mouse
            expect(dropdown.isOpen).to.be.false;
            expect(document.activeElement, 'trigger blurred → no lingering focus ring').to.not.equal(dropdown.trigger);
            dropdown.destroy();
        });

        it('keeps focus on the trigger after a keyboard pick (Enter)', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), searchable: false, rememberSelection: false });
            dropdown._open();
            keydown(dropdown.trigger, 'ArrowDown'); // highlight option B
            keydown(dropdown.trigger, 'Enter');     // select via keyboard
            expect(dropdown.isOpen).to.be.false;
            expect(document.activeElement, 'focus kept for continued keyboard nav').to.equal(dropdown.trigger);
            dropdown.destroy();
        });
    });

    describe('keyboard navigation', function () {
        it('opens a closed popup on ArrowDown/Enter/Space from the trigger', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), searchable: false, rememberSelection: false });
            keydown(dropdown.trigger, 'ArrowDown');
            expect(dropdown.isOpen).to.be.true;
            dropdown._close();
            keydown(dropdown.trigger, 'Enter');
            expect(dropdown.isOpen).to.be.true;
            dropdown._close();
            keydown(dropdown.trigger, ' ');
            expect(dropdown.isOpen).to.be.true;
            dropdown.destroy();
        });

        it('ArrowDown/ArrowUp move the highlight, Enter selects, Escape closes', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), searchable: false, rememberSelection: false });
            dropdown._open();
            // starts highlighting the selected first option (index 0)
            keydown(dropdown.trigger, 'ArrowDown'); // 0 -> 1 (b)
            expect(dropdown.activeIndex).to.equal(1);
            expect(dropdown.itemsEl.children[1].classList.contains('active')).to.be.true;
            keydown(dropdown.trigger, 'ArrowUp'); // 1 -> 0 (a)
            expect(dropdown.activeIndex).to.equal(0);
            keydown(dropdown.trigger, 'ArrowDown'); // to b
            keydown(dropdown.trigger, 'Enter');
            expect(dropdown.getSelectedValue()).to.equal('b');
            expect(dropdown.isOpen).to.be.false;

            dropdown._open();
            keydown(dropdown.trigger, 'Escape');
            expect(dropdown.isOpen).to.be.false;
            dropdown.destroy();
        });

        it('routes keyboard through the search box when searchable', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown._open();
            keydown(dropdown.searchInput, 'ArrowDown');
            expect(dropdown.activeIndex).to.be.greaterThan(-1);
            const before = dropdown.activeIndex;
            keydown(dropdown.searchInput, 'ArrowUp');
            expect(dropdown.activeIndex).to.not.equal(before === 0 ? -99 : before); // moved
            dropdown.destroy();
        });

        it('arrow handlers no-op when there are no visible items', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown._visibleItems = [];
            dropdown.activeIndex = -1;
            dropdown._handleArrowDown();
            dropdown._handleArrowUp();
            expect(dropdown.activeIndex).to.equal(-1);
            dropdown.destroy();
        });

        it('_nextEnabledIndex returns the current index when every option is disabled', function () {
            const select = document.createElement('select');
            select.innerHTML = '<option value="a" disabled>A</option><option value="b" disabled>B</option>';
            document.body.appendChild(select);
            const dropdown = new SearchableDropdown({ element: select, allowEmpty: true, rememberSelection: false });
            dropdown._visibleItems = dropdown.items;
            dropdown.activeIndex = 0;
            expect(dropdown._nextEnabledIndex(0, 1)).to.equal(0);
            dropdown.destroy();
        });

        it('_handleEnter is a no-op when no option is highlighted', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), searchable: false, rememberSelection: false });
            dropdown._open();
            dropdown.activeIndex = -1;
            dropdown._handleEnter();
            expect(dropdown.isOpen).to.be.true; // nothing selected, stays open
            dropdown.destroy();
        });
    });

    describe('search filtering + erase', function () {
        it('filters the option list as the user types', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown._open();
            dropdown.searchInput.value = 'b';
            dropdown.searchInput.dispatchEvent(new window.Event('input'));
            const labels = Array.from(dropdown.itemsEl.children).map(o => o.textContent.replace('×', '').trim());
            expect(labels).to.deep.equal(['B']);
            expect(dropdown.activeIndex).to.equal(-1);
            dropdown.destroy();
        });

        it('the erase icon clears the query and restores the full list', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown._open();
            dropdown.searchInput.value = 'b';
            dropdown.searchInput.dispatchEvent(new window.Event('input'));
            expect(dropdown.itemsEl.children.length).to.equal(1);
            mousedown(dropdown.eraseIcon);
            expect(dropdown.searchInput.value).to.equal('');
            expect(dropdown.itemsEl.children.length).to.equal(3);
            dropdown.destroy();
        });
    });

    describe('option rendering: icons, classes, mouse', function () {
        function iconSelect() {
            const select = document.createElement('select');
            select.innerHTML =
                '<option value="a" data-icon="/i/a.svg">A</option>' +
                '<option value="b">B</option>';
            document.body.appendChild(select);
            return select;
        }

        it('overlays the selected option icon on a single-select trigger', function () {
            const dropdown = new SearchableDropdown({ element: iconSelect(), rememberSelection: false });
            expect(dropdown.triggerIcon.getAttribute('src')).to.equal('/i/a.svg');
            expect(dropdown.triggerIcon.style.display).to.equal('');
            expect(dropdown.trigger.classList.contains('has-icon')).to.be.true;
            dropdown.destroy();
        });

        it('renders an option-icon and has-icon class for single-select options', function () {
            const dropdown = new SearchableDropdown({ element: iconSelect(), rememberSelection: false });
            dropdown._renderOptions(dropdown.items);
            const first = dropdown.itemsEl.children[0];
            expect(first.classList.contains('has-icon')).to.be.true;
            const img = first.querySelector('img.option-icon');
            expect(img).to.exist;
            expect(img.getAttribute('src')).to.equal('/i/a.svg');
            dropdown.destroy();
        });

        it('renders icons inside multi-select options', function () {
            const dropdown = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                multiselect: true,
                rememberSelection: false
            });
            dropdown.addOption('a', 'A', '/i/a.svg');
            dropdown._renderOptions(dropdown.items);
            const opt = dropdown.itemsEl.children[0];
            expect(opt.classList.contains('multiselect-option')).to.be.true;
            expect(opt.querySelector('input[type="checkbox"]')).to.exist;
            expect(opt.querySelector('img.option-icon').getAttribute('src')).to.equal('/i/a.svg');
            dropdown.destroy();
        });

        it('applies an icon background tile from data-icon-bg (element mode)', function () {
            const select = document.createElement('select');
            select.innerHTML =
                '<option value="a" data-icon="/i/a.svg" data-icon-bg="#1a3a5c">A</option>' +
                '<option value="b" data-icon="/i/b.svg">B</option>';
            document.body.appendChild(select);
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            // Option "a" is selected first, so its tile shows on the trigger icon.
            expect(dropdown.triggerIcon.classList.contains('has-icon-bg')).to.be.true;
            expect(dropdown.triggerIcon.style.backgroundColor).to.not.equal('');
            // In the list: "a" gets a tile, "b" (no data-icon-bg) does not.
            dropdown._renderOptions(dropdown.items);
            const iconA = dropdown.itemsEl.children[0].querySelector('img.option-icon');
            const iconB = dropdown.itemsEl.children[1].querySelector('img.option-icon');
            expect(iconA.classList.contains('has-icon-bg')).to.be.true;
            expect(iconA.style.backgroundColor).to.not.equal('');
            expect(iconB.classList.contains('has-icon-bg')).to.be.false;
            dropdown.destroy();
        });

        it('accepts an icon background as the 4th addOption argument (build mode)', function () {
            const dropdown = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                rememberSelection: false
            });
            dropdown.addOption('a', 'A', '/i/a.svg', '#1a3a5c');
            dropdown.addOption('b', 'B', '/i/b.svg');
            dropdown._renderOptions(dropdown.items);
            const iconA = dropdown.itemsEl.children[0].querySelector('img.option-icon');
            const iconB = dropdown.itemsEl.children[1].querySelector('img.option-icon');
            expect(iconA.classList.contains('has-icon-bg')).to.be.true;
            expect(iconA.style.backgroundColor).to.not.equal('');
            expect(iconB.classList.contains('has-icon-bg')).to.be.false;
            dropdown.destroy();
        });

        it('mirrors an option CSS class onto the rendered option when preserveOptionClasses', function () {
            const select = document.createElement('select');
            select.innerHTML = '<option value="a" class="parent">A</option>';
            document.body.appendChild(select);
            const dropdown = new SearchableDropdown({ element: select, preserveOptionClasses: true, rememberSelection: false });
            dropdown._renderOptions(dropdown.items);
            expect(dropdown.itemsEl.children[0].classList.contains('parent')).to.be.true;
            dropdown.destroy();
        });

        it('highlights on mouseover and selects on option mousedown', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown._open();
            const bOption = dropdown.itemsEl.children[1];
            bOption.dispatchEvent(new window.MouseEvent('mouseover', { bubbles: true }));
            expect(dropdown.activeIndex).to.equal(1);
            expect(bOption.classList.contains('active')).to.be.true;
            mousedown(bOption);
            expect(dropdown.getSelectedValue()).to.equal('b');
            dropdown.destroy();
        });
    });

    describe('label text resolution', function () {
        it('prefers the passed label element text for aria-label', function () {
            const container = document.createElement('div');
            document.body.appendChild(container);
            const label = document.createElement('label');
            label.textContent = '  Colour  ';
            const dropdown = new SearchableDropdown({ selectContainer: container, label, rememberSelection: false });
            expect(dropdown.trigger.getAttribute('aria-label')).to.equal('Colour');
            dropdown.destroy();
        });

        it('falls back to the <select> aria-label attribute', function () {
            const select = document.getElementById('single');
            select.setAttribute('aria-label', 'Sizes');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            expect(dropdown.trigger.getAttribute('aria-label')).to.equal('Sizes');
            dropdown.destroy();
            select.removeAttribute('aria-label');
        });

        it('ignores an id that is not usable as a CSS selector', function () {
            const select = document.createElement('select');
            select.id = 'bad"id';
            select.innerHTML = '<option value="a">A</option>';
            document.body.appendChild(select);
            // No label / aria-label, so it tries the label[for=...] lookup and swallows the SyntaxError.
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            expect(dropdown.trigger.hasAttribute('aria-label')).to.be.false;
            dropdown.destroy();
        });
    });

    describe('portal positioning', function () {
        it('shifts the popup left when it would overflow the right viewport edge', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown.trigger.getBoundingClientRect = () => ({ left: 1000, right: 1050, width: 50, top: 10, bottom: 30, height: 20 });
            Object.defineProperty(dropdown.optionsEl, 'offsetWidth', { value: 200, configurable: true });
            Object.defineProperty(dropdown.optionsEl, 'offsetHeight', { value: 100, configurable: true });
            dropdown._position();
            // viewportWidth 1024, gap 4 => left = max(4, 1024-200-4) = 820
            expect(dropdown.portal.style.left).to.equal('820px');
            dropdown.destroy();
        });

        it('flips the popup above the trigger (dropup) when it would not fit below', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown.trigger.getBoundingClientRect = () => ({ left: 10, right: 110, width: 100, top: 700, bottom: 720, height: 20 });
            Object.defineProperty(dropdown.optionsEl, 'offsetWidth', { value: 100, configurable: true });
            Object.defineProperty(dropdown.optionsEl, 'offsetHeight', { value: 300, configurable: true });
            dropdown._position();
            expect(dropdown.optionsEl.classList.contains('dropup')).to.be.true;
            expect(dropdown.portal.style.top).to.equal('700px');
            dropdown.destroy();
        });

        it('repositions on window scroll/resize while open', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            dropdown._open();
            const spy = sinon.spy(dropdown, '_position');
            window.dispatchEvent(new window.Event('scroll'));
            window.dispatchEvent(new window.Event('resize'));
            expect(spy.callCount).to.be.at.least(2);
            spy.restore();
            dropdown.destroy();
        });

        it('focuses the trigger (not a search box) when opened without search', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), searchable: false, rememberSelection: false });
            dropdown._open();
            expect(document.activeElement).to.equal(dropdown.trigger);
            dropdown.destroy();
        });
    });

    describe('changeListener', function () {
        it('fires the change listener with the instance on selection', function () {
            const listener = sinon.spy();
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), changeListener: listener, rememberSelection: false });
            dropdown.selectItem(dropdown.items.find(i => i.value === 'b'));
            expect(listener.calledOnce).to.be.true;
            expect(listener.firstCall.args[0]).to.equal(dropdown);
            dropdown.destroy();
        });
    });

    describe('multi-select chips', function () {
        it('renders one chip per selected value and removes on chip × mousedown', function () {
            const select = document.getElementById('multi');
            const dropdown = new SearchableDropdown({ element: select, multiselect: true, rememberSelection: false });
            dropdown.selectItem(dropdown.items.find(i => i.value === 'a'));
            dropdown.selectItem(dropdown.items.find(i => i.value === 'b'));
            expect(dropdown.trigger.querySelectorAll('.sd-chip').length).to.equal(2);

            const firstRemove = dropdown.trigger.querySelector('.sd-chip .sd-chip-remove');
            mousedown(firstRemove);
            expect(dropdown.getSelectedValue()).to.deep.equal(['b']);
            expect(select.querySelector("option[value='a']").selected).to.be.false;
            dropdown.destroy();
        });

        it('shows the placeholder when nothing is selected', function () {
            const dropdown = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                multiselect: true,
                placeholder: 'Pick some',
                rememberSelection: false
            });
            dropdown.addOption('a', 'A');
            const ph = dropdown.trigger.querySelector('.sd-placeholder');
            expect(ph).to.exist;
            expect(ph.textContent).to.equal('Pick some');
            dropdown.destroy();
        });

        it('getSelectedText returns an array of labels in multi-select', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('multi'), multiselect: true, rememberSelection: false });
            dropdown.selectItem(dropdown.items.find(i => i.value === 'a'));
            dropdown.selectItem(dropdown.items.find(i => i.value === 'c'));
            expect(dropdown.getSelectedText()).to.deep.equal(['A', 'C']);
            dropdown.destroy();
        });
    });

    describe('clearable button mousedown', function () {
        it('clears the selection when the × button is pressed', function () {
            const select = document.createElement('select');
            select.innerHTML = '<option value="a">A</option><option value="b" selected>B</option>';
            document.body.appendChild(select);
            const dropdown = new SearchableDropdown({ element: select, clearable: true, allowEmpty: true, rememberSelection: false });
            expect(dropdown.container.classList.contains('has-value')).to.be.true;
            mousedown(dropdown.clearButton);
            expect(select.selectedIndex).to.equal(-1);
            expect(dropdown.container.classList.contains('has-value')).to.be.false;
            dropdown.destroy();
        });
    });

    describe('refresh + syncFromElement', function () {
        it('refresh re-extracts items and re-applies the saved selection (element mode)', function () {
            const key = SearchableDropdown.COOKIE_PREFIX + 'single';
            document.cookie = `${key}=b; path=/`;
            const dropdown = new SearchableDropdown({ element: document.getElementById('single') });
            dropdown.refresh();
            expect(dropdown.getSelectedValue()).to.equal('b');
            dropdown.destroy();
        });

        it('refresh keeps build-mode items and restores selection', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            dropdown.addOption('b', 'B');
            dropdown.refresh();
            expect(dropdown.items.length).to.equal(2);
            dropdown.destroy();
        });

        it('syncFromElement is a no-op in build mode', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            expect(() => dropdown.syncFromElement()).to.not.throw();
            expect(dropdown.getSelectedValue()).to.equal('a');
            dropdown.destroy();
        });

        it('syncFromElement keeps the first option when the <select> is left blank (no allowEmpty)', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            select.selectedIndex = -1;
            select.dispatchEvent(new window.Event('change'));
            expect(select.selectedIndex).to.equal(0);
            expect(dropdown.trigger.value).to.equal('A');
            dropdown.destroy();
        });

        it('syncFromElement re-syncs multi-select from the native <select multiple>', function () {
            const select = document.getElementById('multi');
            const dropdown = new SearchableDropdown({ element: select, multiselect: true, rememberSelection: false });
            select.querySelector("option[value='b']").selected = true;
            dropdown.syncFromElement();
            expect(dropdown.getSelectedValue()).to.deep.equal(['b']);
            dropdown.destroy();
        });
    });

    describe('selectItem in element + build mode', function () {
        it('sets the native <select> value on selection (element single)', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            dropdown.selectItem(dropdown.items.find(i => i.value === 'b'));
            expect(select.value).to.equal('b');
            expect(dropdown.trigger.value).to.equal('B');
            dropdown.destroy();
        });

        it('updates item selected flags in build mode', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            dropdown.addOption('b', 'B');
            dropdown.selectItem(dropdown.items.find(i => i.value === 'b'));
            expect(dropdown.getSelectedValue()).to.equal('b');
            expect(dropdown.items.find(i => i.value === 'a').selected).to.be.false;
            dropdown.destroy();
        });
    });

    describe('build-mode API: addOption handle + open re-render', function () {
        it('re-renders the open list when a new option is added', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            dropdown._open();
            dropdown.addOption('b', 'B');
            const labels = Array.from(dropdown.itemsEl.children).map(o => o.textContent);
            expect(labels).to.include('B');
            dropdown.destroy();
        });

        it('option handle classList add/remove/toggle mirrors onto item + rendered option', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            const handle = dropdown.addOption('a', 'A');
            handle.label.classList.add('parent');
            expect(dropdown.items[0].className).to.contain('parent');

            dropdown._open(); // so mutate() hits the isOpen re-render branch
            handle.label.classList.toggle('marker'); // adds
            expect(dropdown.items[0].className).to.contain('marker');
            expect(dropdown.itemsEl.querySelector('.option').classList.contains('marker')).to.be.true;
            handle.label.classList.toggle('marker'); // removes
            expect(dropdown.items[0].className).to.not.contain('marker');
            handle.label.classList.remove('parent');
            expect(dropdown.items[0].className).to.not.contain('parent');
            dropdown.destroy();
        });

        it('empty() re-renders the (now empty) open list', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            dropdown._open();
            dropdown.empty();
            expect(dropdown.itemsEl.querySelectorAll('.option').length).to.equal(0);
            expect(dropdown.itemsEl.querySelector('.sd-empty')).to.exist;
            dropdown.destroy();
        });
    });

    describe('selectValue + selectMultipleValues open re-render', function () {
        it('selectValue re-renders the open list in build mode', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            dropdown.addOption('b', 'B');
            dropdown._open();
            dropdown.selectValue('b');
            const selectedOpt = Array.from(dropdown.itemsEl.children).find(o => o.getAttribute('aria-selected') === 'true');
            expect(selectedOpt.textContent).to.equal('B');
            dropdown.destroy();
        });

        it('selectValue sets a native <select> value (element mode) and falls back on no match', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            dropdown.selectValue('b');
            expect(select.value).to.equal('b');
            dropdown.selectValue('nope'); // no match -> keeps first option
            expect(select.selectedIndex).to.equal(0);
            dropdown.destroy();
        });

        it('selectMultipleValues re-renders the open list', function () {
            const dropdown = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                multiselect: true,
                rememberSelection: false
            });
            dropdown.addOption('a', 'A');
            dropdown.addOption('b', 'B');
            dropdown._open();
            dropdown.selectMultipleValues(['a', 'b']);
            const checked = dropdown.itemsEl.querySelectorAll('input[type="checkbox"]:checked');
            expect(checked.length).to.equal(2);
            dropdown.destroy();
        });
    });

    describe('id getter + value setter/getter', function () {
        it('id returns the container id in build mode and the element id in element mode', function () {
            const container = document.createElement('div');
            container.id = 'cid';
            document.body.appendChild(container);
            const build = new SearchableDropdown({ selectContainer: container, rememberSelection: false });
            expect(build.id).to.equal('cid');
            build.destroy();

            const el = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            expect(el.id).to.equal('single');
            el.destroy();
        });

        it('value setter selects a matching option on a native <select>', function () {
            const select = document.getElementById('single');
            const dropdown = new SearchableDropdown({ element: select, rememberSelection: false });
            dropdown.value = 'b';
            expect(select.value).to.equal('b');
            expect(dropdown.trigger.value).to.equal('B');
            // no-match value is ignored (select unchanged)
            dropdown.value = 'zzz';
            expect(select.value).to.equal('b');
            dropdown.destroy();
        });

        it('value setter delegates to selectValue in build mode', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            dropdown.addOption('b', 'B');
            dropdown.value = 'b';
            expect(dropdown.getSelectedValue()).to.equal('b');
            dropdown.destroy();
        });

        it('value setter writes the trigger directly for a non-<select> element', function () {
            const input = document.createElement('input');
            document.body.appendChild(input);
            const dropdown = new SearchableDropdown({ element: input, items: [{ value: 'x', label: 'X' }], rememberSelection: false });
            expect(dropdown.isSelect).to.be.false;
            dropdown.value = 'hello';
            expect(dropdown.trigger.value).to.equal('hello');
            expect(dropdown.value).to.equal('hello');
            dropdown.destroy();
        });

        it('getSelectedText derives the label from the live value in element single mode', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            expect(dropdown.getSelectedText()).to.equal('A');
            dropdown.destroy();
        });
    });

    describe('element mode wrapping a non-<select> element', function () {
        function nonSelect() {
            const input = document.createElement('input');
            document.body.appendChild(input);
            return new SearchableDropdown({
                element: input,
                items: [{ value: 'x', label: 'X' }, { value: 'y', label: 'Y' }],
                rememberSelection: false
            });
        }

        it('getSelectedValue reads the trigger value (no native <select>)', function () {
            const dropdown = nonSelect();
            dropdown.trigger.value = 'anything';
            expect(dropdown.getSelectedValue()).to.equal('anything');
            dropdown.destroy();
        });

        it('syncFromElement maps the trigger value to the matching item label', function () {
            const dropdown = nonSelect();
            dropdown.trigger.value = 'x';
            dropdown.syncFromElement();
            expect(dropdown.trigger.value).to.equal('X');
            dropdown.destroy();
        });

        it('getSelectedText returns empty when the value matches no item', function () {
            const dropdown = nonSelect();
            dropdown.trigger.value = 'no-match';
            expect(dropdown.getSelectedText()).to.equal('');
            dropdown.destroy();
        });

        it('refresh keeps the existing items for a non-<select> element', function () {
            const dropdown = nonSelect();
            dropdown.refresh();
            expect(dropdown.items.map(i => i.value)).to.deep.equal(['x', 'y']);
            dropdown.destroy();
        });
    });

    describe('misc build-mode branches', function () {
        it('addOption uses the value as the label when no text is given', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('only-value');
            expect(dropdown.getSelectedText()).to.equal('only-value');
            dropdown.destroy();
        });

        it('getSelectedText is empty in build mode when nothing is selected', function () {
            const dropdown = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                allowEmpty: true,
                rememberSelection: false
            });
            dropdown.addOption('a', 'A'); // allowEmpty => not auto-selected
            expect(dropdown.getSelectedText()).to.equal('');
            dropdown.destroy();
        });
    });

    describe('portal positioning viewport fallback', function () {
        it('uses documentElement client size when window inner size is 0', function () {
            const dropdown = new SearchableDropdown({ element: document.getElementById('single'), rememberSelection: false });
            const origW = window.innerWidth;
            const origH = window.innerHeight;
            // Force the `window.innerWidth || documentElement.clientWidth` fallback branch.
            window.innerWidth = 0;
            window.innerHeight = 0;
            dropdown.trigger.getBoundingClientRect = () => ({ left: 10, right: 60, width: 50, top: 10, bottom: 30, height: 20 });
            Object.defineProperty(dropdown.optionsEl, 'offsetWidth', { value: 40, configurable: true });
            Object.defineProperty(dropdown.optionsEl, 'offsetHeight', { value: 40, configurable: true });
            expect(() => dropdown._position()).to.not.throw();
            window.innerWidth = origW;
            window.innerHeight = origH;
            dropdown.destroy();
        });
    });

    describe('defensive _visibleItems fallback while open', function () {
        // These `_visibleItems || this.items` guards only differ when the popup is open but
        // _visibleItems is unset — a state normal flows never produce (open always renders first).
        // Force it to exercise the guard.
        it('selectValue falls back to items when _visibleItems is unset', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            dropdown.addOption('a', 'A');
            dropdown.addOption('b', 'B');
            dropdown._open();
            dropdown._visibleItems = null;
            dropdown.selectValue('b');
            expect(dropdown.itemsEl.children.length).to.equal(2);
            dropdown.destroy();
        });

        it('selectMultipleValues falls back to items when _visibleItems is unset', function () {
            const dropdown = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                multiselect: true,
                rememberSelection: false
            });
            dropdown.addOption('a', 'A');
            dropdown.addOption('b', 'B');
            dropdown._open();
            dropdown._visibleItems = null;
            dropdown.selectMultipleValues(['a']);
            expect(dropdown.itemsEl.children.length).to.equal(2);
            dropdown.destroy();
        });

        it('option class mutate falls back to items when _visibleItems is unset', function () {
            const dropdown = new SearchableDropdown({ selectContainer: document.getElementById('build-container'), rememberSelection: false });
            const handle = dropdown.addOption('a', 'A');
            dropdown._open();
            dropdown._visibleItems = null;
            handle.label.classList.add('parent');
            expect(dropdown.itemsEl.querySelector('.option').classList.contains('parent')).to.be.true;
            dropdown.destroy();
        });
    });

    describe('editable (free-text) mode', function () {
        function editableInput(initial) {
            const input = document.createElement('input');
            input.type = 'text';
            if (initial !== undefined) input.value = initial;
            document.body.appendChild(input);
            return input;
        }

        it('has an editable trigger, no popup search box, and seeds from the input value', function () {
            const dd = new SearchableDropdown({
                element: editableInput('42'), editable: true, rememberSelection: false,
                items: [{ value: '1', label: 'One' }, { value: '2', label: 'Two' }]
            });
            expect(dd.editable).to.be.true;
            expect(dd.trigger.readOnly).to.be.false;
            expect(dd._hasSearchBox).to.be.false;
            expect(dd.searchInput).to.be.undefined;
            expect(dd.trigger.value).to.equal('42');
            // The container is flagged .editable so CSS can drop the combobox chevron.
            expect(dd.container.classList.contains('editable')).to.be.true;
            dd.destroy();
        });

        it('focus opens the full list', function () {
            const dd = new SearchableDropdown({
                element: editableInput(''), editable: true, rememberSelection: false,
                items: [{ value: '1', label: 'One' }, { value: '2', label: 'Two' }]
            });
            dd.trigger.dispatchEvent(new Event('focus'));
            expect(dd.isOpen).to.be.true;
            expect(dd.itemsEl.children.length).to.equal(2);
            dd.destroy();
        });

        it('focus on a field that already holds a value does not open the popup', function () {
            const dd = new SearchableDropdown({
                element: editableInput('A'), editable: true, rememberSelection: false,
                items: [{ value: 'A', label: 'A' }, { value: 'B', label: 'B' }]
            });
            dd.trigger.dispatchEvent(new Event('focus'));
            expect(dd.isOpen).to.be.false;
            dd.destroy();
        });

        it('typing sanitises via inputFilter and filters the list', function () {
            const dd = new SearchableDropdown({
                element: editableInput(''), editable: true, rememberSelection: false,
                inputFilter: v => v.replace(/\D/g, ''),
                items: [{ value: '10', label: '10 ten' }, { value: '22', label: '22 two' }]
            });
            dd.trigger.value = '1a0';
            dd.trigger.dispatchEvent(new Event('input'));
            expect(dd.trigger.value).to.equal('10');
            expect(dd.isOpen).to.be.true;
            expect(dd.itemsEl.children.length).to.equal(1);
            dd.destroy();
        });

        it('typing without an inputFilter still filters', function () {
            const dd = new SearchableDropdown({
                element: editableInput(''), editable: true, rememberSelection: false,
                items: [{ value: 'a', label: 'Apple' }, { value: 'b', label: 'Banana' }]
            });
            dd.trigger.value = 'ban';
            dd.trigger.dispatchEvent(new Event('input'));
            expect(dd.trigger.value).to.equal('ban');
            expect(dd.itemsEl.children.length).to.equal(1);
            dd.destroy();
        });

        it('typing a value matching no suggestion closes the popup (no "No matches" box)', function () {
            const az = Array.from({ length: 26 }, (_, i) => {
                const l = String.fromCharCode(65 + i);
                return { value: l, label: l };
            });
            const dd = new SearchableDropdown({
                element: editableInput(''), editable: true, rememberSelection: false,
                inputFilter: v => v.replace(/[^A-Za-z]/g, '').toUpperCase(),
                items: az
            });
            // "A" matches an A–Z hint → popup open with the single suggestion.
            dd.trigger.value = 'A';
            dd.trigger.dispatchEvent(new Event('input'));
            expect(dd.isOpen).to.be.true;
            expect(dd.itemsEl.children.length).to.equal(1);
            // Extend to "AA" — valid free text, no single-letter hint matches → popup closes, and it
            // must NOT show the "No matches" empty state.
            dd.trigger.value = 'AA';
            dd.trigger.dispatchEvent(new Event('input'));
            expect(dd.isOpen).to.be.false;
            expect(dd.itemsEl.querySelector('.sd-empty')).to.equal(null);
            dd.destroy();
        });

        it('_open() stays closed when no suggestion matches the seeded free text', function () {
            const dd = new SearchableDropdown({
                element: editableInput('AA'), editable: true, rememberSelection: false,
                items: [{ value: 'A', label: 'A' }, { value: 'B', label: 'B' }]
            });
            dd._open();
            expect(dd.isOpen).to.be.false;
            dd.destroy();
        });

        it('_open() pre-highlights the matching option against the FILTERED list, not full items', function () {
            const dd = new SearchableDropdown({
                element: editableInput('api'), editable: true, rememberSelection: false,
                items: [{ value: 'apple', label: 'apple (A)' }, { value: 'api', label: 'api (I)' }]
            });
            dd._open();
            // Only 'api (I)' matches the filter → rendered at index 0. activeIndex must point at it
            // (using the full-items index 1 would mark nothing active).
            expect(dd.itemsEl.querySelectorAll('.option').length).to.equal(1);
            expect(dd.activeIndex).to.equal(0);
            const active = dd.itemsEl.querySelector('.option.active');
            expect(active).to.exist;
            expect(active.textContent).to.contain('api');
            dd.destroy();
        });

        it('syncFromElement mirrors the wrapped input value verbatim (free text, not label)', function () {
            const input = editableInput('');
            const dd = new SearchableDropdown({
                element: input, editable: true, rememberSelection: false,
                items: [{ value: 'A', label: 'Alpha' }]
            });
            // Free text not in the item list must survive (old code blanked it to '').
            input.value = 'ZZ';
            dd.syncFromElement();
            expect(dd.trigger.value).to.equal('ZZ');
            // A value that IS an item shows as its raw value, not the label.
            input.value = 'A';
            dd.syncFromElement();
            expect(dd.trigger.value).to.equal('A');
            // Clearing the wrapped input clears the trigger.
            input.value = '';
            dd.syncFromElement();
            expect(dd.trigger.value).to.equal('');
            dd.destroy();
        });

        it('the value setter mirrors onto the wrapped input in editable mode', function () {
            const input = editableInput('');
            const dd = new SearchableDropdown({
                element: input, editable: true, rememberSelection: false,
                items: [{ value: 'A', label: 'A' }]
            });
            dd.value = 'ZZ';
            expect(dd.trigger.value).to.equal('ZZ');
            expect(input.value).to.equal('ZZ'); // backing <input> kept in sync, not left stale
            dd.destroy();
        });

        it('selecting an option commits its value (not label) and mirrors onto the input', function () {
            const input = editableInput('');
            let fired = 0; input.addEventListener('change', () => fired++);
            const dd = new SearchableDropdown({
                element: input, editable: true, rememberSelection: false,
                items: [{ value: '12345', label: '12345 revision' }]
            });
            dd.selectItem(dd.items[0]);
            expect(dd.trigger.value).to.equal('12345');
            expect(input.value).to.equal('12345');
            expect(fired).to.equal(1);
            dd.destroy();
        });

        it('Enter on a highlighted option selects it', function () {
            const input = editableInput('');
            const dd = new SearchableDropdown({
                element: input, editable: true, rememberSelection: false,
                items: [{ value: 'x', label: 'X' }]
            });
            dd._open();
            dd.activeIndex = 0;
            dd._handleEnter();
            expect(input.value).to.equal('x');
            dd.destroy();
        });

        it('Enter with no highlighted option commits the free text', function () {
            const input = editableInput('');
            const dd = new SearchableDropdown({
                element: input, editable: true, rememberSelection: false,
                items: [{ value: '1', label: 'One' }]
            });
            dd._open();
            dd.trigger.value = '999';
            dd.activeIndex = -1;
            dd._handleEnter();
            expect(input.value).to.equal('999');
            dd.destroy();
        });

        it('Enter commits the free text while the popup is closed (no matches)', function () {
            const input = editableInput('');
            let fired = 0; input.addEventListener('change', () => fired++);
            const dd = new SearchableDropdown({
                element: input, editable: true, rememberSelection: false,
                items: [{ value: 'A', label: 'A' }]
            });
            // Free text matching no suggestion → popup stays closed.
            dd.trigger.value = 'ZZ';
            dd.trigger.dispatchEvent(new Event('input'));
            expect(dd.isOpen).to.be.false;
            // Enter must still commit (and preventDefault the event so a form can't submit stale).
            const evt = new KeyboardEvent('keydown', { key: 'Enter', cancelable: true });
            dd.trigger.dispatchEvent(evt);
            expect(input.value).to.equal('ZZ');
            expect(fired).to.equal(1);
            expect(evt.defaultPrevented).to.be.true;
            dd.destroy();
        });

        it('blur commits the free text once, and a second blur with no change does nothing', function () {
            const input = editableInput('');
            let fired = 0; input.addEventListener('change', () => fired++);
            const dd = new SearchableDropdown({ element: input, editable: true, rememberSelection: false, items: [] });
            dd.trigger.value = 'abc';
            dd.trigger.dispatchEvent(new Event('blur'));
            expect(input.value).to.equal('abc');
            expect(fired).to.equal(1);
            dd.trigger.dispatchEvent(new Event('blur'));
            expect(fired).to.equal(1);
            dd.destroy();
        });

        it('selectItem(null) clears the editable field and the wrapped input', function () {
            const input = editableInput('');
            const dd = new SearchableDropdown({
                element: input, editable: true, rememberSelection: false,
                items: [{ value: '7', label: 'Seven' }]
            });
            dd.selectItem(dd.items[0]);
            expect(input.value).to.equal('7');
            dd.selectItem(null);
            expect(dd.trigger.value).to.equal('');
            expect(input.value).to.equal('');
            dd.destroy();
        });

        it('build-mode editable has no <input> to mirror onto — blur is a no-op', function () {
            const dd = new SearchableDropdown({
                selectContainer: document.getElementById('build-container'),
                editable: true, rememberSelection: false
            });
            dd.addOption('a', 'Apple');
            expect(dd.originalElement).to.equal(null);
            dd.trigger.value = 'free';
            expect(() => dd.trigger.dispatchEvent(new Event('blur'))).to.not.throw();
            dd.destroy();
        });
    });
});
