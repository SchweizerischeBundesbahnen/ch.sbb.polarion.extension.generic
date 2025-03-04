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
});
