import ConfigurationsPane from '../../../main/resources/js/modules/ConfigurationsPane.js';
import { expect } from 'chai';
import sinon from 'sinon';
import { JSDOM } from 'jsdom';

describe('ConfigurationsPane', function () {
    let dom, window, document, ctxMock, configPane, preDelete;

    beforeEach(function () {
        dom = new JSDOM(`<!DOCTYPE html>
        <html lang="en">
        <body>
            <div class="standard-admin-page">
                <div id="configurations-pane"></div>
                <div id="edit-configuration-pane"></div>
                <input id="new-configuration-input"/>
                <input id="edit-configuration-input"/>
                <select id="configurations-select"></select>
                <div id="configurations-label"></div>
                <div id="configurations-load-error" style="display:none"></div>
                <div id="configuration-save-error" style="display:none"></div>
                <div id="configuration-delete-error" style="display:none"></div>
            </div>
        </body>
        </html>`);

        window = dom.window;
        document = window.document;

        global.window = window;
        global.document = document;
        // jsdom's dispatchEvent requires an Event from the same realm as the element,
        // so expose the window's Event constructor as the global one the modules use.
        global.Event = window.Event;
        global.MutationObserver = window.MutationObserver;

        ctxMock = {
            onClick: sinon.spy(),
            callAsync: sinon.stub(),
            setCookie: sinon.spy(),
            getCookie: sinon.stub().returns(null),
            disableIf: sinon.spy(),
            extension: 'testExtension',
            setting: 'testSetting',
            scope: 'testScope',
            readAndFillRevisions: sinon.spy()
        };

        preDelete = {
            then: (callback) => {
                callback();
                return { catch: sinon.stub() };
            },
            catch: sinon.stub()
        };

        configPane = new ConfigurationsPane({ label: 'Test Label', ctx: ctxMock, preDeleteCallback: function() {return preDelete;}});
    });

    afterEach(function () {
        // Tear down the dropdown so its body-level portal and global listeners don't leak across tests.
        configPane.configurationsSelect.destroy();
        delete global.window;
        delete global.document;
        delete global.Event;
        delete global.MutationObserver;
        sinon.restore();
    });

    it('should initialize with correct elements and event listeners', function () {
        expect(ctxMock.onClick.called).to.be.true;
        expect(configPane.configurationsPane).to.equal(document.getElementById('configurations-pane'));
        expect(configPane.editConfigurationPane).to.equal(document.getElementById('edit-configuration-pane'));
    });

    it('should call loadConfigurationNames correctly', function () {
        ctxMock.callAsync.callsFake();
        configPane.loadConfigurationNames();
        expect(ctxMock.callAsync.calledOnce).to.be.true;
    });

    it('should handle new configuration creation', function () {
        configPane.newConfiguration();
        expect(configPane.configurationsPane.style.display).to.equal('none');
        expect(configPane.editConfigurationPane.style.display).to.equal('block');
    });

    it('should handle cancel edit configuration', function () {
        configPane.cancelEditConfiguration();
        expect(configPane.configurationsPane.style.display).to.equal('block');
        expect(configPane.editConfigurationPane.style.display).to.equal('none');
    });

    it('should call async function for deleting configuration', function () {
        global.confirm = () => true;
        sinon.stub(global, 'confirm').returns(true);
        configPane.getSelectedConfiguration = sinon.stub().returns('testConfig');
        configPane.deleteConfiguration();
        expect(ctxMock.callAsync.calledOnce).to.be.true;
    });

    it('should remove the dropdown portal on destroy', function () {
        const before = document.querySelectorAll('.sd-portal').length;
        expect(before).to.be.greaterThan(0);
        configPane.configurationsSelect.destroy();
        expect(document.querySelectorAll('.sd-portal').length).to.equal(before - 1);
    });

    it('should disable Save while the new-configuration name is empty', function () {
        configPane.newConfiguration();
        expect(ctxMock.disableIf.lastCall.args).to.deep.equal(['configurations-button-save', true]);
    });

    it('should enable Save once a non-blank name is typed', function () {
        const input = document.getElementById('new-configuration-input');
        input.value = 'my-config';
        input.dispatchEvent(new global.Event('input'));
        expect(ctxMock.disableIf.lastCall.args).to.deep.equal(['configurations-button-save', false]);
    });

    it('should keep Save disabled for a whitespace-only name', function () {
        const input = document.getElementById('new-configuration-input');
        input.value = '   ';
        input.dispatchEvent(new global.Event('input'));
        expect(ctxMock.disableIf.lastCall.args).to.deep.equal(['configurations-button-save', true]);
    });

    it('should enable Update when editing a configuration with a name', function () {
        configPane.getSelectedConfiguration = sinon.stub().returns('existing-config');
        configPane.editConfiguration();
        expect(ctxMock.disableIf.lastCall.args).to.deep.equal(['configurations-button-update', false]);
    });

    it('should disable Update when the edit name is cleared to whitespace', function () {
        const input = document.getElementById('edit-configuration-input');
        input.value = '   ';
        input.dispatchEvent(new global.Event('input'));
        expect(ctxMock.disableIf.lastCall.args).to.deep.equal(['configurations-button-update', true]);
    });
});
