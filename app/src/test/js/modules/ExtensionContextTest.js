import ExtensionContext from '../../../main/resources/js/modules/ExtensionContext.js';
import { expect } from 'chai';
import sinon from 'sinon';
import {JSDOM} from "jsdom";

describe('ExtensionContext', function () {
    let context, sandbox, fakeDocument, dom, window, document;

    beforeEach(function () {
        dom = new JSDOM(`<!DOCTYPE html><html lang="en"><body></body></html>`);
        window = dom.window;
        document = window.document;

        global.window = window;
        global.document = document;

        sandbox = sinon.createSandbox();
        fakeDocument = sandbox.stub(document, 'querySelector');
        context = new ExtensionContext({ rootComponentSelector: '#root' });
    });

    afterEach(function () {
        delete global.window;
        delete global.document;
        if (sandbox) {
            sandbox.restore();
        }
    });

    it('should initialize with default values', function () {
        expect(context.extension).to.equal('');
        expect(context.setting).to.equal('');
        expect(context.rootComponentSelector).to.equal('#root');
    });

    it('should query elements with rootComponentSelector', function () {
        fakeDocument.withArgs('#root #testId').returns({ value: 'testValue' });
        const result = context.getElementById('testId');
        expect(result.value).to.equal('testValue');
    });

    it('should set and get value by element ID', function () {
        const fakeElement = { value: '' };
        fakeDocument.withArgs('#root #inputId').returns(fakeElement);

        context.setValueById('inputId', 'newValue');
        expect(fakeElement.value).to.equal('newValue');
        expect(context.getValueById('inputId')).to.equal('newValue');
    });

    it('should add and trigger click event listener', function () {
        const fakeElement = {
            addEventListener: sandbox.spy(),
            removeAttribute: sandbox.spy()
        };
        fakeDocument.withArgs('#root #button').returns(fakeElement);

        const clickHandler = sandbox.spy();
        context.onClick('button', clickHandler);

        expect(fakeElement.addEventListener.calledWith('click', clickHandler)).to.be.true;
    });

    it('should call async request and handle success', function (done) {
        global.XMLHttpRequest = sinon.useFakeXMLHttpRequest();
        let requests = [];
        global.XMLHttpRequest.onCreate = (xhr) => requests.push(xhr);

        const successCallback = sinon.spy();
        const errorCallback = sinon.spy();

        context.callAsync({
            method: 'GET',
            url: '/test',
            onOk: successCallback,
            onError: errorCallback
        });

        expect(requests).to.have.length(1);
        requests[0].respond(200, { 'Content-Type': 'application/json' }, '{}');

        setTimeout(() => {
            expect(successCallback.calledOnce).to.be.true;
            expect(errorCallback.called).to.be.false;
            done();
        }, 10);
    });
});
