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
                <span class="configuration-label"></span>
                <span class="configuration-label-capitalized"></span>
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

    // Add an element with the given id/classes to the admin page (helper for the DOM the methods touch).
    function addEl(idOrClass, tag = 'div') {
        const el = document.createElement(tag);
        if (idOrClass.startsWith('.')) { el.className = idOrClass.slice(1); } else { el.id = idOrClass; }
        document.querySelector('.standard-admin-page').appendChild(el);
        return el;
    }

    it('containsInvalidCharacters accepts alphanumerics/dash/underscore/space and rejects the rest', function () {
        expect(configPane.containsInvalidCharacters('My config-1_x')).to.be.false;
        expect(configPane.containsInvalidCharacters('bad/name!')).to.be.true;
    });

    it('nameClashes detects a duplicate, ignores the renamed item and inherited (parent) options', function () {
        const sel = configPane.configSelectElement;
        sel.innerHTML = '<option value="A"></option><option value="B"></option><option value="G" class="parent"></option>';
        expect(configPane.nameClashes('A', null)).to.be.true;   // A already exists
        expect(configPane.nameClashes('A', 'A')).to.be.false;   // renaming A to A — ignore itself
        expect(configPane.nameClashes('G', null)).to.be.false;  // inherited option is not a clash
        expect(configPane.nameClashes('Z', null)).to.be.false;  // brand-new name
    });

    it('saveConfiguration shows the invalid-value error and does not call the backend', function () {
        addEl('invalid-value-error');
        configPane.newConfigurationInput.value = 'bad/name';
        configPane.saveConfiguration();
        expect(document.getElementById('invalid-value-error').style.display).to.equal('block');
        expect(ctxMock.callAsync.called).to.be.false;
    });

    it('saveConfiguration shows the clash error when the name already exists', function () {
        addEl('configuration-clashes-error');
        configPane.configSelectElement.innerHTML = '<option value="dup"></option>';
        configPane.newConfigurationInput.value = 'dup';
        configPane.saveConfiguration();
        expect(document.getElementById('configuration-clashes-error').style.display).to.equal('block');
        expect(ctxMock.callAsync.called).to.be.false;
    });

    it('saveConfiguration PUTs the new configuration and reloads on success', function () {
        configPane.newConfigurationInput.value = 'fresh';
        configPane.loadConfigurationNames = sinon.spy();
        ctxMock.callAsync.callsFake(({ onOk }) => onOk());
        configPane.saveConfiguration();
        expect(ctxMock.callAsync.calledOnce).to.be.true;
        expect(ctxMock.setCookie.called).to.be.true;
        expect(configPane.newConfigurationInput.value).to.equal('');
        expect(configPane.loadConfigurationNames.calledOnce).to.be.true;
    });

    it('saveConfiguration shows the save error on backend failure', function () {
        addEl('configuration-save-error');
        configPane.newConfigurationInput.value = 'fresh';
        ctxMock.callAsync.callsFake(({ onError }) => onError());
        configPane.saveConfiguration();
        expect(document.getElementById('configuration-save-error').style.display).to.equal('block');
    });

    it('updateConfiguration POSTs the rename and reloads on success', function () {
        configPane.editConfigurationInput.value = 'renamed';
        configPane.getSelectedConfiguration = sinon.stub().returns('old');
        configPane.loadConfigurationNames = sinon.spy();
        ctxMock.callAsync.callsFake(({ onOk }) => onOk());
        configPane.updateConfiguration();
        expect(ctxMock.setCookie.called).to.be.true;
        expect(configPane.loadConfigurationNames.calledOnce).to.be.true;
    });

    it('loadConfigurationNames renders the options, marking inherited ones as parent', function () {
        configPane.setContentAreaEnabled = sinon.spy();
        configPane.configurationChanged = sinon.spy();
        ctxMock.callAsync.callsFake(({ onOk }) => onOk(JSON.stringify([
            { name: 'A', scope: 'testScope' },
            { name: 'G', scope: '' } // inherited from a broader scope
        ])));
        configPane.loadConfigurationNames();
        const options = configPane.configSelectElement.querySelectorAll('option');
        expect(options.length).to.equal(2);
        expect(options[1].classList.contains('parent')).to.be.true;
        expect(ctxMock.disableIf.calledWith('configurations-button-edit', false)).to.be.true;
    });

    it('loadConfigurationNames shows the load error on backend failure', function () {
        ctxMock.callAsync.callsFake(({ onError }) => onError());
        configPane.loadConfigurationNames();
        expect(document.getElementById('configurations-load-error').style.display).to.equal('block');
    });

    it('configurationChanged loads content, shows the Default note and disables actions', function () {
        addEl('default-note'); addEl('global-note');
        const btn = addEl('.toolbar-button');
        document.getElementById('configurations-pane').appendChild(btn); // action button under the pane
        ctxMock.scope = '';
        configPane.loadConfigurationContent = sinon.spy();
        const checkbox = document.createElement('option');
        checkbox.value = 'Default';
        configPane.configurationChanged(checkbox);
        expect(configPane.loadConfigurationContent.calledWith('Default')).to.be.true;
        expect(document.getElementById('default-note').style.display).to.equal('block');
    });

    it('configurationChanged with no selection clears the html inputs', function () {
        const htmlInput = addEl('.html-input', 'input');
        htmlInput.value = 'stale';
        configPane.configurationChanged(null);
        expect(htmlInput.value).to.equal('');
    });

    it('loadConfigurationContent applies the content and fills revisions on success', function () {
        configPane.setConfigurationContentCallback = sinon.spy();
        configPane.setContentAreaEnabled = sinon.spy();
        ctxMock.callAsync.callsFake(({ onOk }) => onOk('{"a":1}'));
        configPane.loadConfigurationContent('cfg');
        expect(configPane.setConfigurationContentCallback.calledWith('{"a":1}')).to.be.true;
        expect(ctxMock.readAndFillRevisions.calledOnce).to.be.true;
    });

    it('loadConfigurationContent re-enables the content area on failure', function () {
        configPane.setContentAreaEnabled = sinon.spy();
        ctxMock.callAsync.callsFake(({ onError }) => onError());
        configPane.loadConfigurationContent('cfg');
        expect(configPane.setContentAreaEnabled.calledWith(true)).to.be.true;
    });

    it('setContentAreaEnabled(false) dims inputs, disables controls and hides content below the config panel', function () {
        const container = addEl('.input-container');
        addEl('.common-configuration-panel');
        const below = addEl('.some-content');
        addEl('.hide-on-edit-configuration');
        // controls the method toggles
        const htmlInput = addEl('.html-input');
        const textarea = document.createElement('textarea'); htmlInput.appendChild(textarea);
        const contentArea = addEl('.content-area');
        const select = document.createElement('select'); contentArea.appendChild(select);
        const saveArea = addEl('.save-area');
        const saveBtn = document.createElement('button'); saveBtn.className = 'toolbar-button'; saveArea.appendChild(saveBtn);
        configPane.setContentAreaEnabledCallback = sinon.spy();

        configPane.setContentAreaEnabled(false);
        expect(container.style.opacity).to.equal('30%');
        expect(textarea.disabled).to.be.true;
        expect(select.disabled).to.be.true;
        expect(saveBtn.disabled).to.be.true;
        expect(below.classList.contains('hidden-on-edit-configuration')).to.be.true;
        expect(configPane.setContentAreaEnabledCallback.calledWith(false)).to.be.true;
        // re-enabling clears the hidden marker and re-enables the controls
        configPane.setContentAreaEnabled(true);
        expect(below.classList.contains('hidden-on-edit-configuration')).to.be.false;
        expect(textarea.disabled).to.be.false;
    });

    it('hideConfigurationErrors and hideConfigurationNotes hide their elements', function () {
        const err = addEl('.configuration-error'); err.style.display = 'block';
        const note = addEl('.note'); note.style.display = 'block';
        configPane.hideConfigurationErrors();
        configPane.hideConfigurationNotes();
        expect(err.style.display).to.equal('none');
        expect(note.style.display).to.equal('none');
    });

    it('deleteConfiguration shows the delete error when the backend fails', function () {
        sinon.stub(global, 'confirm').returns(true);
        configPane.getSelectedConfiguration = sinon.stub().returns('cfg');
        ctxMock.callAsync.callsFake(({ onError }) => onError());
        configPane.deleteConfiguration();
        expect(document.getElementById('configuration-delete-error').style.display).to.equal('block');
    });

    it('deleteConfiguration shows the delete error when the pre-delete callback rejects', function () {
        sinon.stub(global, 'confirm').returns(true);
        configPane.getSelectedConfiguration = sinon.stub().returns('cfg');
        configPane.preDeleteCallback = () => Promise.reject(new Error('nope'));
        return Promise.resolve().then(() => {
            configPane.deleteConfiguration();
            return new Promise(r => setTimeout(r, 0)); // let the rejected promise settle
        }).then(() => {
            expect(document.getElementById('configuration-delete-error').style.display).to.equal('block');
            expect(ctxMock.callAsync.called).to.be.false;
        });
    });

    it('updateConfiguration shows validation errors and does not call the backend', function () {
        addEl('invalid-value-error');
        addEl('configuration-clashes-error');
        configPane.getSelectedConfiguration = sinon.stub().returns('old');
        configPane.editConfigurationInput.value = 'bad/name';
        configPane.updateConfiguration();
        expect(document.getElementById('invalid-value-error').style.display).to.equal('block');
        configPane.configSelectElement.innerHTML = '<option value="dup"></option>';
        configPane.editConfigurationInput.value = 'dup';
        configPane.updateConfiguration();
        expect(document.getElementById('configuration-clashes-error').style.display).to.equal('block');
        expect(ctxMock.callAsync.called).to.be.false;
    });

    it('loadConfigurationNames preselects the cookie value when it matches an option', function () {
        configPane.setContentAreaEnabled = sinon.spy();
        configPane.configurationChanged = sinon.spy();
        ctxMock.getCookie.returns('B');
        ctxMock.callAsync.callsFake(({ onOk }) => onOk(JSON.stringify([
            { name: 'A', scope: 'testScope' }, { name: 'B', scope: 'testScope' }
        ])));
        configPane.loadConfigurationNames();
        expect(configPane.configSelectElement.value).to.equal('B');
    });

    it('loadConfigurationNames clears the selection and disables actions for an empty list', function () {
        configPane.setContentAreaEnabled = sinon.spy();
        configPane.configurationChanged = sinon.spy();
        ctxMock.callAsync.callsFake(({ onOk }) => onOk('[]'));
        configPane.loadConfigurationNames();
        expect(configPane.configSelectElement.value).to.equal('');
        expect(configPane.configurationChanged.calledWith(null)).to.be.true;
        expect(ctxMock.disableIf.calledWith('configurations-button-edit', true)).to.be.true;
    });

    it('newConfiguration shows the new inputs and hides the edit inputs', function () {
        const editInput = document.createElement('input'); editInput.className = 'edit-configuration';
        const newInput = document.createElement('input'); newInput.className = 'new-configuration';
        document.getElementById('edit-configuration-pane').append(editInput, newInput);
        configPane.newConfiguration();
        expect(editInput.style.display).to.equal('none');
        expect(newInput.style.display).to.equal('inline-block');
    });

    it('editConfiguration shows the edit inputs and hides the new inputs', function () {
        const editInput = document.createElement('input'); editInput.className = 'edit-configuration';
        const newInput = document.createElement('input'); newInput.className = 'new-configuration';
        document.getElementById('edit-configuration-pane').append(editInput, newInput);
        configPane.getSelectedConfiguration = sinon.stub().returns('cfg');
        configPane.editConfiguration();
        expect(editInput.style.display).to.equal('inline-block');
        expect(newInput.style.display).to.equal('none');
    });

    it('configurationChanged shows the global note for an inherited config on a project scope', function () {
        addEl('default-note'); addEl('global-note');
        ctxMock.scope = 'projectX';
        configPane.loadConfigurationContent = sinon.spy();
        const checkbox = document.createElement('option');
        checkbox.value = 'Inherited';
        checkbox.classList.add('parent');
        configPane.configurationChanged(checkbox);
        expect(document.getElementById('global-note').style.display).to.equal('block');
    });

    it('getSelectedConfiguration returns the select value', function () {
        configPane.configSelectElement.innerHTML = '<option value="picked" selected></option>';
        expect(configPane.getSelectedConfiguration()).to.equal('picked');
    });

    it('deleteConfiguration deletes without a pre-delete callback and reloads on success', function () {
        sinon.stub(global, 'confirm').returns(true);
        configPane.getSelectedConfiguration = sinon.stub().returns('cfg');
        configPane.preDeleteCallback = null; // no pre-delete hook → Promise.resolve()
        configPane.loadConfigurationNames = sinon.spy();
        ctxMock.callAsync.callsFake(({ onOk }) => onOk());
        configPane.deleteConfiguration();
        return new Promise(r => setTimeout(r, 0)).then(() => {
            expect(ctxMock.callAsync.calledOnce).to.be.true;
            expect(configPane.loadConfigurationNames.calledOnce).to.be.true;
        });
    });
});
