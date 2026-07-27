import CustomSelect from "../../../main/resources/js/modules/CustomSelect.js";
import { expect } from 'chai';
import { JSDOM } from 'jsdom';

describe('CustomSelect', () => {
    let dom, window, document;

    beforeEach(() => {
        dom = new JSDOM(`<!DOCTYPE html><html lang="en"><body></body></html>`);
        window = dom.window;
        document = window.document;

        // Make sure CustomSelect uses the correct window and document
        global.window = window;
        global.document = document;
    });

    afterEach(() => {
        delete global.window;
        delete global.document;
    });

    it('should create a select container', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);

        const select = new CustomSelect({ selectContainer });
        expect(select.selectContainer).to.exist;
        expect(select.selectContainer.classList.contains('sbb-custom-select')).to.be.true;
    });

    it('should toggle checkbox container visibility', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);

        const select = new CustomSelect({ selectContainer });
        expect(select.checkboxContainer.style.display).to.equal('none');
        select.toggleCheckboxContainer();
        expect(select.checkboxContainer.style.display).to.equal('block');
        select.toggleCheckboxContainer();
        expect(select.checkboxContainer.style.display).to.equal('none');
    });

    it('should add an option', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);

        const select = new CustomSelect({ selectContainer });
        const { checkbox, label } = select.addOption('1', 'Option 1');
        expect(select.containsOption('1')).to.be.true;
        expect(label.textContent).to.include('Option 1');
        expect(checkbox.value).to.equal('1');
    });

    it('should return selected value', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);

        const select = new CustomSelect({ selectContainer });
        select.addOption('1', 'Option 1');
        select.addOption('2', 'Option 2');

        const checkboxes = select.getAllCheckboxes();
        checkboxes[0].checked = true;
        expect(select.getSelectedValue()).to.equal('1');
    });

    it('should select multiple values in multiselect mode', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);

        const select = new CustomSelect({ selectContainer, multiselect: true });
        select.addOption('1', 'Option 1');
        select.addOption('2', 'Option 2');
        select.selectMultipleValues(['1', '2']);
        expect(select.getSelectedValue()).to.deep.equal(['1', '2']);
    });

    it('containsOption reports whether an option value exists', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);
        const select = new CustomSelect({ selectContainer });
        select.addOption('a', 'A');
        expect(select.containsOption('a')).to.be.true;
        expect(select.containsOption('missing')).to.be.false;
    });

    it('multiselect: reflects selection in value, text and fires the change listener', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);
        let fired = 0;
        const select = new CustomSelect({ selectContainer, multiselect: true, changeListener: () => fired++ });
        select.addOption('a', 'A');
        select.addOption('b', 'B');
        select.selectMultipleValues(['a', 'b']);
        expect(select.getSelectedValue()).to.deep.equal(['a', 'b']);
        expect(select.getSelectedText()).to.deep.equal(['A', 'B']);
        expect(fired).to.be.greaterThan(0);
    });

    it('single-select: selectValue picks one and getSelectedText returns its label', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);
        const select = new CustomSelect({ selectContainer });
        select.addOption('a', 'A');
        select.addOption('b', 'B');
        select.selectValue('b');
        expect(select.getSelectedValue()).to.equal('b');
        expect(select.getSelectedText()).to.equal('B');
    });

    it('single-select: empty selection reads as "", and un-checking the last option re-selects it', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);
        let last = null;
        const select = new CustomSelect({ selectContainer, changeListener: (cb) => { last = cb; } });
        select.addOption('a', 'A');
        select.addOption('b', 'B');
        expect(select.getSelectedValue()).to.equal(''); // nothing selected yet
        expect(select.getSelectedText()).to.equal('');
        const [cbA, cbB] = select.getAllCheckboxes();
        cbA.checked = true;
        cbA.dispatchEvent(new window.Event('change'));
        expect(select.getSelectedValue()).to.equal('a');
        expect(last).to.equal(cbA);
        // selecting B enforces single selection (A is un-checked)
        cbB.checked = true;
        cbB.dispatchEvent(new window.Event('change'));
        expect(cbA.checked).to.be.false;
        expect(select.getSelectedValue()).to.equal('b');
        // clearing the last one is not allowed — it is re-checked
        cbB.checked = false;
        cbB.dispatchEvent(new window.Event('change'));
        expect(cbB.checked).to.be.true;
    });

    it('wires the <label for> to the hidden select when a label is given', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);
        const label = document.createElement('label');
        const select = new CustomSelect({ selectContainer, label });
        expect(label.htmlFor).to.equal(select.selectElement.getAttribute('id') || '');
    });

    it('closes on an outside click, stays open on an inside click, and empty() clears the options', () => {
        const selectContainer = document.createElement('div');
        document.body.appendChild(selectContainer);
        const select = new CustomSelect({ selectContainer });
        select.addOption('a', 'A');
        select.toggleCheckboxContainer(); // open
        expect(select.checkboxContainer.style.display).to.equal('block');
        // click inside the container → treated as a child → stays open
        select.checkboxContainer.dispatchEvent(new window.Event('click', { bubbles: true }));
        expect(select.checkboxContainer.style.display).to.equal('block');
        // click outside → not a child → closes
        document.body.dispatchEvent(new window.Event('click', { bubbles: true }));
        expect(select.checkboxContainer.style.display).to.equal('none');
        // empty() removes all options
        select.empty();
        expect(select.getAllCheckboxes().length).to.equal(0);
    });

    it('defaults a missing container to a new element and an option without text to its value', () => {
        const select = new CustomSelect({}); // no selectContainer → one is created
        expect(select.selectContainer).to.exist;
        select.addOption('only-value'); // no text → the label falls back to the value
        const [cb] = select.getAllCheckboxes();
        expect(cb.parentElement.textContent).to.equal('only-value');
    });
});
