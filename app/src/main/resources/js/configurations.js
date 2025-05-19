const SELECTED_CONFIGURATION_COOKIE = 'selected-configuration-';

const Configurations = {
    configurationsPane: document.getElementById("configurations-pane"),
    editConfigurationPane: document.getElementById("edit-configuration-pane"),
    newConfigurationInput: document.getElementById("new-configuration-input"),
    editConfigurationInput: document.getElementById("edit-configuration-input"),

    init: function ({
                        label,
                        setConfigurationContentCallback = () => {},
                        setContentAreaEnabledCallback = null,
                        preDeleteCallback = null, // It should return Promise
                        newConfigurationCallback = () => {}
                    }) {
        if (label) {
            document.querySelectorAll('span.configuration-label').forEach(labelSpan => {
                labelSpan.innerText = label;
            });
            if (label.length > 0) {
                document.querySelectorAll('span.configuration-label-capitalized').forEach(labelSpan => {
                    labelSpan.innerText = label.charAt(0).toUpperCase() + label.slice(1);
                });
            }
        }

        this.setConfigurationContentCallback = setConfigurationContentCallback;
        this.setContentAreaEnabledCallback = setContentAreaEnabledCallback;
        this.preDeleteCallback = preDeleteCallback;
        this.newConfigurationCallback = newConfigurationCallback;

        this.configurationsSelect = new SbbCustomSelect({
            selectContainer: document.getElementById("configurations-select"),
            label: document.getElementById("configurations-label"),
            changeListener: (selectedCheckbox) => this.configurationChanged(selectedCheckbox)
        });
    },

    loadConfigurationNames: function() {
        this.setContentAreaEnabled(false);
        this.hideConfigurationErrors();
        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                this.configurationsSelect.empty();

                const previouslySelectedValue = SbbCommon.getCookie(SELECTED_CONFIGURATION_COOKIE + SbbCommon.setting);
                let preselectDefault = true;
                let defaultValue = null;
                let names = JSON.parse(responseText);
                for (let name of names) {
                    defaultValue = defaultValue || name.name; // Take first element from list as default
                    if (name.name === previouslySelectedValue) {
                        preselectDefault = false;
                    }
                    const addedOption = this.configurationsSelect.addOption(name.name);
                    if (name.scope !== SbbCommon.scope) {
                        addedOption.checkbox.classList.add('parent');
                        addedOption.label.classList.add('parent');
                    }
                }

                const hasNames = names.length > 0
                document.getElementById('configurations-button-edit').disabled = !hasNames;
                document.getElementById('configurations-button-delete').disabled = !hasNames;
                if (hasNames) {
                    this.configurationsSelect.selectValue(previouslySelectedValue && !preselectDefault ? previouslySelectedValue : defaultValue);
                    this.setContentAreaEnabled(true);
                }
            },
            onError: () => document.getElementById("configurations-load-error").style.display = "block"
        });
    },

    newConfiguration: function() {
        this.configurationsPane.style.display = "none";
        document.querySelectorAll('#edit-configuration-pane .edit-configuration').forEach(htmlInput => {
            htmlInput.style.display = "none";
        });
        document.querySelectorAll('#edit-configuration-pane .new-configuration').forEach(htmlInput => {
            htmlInput.style.display = "inline-block";
        });
        this.editConfigurationPane.style.display = "block";
        this.setContentAreaEnabled(false);
        this.newConfigurationCallback();
    },

    editConfiguration: function() {
        this.configurationsPane.style.display = "none";
        document.querySelectorAll('#edit-configuration-pane .edit-configuration').forEach(htmlInput => {
            htmlInput.style.display = "inline-block";
        });
        document.querySelectorAll('#edit-configuration-pane .new-configuration').forEach(htmlInput => {
            htmlInput.style.display = "none";
        });
        this.editConfigurationInput.value = this.getSelectedConfiguration();
        this.editConfigurationPane.style.display = "block";
        this.setContentAreaEnabled(false);
    },

    cancelEditConfiguration: function() {
        this.editConfigurationPane.style.display = "none";
        this.configurationsPane.style.display = "block";
        this.setContentAreaEnabled(true);
        this.configurationsSelect.handleChange();
    },

    saveConfiguration: function() {
        this.hideConfigurationErrors();
        if (this.containsInvalidCharacters(this.newConfigurationInput.value)) {
            document.getElementById("invalid-value-error").style.display = "block";
            return;
        }
        if (this.nameClashes(this.newConfigurationInput.value, null)) {
            document.getElementById("configuration-clashes-error").style.display = "block";
            return;
        }

        SbbCommon.callAsync({
            method: 'PUT',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${this.newConfigurationInput.value}/content?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            onOk: () => {
                SbbCommon.setCookie(SELECTED_CONFIGURATION_COOKIE + SbbCommon.setting, this.newConfigurationInput.value);
                this.newConfigurationInput.value = "";
                this.editConfigurationPane.style.display = "none";
                this.configurationsPane.style.display = "block";
                Configurations.loadConfigurationNames();
            },
            onError: () => document.getElementById("configuration-save-error").style.display = "block"
        });
    },

    updateConfiguration: function() {
        this.hideConfigurationErrors();
        if (this.containsInvalidCharacters(this.editConfigurationInput.value)) {
            document.getElementById("invalid-value-error").style.display = "block";
            return;
        }
        if (this.nameClashes(this.editConfigurationInput.value, this.getSelectedConfiguration())) {
            document.getElementById("configuration-clashes-error").style.display = "block";
            return;
        }

        SbbCommon.callAsync({
            method: 'POST',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${this.getSelectedConfiguration()}?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            body: this.editConfigurationInput.value,
            onOk: () => {
                SbbCommon.setCookie(SELECTED_CONFIGURATION_COOKIE + SbbCommon.setting, this.editConfigurationInput.value);
                this.editConfigurationInput.value = "";
                this.editConfigurationPane.style.display = "none";
                this.configurationsPane.style.display = "block";
                Configurations.loadConfigurationNames();
            },
            onError: () => document.getElementById("configuration-save-error").style.display = "block"
        });
    },

    nameClashes: function (newValue, oldValue) {
        for (let checkbox of this.configurationsSelect.getAllCheckboxes()) {
            if (!checkbox.classList.contains('parent')) { // Projects scope configurations can override ones from global scope
                if (checkbox.value !== oldValue) { // Ignore persisted name of the option to be renamed itself
                    if (checkbox.value === newValue) {
                        return true;
                    }
                }
            }
        }
        return false;
    },

    containsInvalidCharacters: function (value) {
        const regex = new RegExp(/[^a-zA-Z0-9\-_ ]+/);
        return regex.test(value);
    },

    deleteConfiguration: function() {
        if (confirm("Are you sure you want to delete this configuration?")) {
            this.setContentAreaEnabled(false);
            this.hideConfigurationErrors();
            const configurationToDelete = this.getSelectedConfiguration();
            const preDeleteCallback = this.preDeleteCallback ? this.preDeleteCallback(configurationToDelete) : Promise.resolve();
            preDeleteCallback.then(() => {
                SbbCommon.callAsync({
                    method: 'DELETE',
                    url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${configurationToDelete}?scope=${SbbCommon.scope}`,
                    contentType: 'application/json',
                    onOk: () => this.loadConfigurationNames(),
                    onError: () => document.getElementById("configuration-delete-error").style.display = "block"
                })
            }).catch(() => {
                document.getElementById("configuration-delete-error").style.display = "block";
            })
        }
    },

    configurationChanged: function(selectedCheckbox) {
        this.hideConfigurationErrors();
        if (selectedCheckbox) {
            const configurationName = selectedCheckbox.value;

            this.loadConfigurationContent(configurationName);
            SbbCommon.setCookie(SELECTED_CONFIGURATION_COOKIE + SbbCommon.setting, configurationName);

            this.hideConfigurationNotes();
            let noteShown = false;
            if (SbbCommon.scope === "" && configurationName === "Default") {
                document.getElementById('default-note').style.display = "block";
                noteShown = true;
            } else if (SbbCommon.scope !== "" && selectedCheckbox.classList.contains('parent')) {
                document.getElementById('global-note').style.display = "block";
                noteShown = true;
            }

            document.querySelectorAll('#configurations-pane .toolbar-button:not(.new-configuration)').forEach(actionButton => {
                actionButton.disabled = noteShown;
            });
        } else {
            document.querySelectorAll('.html-input').forEach(htmlInput => {
                htmlInput.value = "";
            });
        }
    },

    getSelectedConfiguration: function () {
        return this.configurationsSelect.getSelectedValue();
    },

    loadConfigurationContent: function(configurationName) {
        this.setContentAreaEnabled(false);

        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${configurationName}/content?scope=${SbbCommon.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                this.setConfigurationContentCallback(responseText);
                this.setContentAreaEnabled(true);
                SbbCommon.readAndFillRevisions({
                    configurationName: configurationName,
                    revertToRevisionCallback: (responseText) => this.setConfigurationContentCallback(responseText)
                });
            },
            onError: () => this.setContentAreaEnabled(true)
        });
    },

    setContentAreaEnabled: function(enabled) {
        document.querySelectorAll('.input-container').forEach(inputContainer => {
            inputContainer.style.opacity = enabled ? "100%" : "30%";
        });
        if (!enabled) {
            document.querySelectorAll('.html-input').forEach(htmlInput => {
                htmlInput.value = "";
            });
        }
        document.querySelectorAll('.html-input textarea').forEach(textarea => {
            textarea.disabled = !enabled;
        });
        document.querySelectorAll('.content-area select').forEach(select => {
            if (!enabled) {
                select.value = null;
            }
            select.disabled = !enabled;
        });
        document.querySelectorAll('.save-area .toolbar-button').forEach(inputContainer => {
            inputContainer.disabled = !enabled;
        });
        if (!enabled) {
            // hide all children of 'standard-admin-page' below configuration div
            let foundConfigurationElement = false;
            Array.from(document.querySelector('.standard-admin-page').children).forEach(item => {
                if (item.classList.contains('common-configuration-panel')) {
                    foundConfigurationElement = true;
                } else if (foundConfigurationElement && !item.classList.contains('skip-hide-on-edit-configuration')) {
                    item.classList.add("hidden-on-edit-configuration");
                }
            });
            // also hide all components, explicitly marked with 'hide-on-edit-configuration'
            document.querySelectorAll('.hide-on-edit-configuration')
                .forEach(item => item.classList.add("hidden-on-edit-configuration"));
        } else {
            document.querySelectorAll('.hidden-on-edit-configuration')
                .forEach(item => item.classList.remove("hidden-on-edit-configuration"));
        }
        if (this.setContentAreaEnabledCallback) {
            this.setContentAreaEnabledCallback(enabled);
        }
    },

    hideConfigurationErrors: function () {
        document.querySelectorAll('.configuration-error').forEach(htmlInput => {
            htmlInput.style.display = "none";
        });
    },

    hideConfigurationNotes: function () {
        document.querySelectorAll('.note').forEach(htmlInput => {
            htmlInput.style.display = "none";
        });
    }
}
