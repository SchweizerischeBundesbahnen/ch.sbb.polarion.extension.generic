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
