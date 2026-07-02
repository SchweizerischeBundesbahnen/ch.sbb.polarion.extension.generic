import SearchableDropdown from '../../../main/resources/js/modules/SearchableDropdown.js';
import { expect } from 'chai';
import { JSDOM } from 'jsdom';

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
        </html>`);

        window = dom.window;
        document = window.document;

        global.window = window;
        global.document = document;
        global.Event = window.Event;
        global.MutationObserver = window.MutationObserver;
    });

    afterEach(function () {
        delete global.window;
        delete global.document;
        delete global.Event;
        delete global.MutationObserver;
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
});
