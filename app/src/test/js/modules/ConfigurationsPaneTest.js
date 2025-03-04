import ConfigurationsPane from '../../../main/resources/js/modules/ConfigurationsPane.js';
import CustomSelect from '../../../main/resources/js/modules/CustomSelect.js';
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
                <div id="configurations-select"></div>
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

        ctxMock = {
            onClick: sinon.spy(),
            callAsync: sinon.stub(),
            setCookie: sinon.spy(),
            getCookie: sinon.stub().returns(null),
            extension: 'testExtension',
            setting: 'testSetting',
            scope: 'testScope',
            readAndFillRevisions: sinon.spy()
        };

        sinon.stub(CustomSelect.prototype, 'addOption').returns({ checkbox: {}, label: {} });
        sinon.stub(CustomSelect.prototype, 'empty');
        sinon.stub(CustomSelect.prototype, 'selectValue');
        sinon.stub(CustomSelect.prototype, 'getSelectedValue').returns('testConfig');
        sinon.stub(CustomSelect.prototype, 'getAllCheckboxes').returns([]);
        sinon.stub(CustomSelect.prototype, 'handleChange');

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
        delete global.window;
        delete global.document;
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
});
