import { initNumericSpinners, enhanceNumericInput } from '../../../main/resources/js/modules/NumericSpinner.js';
import { expect } from 'chai';
import { JSDOM } from 'jsdom';

describe('NumericSpinner', function () {
    let dom, document;

    beforeEach(function () {
        dom = new JSDOM('<!DOCTYPE html><html lang="en"><body></body></html>');
        global.window = dom.window;
        global.document = dom.window.document;
        global.Event = dom.window.Event;
        document = dom.window.document;
    });

    afterEach(function () {
        delete global.window;
        delete global.document;
        delete global.Event;
    });

    function makeInput(attrs = {}) {
        const input = document.createElement('input');
        input.type = 'number';
        Object.entries(attrs).forEach(([k, v]) => input.setAttribute(k, v));
        document.body.appendChild(input);
        return input;
    }

    function counters(input) {
        const c = { input: 0, change: 0 };
        input.addEventListener('input', () => c.input++);
        input.addEventListener('change', () => c.change++);
        return c;
    }

    it('wraps the input in .sbb-number with up/down caret buttons', function () {
        const input = makeInput({ value: '1' });
        enhanceNumericInput(input);

        const wrap = input.parentNode;
        expect(wrap.className).to.equal('sbb-number');
        const buttons = wrap.querySelectorAll('.sbb-number-spin button');
        expect(buttons.length).to.equal(2);
        expect(buttons[0].getAttribute('aria-label')).to.equal('Increment');
        expect(buttons[1].getAttribute('aria-label')).to.equal('Decrement');
        expect(input.dataset.sbbSpinner).to.equal('1');
    });

    it('is idempotent — a second enhance does not re-wrap', function () {
        const input = makeInput({ value: '1' });
        enhanceNumericInput(input);
        const wrap = input.parentNode;
        enhanceNumericInput(input);
        expect(input.parentNode).to.equal(wrap);
        expect(document.querySelectorAll('.sbb-number-spin').length).to.equal(1);
    });

    it('initNumericSpinners enhances only input[type=number]', function () {
        makeInput({ value: '1' });
        const text = document.createElement('input');
        text.type = 'text';
        document.body.appendChild(text);

        initNumericSpinners(document);

        expect(document.querySelectorAll('.sbb-number').length).to.equal(1);
        expect(text.dataset.sbbSpinner).to.be.undefined;
    });

    it('up caret increments and fires input+change; down decrements', function () {
        const input = makeInput({ value: '2', step: '1' });
        enhanceNumericInput(input);
        const c = counters(input);
        const [up, down] = input.parentNode.querySelectorAll('.sbb-number-spin button');

        up.click();
        expect(input.value).to.equal('3');
        expect(c.input).to.equal(1);
        expect(c.change).to.equal(1);

        down.click();
        expect(input.value).to.equal('2');
        expect(c.input).to.equal(2);
        expect(c.change).to.equal(2);
    });

    it('does not fire events when clamped at max (no phantom change)', function () {
        const input = makeInput({ value: '5', min: '0', max: '5', step: '1' });
        enhanceNumericInput(input);
        const c = counters(input);
        const up = input.parentNode.querySelector('.sbb-number-spin button');

        up.click();
        expect(input.value).to.equal('5'); // unchanged — already at max
        expect(c.input).to.equal(0);
        expect(c.change).to.equal(0);
    });

    it('does not fire events when clamped at min (no phantom change)', function () {
        const input = makeInput({ value: '0', min: '0', max: '5', step: '1' });
        enhanceNumericInput(input);
        const c = counters(input);
        const down = input.parentNode.querySelectorAll('.sbb-number-spin button')[1];

        down.click();
        expect(input.value).to.equal('0'); // unchanged — already at min
        expect(c.input).to.equal(0);
        expect(c.change).to.equal(0);
    });

    it('ignores clicks on a disabled or readonly input', function () {
        const disabled = makeInput({ value: '1', disabled: '' });
        enhanceNumericInput(disabled);
        disabled.parentNode.querySelector('.sbb-number-spin button').click();
        expect(disabled.value).to.equal('1');

        const readonly = makeInput({ value: '1', readonly: '' });
        enhanceNumericInput(readonly);
        readonly.parentNode.querySelector('.sbb-number-spin button').click();
        expect(readonly.value).to.equal('1');
    });
});
