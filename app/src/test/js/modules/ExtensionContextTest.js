import ExtensionContext from '../../../main/resources/js/modules/ExtensionContext.js';
import { expect } from 'chai';
import sinon from 'sinon';
import nise from 'nise';
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
        global.XMLHttpRequest = nise.fakeXhr.useFakeXMLHttpRequest();
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

// Real-DOM coverage of the DOM/alert/cookie/revisions helpers (no querySelector stub).
describe('ExtensionContext — DOM-backed helpers', function () {
    let dom, window, document, ctx, requests, clock;

    beforeEach(function () {
        clock = sinon.useFakeTimers(); // control showActionAlert's 5s auto-hide so no real timer leaks
        dom = new JSDOM(`<!DOCTYPE html><html lang="en"><body><div id="root">
            <input id="inputId" value="init"/>
            <input id="checkId" type="checkbox"/>
            <select id="selId"><option value="a">A</option><option value="Default">Default</option></select>
            <div id="box"></div>
            <div id="newer-version-warning"></div>
            <div id="data-loading-error"></div>
            <div id="revisions-loading-error"></div>
            <div id="revisions-expand-container" style="display:none"></div>
            <div class="action-alerts"><div id="action-success" class="alert"></div><div id="action-error" class="alert"></div></div>
            <table id="revisions-table"><tbody></tbody></table>
        </div></body></html>`, { url: 'http://localhost/' });
        window = dom.window;
        document = window.document;
        global.window = window;
        global.document = document;
        global.alert = sinon.stub();
        global.confirm = sinon.stub().returns(false);
        global.XMLHttpRequest = nise.fakeXhr.useFakeXMLHttpRequest();
        requests = [];
        global.XMLHttpRequest.onCreate = (xhr) => requests.push(xhr);
        ctx = new ExtensionContext({ extension: 'ext', setting: 'set', rootComponentSelector: '#root', scope: 's' });
    });

    afterEach(function () {
        sinon.restore();
        delete global.window;
        delete global.document;
        delete global.alert;
        delete global.confirm;
        delete global.XMLHttpRequest;
        // Browser libs some tests stub ad hoc — cleared here so a mid-test assertion failure
        // can't leak them into later tests.
        delete global.codeInput;
        delete global.Prism;
        delete global.$;
    });

    it('reads/writes input, checkbox, selector and toggles visibility', function () {
        ctx.setValue('inputId', 'x');
        expect(ctx.getValue('inputId')).to.equal('x');
        ctx.setCheckbox('checkId', true);
        expect(ctx.getCheckbox('checkId')).to.be.true;
        expect(ctx.containsOption(ctx.getElementById('selId'), 'a')).to.be.true;
        ctx.setSelector('selId', 'a');
        expect(ctx.getValue('selId')).to.equal('a');
        ctx.setSelector('selId', 'missing'); // unknown → DEFAULT
        expect(ctx.getValue('selId')).to.equal('Default');
        ctx.selectOptionByValue('selId', 'a');
        expect(ctx.getValue('selId')).to.equal('a');
        ctx.displayIf('box', false); expect(ctx.getElementById('box').style.display).to.equal('none');
        ctx.displayIf('box', true); expect(ctx.getElementById('box').style.display).to.equal('block');
        ctx.visibleIf('box', false); expect(ctx.getElementById('box').style.visibility).to.equal('hidden');
        ctx.disableIf('inputId', true); expect(ctx.getElementById('inputId').disabled).to.be.true;
        expect(ctx.querySelectorAll('.alert')).to.have.length(2);
        expect(ctx.isWindowDefined()).to.be.true;
    });

    it('syncs a wrapped SearchableDropdown when the value is set programmatically', function () {
        const sync = sinon.spy();
        ctx.getElementById('inputId')._searchableDropdown = { syncFromElement: sync };
        ctx.setValueById('inputId', 'y');
        expect(sync.calledOnce).to.be.true;
        ctx.getElementById('selId')._searchableDropdown = { syncFromElement: sync };
        ctx.setSelector('selId', 'a');
        expect(sync.calledTwice).to.be.true;
    });

    it('adds listeners, warns for a missing element and errors on an invalid pair', function () {
        const onClick = sinon.spy();
        ctx.onClick('box', onClick);
        ctx.getElementById('box').dispatchEvent(new window.Event('click'));
        expect(onClick.calledOnce).to.be.true;
        const warn = sinon.stub(console, 'log');
        ctx.onChange('does-not-exist', () => {});
        expect(warn.calledOnce).to.be.true;
        const err = sinon.stub(console, 'error');
        ctx.onBlur(42, 'not-a-function'); // invalid pair
        expect(err.calledOnce).to.be.true;
    });

    it('shows and auto-hides action alerts, and delegates the canned alerts', function () {
        ctx.showActionAlert({ containerId: 'action-success', message: 'hi' });
        expect(document.getElementById('action-success').style.display).to.equal('inline-block');
        clock.tick(5000);
        expect(document.getElementById('action-success').style.display).to.equal('none');
        ctx.showActionAlert({ containerId: 'nope', message: '' }); // no container/message → no throw
        ctx.showRevertedToRevisionAlert({ name: '123' });
        expect(document.getElementById('action-success').innerHTML).to.contain('123');
        ctx.showRevertedToDefaultAlert();
        ctx.showSaveSuccessAlert();
        ctx.showSaveErrorAlert();
        expect(document.getElementById('action-error').innerHTML).to.contain('Error');
    });

    it('toggles the three notification banners', function () {
        ctx.setNewerVersionNotificationVisible(true);
        expect(ctx.getElementById('newer-version-warning').style.display).to.equal('block');
        ctx.setLoadingErrorNotificationVisible(false);
        expect(ctx.getElementById('data-loading-error').style.display).to.equal('none');
        ctx.setRevisionsLoadingErrorNotificationVisible(true);
        expect(ctx.getElementById('revisions-loading-error').style.display).to.equal('block');
    });

    it('cancelEdit reloads only when confirmed', function () {
        // jsdom's location.reload is not stubbable and just logs "Not implemented"; exercise both
        // branches (confirm false → no-op, confirm true → reload path) via the confirm stub.
        global.confirm.returns(false);
        ctx.cancelEdit();
        expect(global.confirm.calledOnce).to.be.true;
        global.confirm.returns(true);
        ctx.cancelEdit(); // takes the reload branch
    });

    it('toggleRevisions flips the panel display', function () {
        ctx.toggleRevisions(); // was none → block
        expect(document.getElementById('revisions-expand-container').style.display).to.equal('block');
    });

    it('insertRevisionSpaces groups digits in threes from the right', function () {
        expect(ctx.insertRevisionSpaces('12345')).to.equal('12 345');
    });

    it('sets, reads and deletes cookies (trimming the leading space of later cookies)', function () {
        ctx.setCookie('k', 'v v');
        ctx.setCookie('k2', '2'); // a second cookie is stored after "; " → its leading space is trimmed
        expect(ctx.getCookie('k')).to.equal('v v');
        expect(ctx.getCookie('k2')).to.equal('2');
        expect(ctx.getCookie('absent')).to.equal(null);
        ctx.deleteCookie('k');
        expect(ctx.getCookie('k')).to.equal(null);
    });

    it('constructor wires code-input highlighting when requested', function () {
        global.codeInput = { registerTemplate: sinon.spy(), templates: { prism: sinon.stub().returns({}) } };
        global.Prism = { languages: {} };
        new ExtensionContext({ initCodeInput: true, propertiesHighlighting: true });
        expect(global.codeInput.registerTemplate.calledOnce).to.be.true;
        expect(global.Prism.languages.properties).to.exist;
    });

    it('getJQueryElement and querySelector delegate to the root-scoped selectors', function () {
        global.$ = sinon.stub().returns('jq');
        expect(ctx.getJQueryElement('.x')).to.equal('jq');
        expect(global.$.calledWith('#root .x')).to.be.true;
        delete global.$;
        expect(ctx.querySelector('.alert')).to.equal(document.querySelector('#root .alert'));
    });

    it('callAsync honours a responseType and still calls onOk on success', function () {
        const onOk = sinon.spy();
        ctx.callAsync({ method: 'GET', url: '/x', responseType: 'text', onOk });
        requests[0].respond(200, {}, 'ok');
        expect(onOk.calledOnce).to.be.true;
    });

    it('callAsync falls back to a plain alert when the error alert itself throws', function () {
        sinon.stub(ctx, 'showActionAlert').throws(new Error('boom'));
        ctx.callAsync({ method: 'GET', url: '/x' });
        requests[0].respond(400, {}, 'x');
        expect(global.alert.called).to.be.true;
    });

    it('covers the remaining conditional branches', function () {
        // scopeFieldId path in the constructor derives the scope from the field's value
        const c = new ExtensionContext({ rootComponentSelector: '#root', scopeFieldId: 'inputId' });
        expect(c.scope).to.equal('init');
        // visibleIf(true) and the "off" side of the notification toggles
        ctx.visibleIf('box', true);
        expect(ctx.getElementById('box').style.visibility).to.equal('visible');
        ctx.setNewerVersionNotificationVisible(false);
        expect(ctx.getElementById('newer-version-warning').style.display).to.equal('none');
        ctx.setLoadingErrorNotificationVisible(true);
        expect(ctx.getElementById('data-loading-error').style.display).to.equal('block');
        ctx.setRevisionsLoadingErrorNotificationVisible(false);
        // toggleRevisions the other way (block → none)
        ctx.toggleRevisions();
        ctx.toggleRevisions();
        expect(document.getElementById('revisions-expand-container').style.display).to.equal('none');
        // createRevisionRow renders an empty baseline cell when the revision has none
        const tbody = document.querySelector('#revisions-table tbody');
        ctx.createRevisionRow(tbody, { name: '1', date: 'd', author: 'a', description: 'x' }, document.createElement('button'));
        expect(tbody.querySelectorAll('tr').length).to.equal(1);
        // a network error without onError falls back to alert
        ctx.callAsync({ method: 'GET', url: '/x' });
        requests[requests.length - 1].error();
        expect(global.alert.called).to.be.true;
    });

    it('callAsync blocks directory traversal', function () {
        const onError = sinon.spy();
        ctx.callAsync({ method: 'GET', url: '/x/../y', onError });
        expect(onError.calledWith(undefined, 'directory traversal restricted')).to.be.true;
        ctx.callAsync({ method: 'GET', url: '/x/../y' }); // no onError → alert
        expect(global.alert.calledOnce).to.be.true;
    });

    it('callAsync reports a backend error via onError with the parsed message', function () {
        const onError = sinon.spy();
        ctx.callAsync({ method: 'GET', url: '/x', onError });
        requests[0].respond(500, { 'Content-Type': 'application/json' }, '{"message":"boom"}');
        expect(onError.calledWith(500, 'boom')).to.be.true;
    });

    it('callAsync without onError shows the error alert on failure', function () {
        ctx.callAsync({ method: 'GET', url: '/x' });
        requests[0].respond(400, { 'Content-Type': 'text/plain' }, 'not json');
        expect(document.getElementById('action-error').style.display).to.equal('inline-block');
    });

    it('callAsync onerror routes to onError', function () {
        const onError = sinon.spy();
        ctx.callAsync({ method: 'GET', url: '/x', onError });
        requests[0].error();
        expect(onError.called).to.be.true;
    });

    it('getStringIfTextResponse returns text only for text response types', function () {
        expect(ctx.getStringIfTextResponse({ responseType: 'text', responseText: 'a' })).to.equal('a');
        expect(ctx.getStringIfTextResponse({ responseType: 'blob', responseText: 'a' })).to.equal(undefined);
    });

    it('downloadBlob creates a temporary object URL and clicks a link', function () {
        window.URL.createObjectURL = sinon.stub().returns('blob:x');
        window.URL.revokeObjectURL = sinon.stub();
        global.URL = window.URL;
        const click = sinon.stub(window.HTMLAnchorElement.prototype, 'click');
        ctx.downloadBlob(new window.Blob(['data']), 'file.txt');
        expect(window.URL.createObjectURL.calledOnce).to.be.true;
        expect(click.calledOnce).to.be.true;
        // Note: the webkitURL fallback and the in-iframe (window.top) download path are
        // environment-specific and not reproducible in jsdom, so they stay uncovered.
    });

    it('readAndFillRevisions renders rows and wires a revert button; empty list shows a message', function () {
        const revertCb = sinon.spy();
        ctx.readAndFillRevisions({ configurationName: 'Default', revertToRevisionCallback: revertCb });
        requests[0].respond(200, { 'Content-Type': 'application/json' },
            JSON.stringify([{ name: '100', baseline: 'b', date: 'd', author: 'a', description: 'x' }]));
        const rows = document.querySelector('#revisions-table tbody').querySelectorAll('tr');
        expect(rows.length).to.equal(1);
        // clicking the revert button fires a second request whose onOk calls the callback
        document.querySelector('.revert-to-revision-button').dispatchEvent(new window.Event('click'));
        requests[1].respond(200, { 'Content-Type': 'application/json' }, 'reverted-content');
        expect(revertCb.calledWith('reverted-content')).to.be.true;
        // now an empty list
        ctx.readAndFillRevisions({ configurationName: 'Default' });
        requests[2].respond(200, { 'Content-Type': 'application/json' }, '[]');
        expect(document.querySelector('#revisions-table .empty-message')).to.exist;
    });

    it('readAndFillRevisions shows the revisions-loading error on failure', function () {
        ctx.readAndFillRevisions({ setting: 'other' });
        requests[0].respond(500, {}, '');
        expect(ctx.getElementById('revisions-loading-error').style.display).to.equal('block');
    });
});
