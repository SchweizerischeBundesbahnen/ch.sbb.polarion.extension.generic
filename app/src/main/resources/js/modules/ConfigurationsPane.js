import SearchableDropdown from "./SearchableDropdown.js";

export default class ConfigurationsPane {

    static SELECTED_CONFIGURATION_COOKIE = 'selected-configuration-';

    /**
     * @param {string} label
     * @param {ExtensionContext} ctx
     * @param {object} componentIds
     * @param {function} setConfigurationContentCallback
     * @param {function} setContentAreaEnabledCallback
     * @param {function} preDeleteCallback
     * @param {function} newConfigurationCallback
     */
    constructor({
                    label,
                    ctx,
                    componentIds = {
                        configurationsPaneId: "configurations-pane",
                        editConfigurationPaneId: "edit-configuration-pane",
                        newConfigurationInputId: "new-configuration-input",
                        editConfigurationInputId: "edit-configuration-input",
                    },
                    setConfigurationContentCallback = () => {
                    },
                    setContentAreaEnabledCallback = null,
                    preDeleteCallback = null, // It should return Promise
                    newConfigurationCallback = () => {
                    }
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

        this.ctx = ctx;
        this.ctx.onClick(
            'configurations-button-edit', () => this.editConfiguration(),
            'configurations-button-delete', () => this.deleteConfiguration(),
            'configurations-button-create', () => this.newConfiguration(),
            'configurations-button-cancel-edit', () => this.cancelEditConfiguration(),
            'configurations-button-save', () => this.saveConfiguration(),
            'configurations-button-update', () => this.updateConfiguration(),
        );

        this.configurationsPane = document.getElementById(componentIds.configurationsPaneId);
        this.editConfigurationPane = document.getElementById(componentIds.editConfigurationPaneId);
        this.newConfigurationInput = document.getElementById(componentIds.newConfigurationInputId);
        this.editConfigurationInput = document.getElementById(componentIds.editConfigurationInputId);

        // Disable Save / Update while the configuration name is empty or whitespace-only, so an
        // unnamed config can't be submitted (the backend rejects it with a "save error").
        if (this.newConfigurationInput) {
            this.newConfigurationInput.addEventListener('input', () => this.refreshSaveEnabled());
        }
        if (this.editConfigurationInput) {
            this.editConfigurationInput.addEventListener('input', () => this.refreshUpdateEnabled());
        }

        this.setConfigurationContentCallback = setConfigurationContentCallback;
        this.setContentAreaEnabledCallback = setContentAreaEnabledCallback;
        this.preDeleteCallback = preDeleteCallback;
        this.newConfigurationCallback = newConfigurationCallback;

        this.configSelectElement = document.getElementById("configurations-select");
        const configLabel = document.getElementById("configurations-label");
        if (configLabel) {
            configLabel.htmlFor = "configurations-select";
        }
        this.configurationsSelect = new SearchableDropdown({
            element: this.configSelectElement,
            preserveOptionClasses: true,
            rememberSelection: false
        });
        // Native <select> change (user pick or programmatic dispatch) drives the config logic
        this.configSelectElement.addEventListener('change', () =>
            this.configurationChanged(this.configSelectElement.selectedOptions[0] || null));
    }

    loadConfigurationNames() {
        this.setContentAreaEnabled(false);
        this.hideConfigurationErrors();
        this.ctx.callAsync({
            method: 'GET',
            url: `/polarion/${this.ctx.extension}/rest/internal/settings/${this.ctx.setting}/names?scope=${this.ctx.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                this.configSelectElement.innerHTML = "";

                const previouslySelectedValue = this.ctx.getCookie(ConfigurationsPane.SELECTED_CONFIGURATION_COOKIE + this.ctx.setting);
                let preselectDefault = true;
                let defaultValue = null;
                let names = JSON.parse(responseText);
                for (let name of names) {
                    defaultValue = defaultValue || name.name; // Take first element from list as default
                    if (name.name === previouslySelectedValue) {
                        preselectDefault = false;
                    }
                    const option = document.createElement('option');
                    option.value = name.name;
                    option.textContent = name.name;
                    if (name.scope !== this.ctx.scope) { // inherited from a broader (e.g. global) scope
                        option.classList.add('parent');
                    }
                    this.configSelectElement.appendChild(option);
                }

                const hasNames = names.length > 0
                this.ctx.disableIf('configurations-button-edit', !hasNames);
                this.ctx.disableIf('configurations-button-delete', !hasNames);
                if (hasNames) {
                    this.configSelectElement.value = previouslySelectedValue && !preselectDefault ? previouslySelectedValue : defaultValue;
                    this.configSelectElement.dispatchEvent(new Event('change'));
                    this.setContentAreaEnabled(true);
                } else {
                    this.configSelectElement.value = "";
                    this.configurationChanged(null);
                }
            },
            onError: () => document.getElementById("configurations-load-error").style.display = "block"
        });
    }

    newConfiguration() {
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
        this.refreshSaveEnabled();
    }

    editConfiguration() {
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
        this.refreshUpdateEnabled();
    }

    cancelEditConfiguration() {
        this.editConfigurationPane.style.display = "none";
        this.configurationsPane.style.display = "block";
        this.setContentAreaEnabled(true);
        this.configSelectElement.dispatchEvent(new Event('change'));
    }

    refreshSaveEnabled() {
        this.ctx.disableIf('configurations-button-save', !this.newConfigurationInput || !this.newConfigurationInput.value.trim());
    }

    refreshUpdateEnabled() {
        this.ctx.disableIf('configurations-button-update', !this.editConfigurationInput || !this.editConfigurationInput.value.trim());
    }

    saveConfiguration() {
        this.hideConfigurationErrors();
        if (this.containsInvalidCharacters(this.newConfigurationInput.value)) {
            document.getElementById("invalid-value-error").style.display = "block";
            return;
        }
        if (this.nameClashes(this.newConfigurationInput.value, null)) {
            document.getElementById("configuration-clashes-error").style.display = "block";
            return;
        }

        this.ctx.callAsync({
            method: 'PUT',
            url: `/polarion/${this.ctx.extension}/rest/internal/settings/${this.ctx.setting}/names/${this.newConfigurationInput.value}/content?scope=${this.ctx.scope}`,
            contentType: 'application/json',
            onOk: () => {
                this.ctx.setCookie(ConfigurationsPane.SELECTED_CONFIGURATION_COOKIE + this.ctx.setting, this.newConfigurationInput.value);
                this.newConfigurationInput.value = "";
                this.editConfigurationPane.style.display = "none";
                this.configurationsPane.style.display = "block";
                this.loadConfigurationNames();
            },
            onError: () => document.getElementById("configuration-save-error").style.display = "block"
        });
    }

    updateConfiguration() {
        this.hideConfigurationErrors();
        if (this.containsInvalidCharacters(this.editConfigurationInput.value)) {
            document.getElementById("invalid-value-error").style.display = "block";
            return;
        }
        if (this.nameClashes(this.editConfigurationInput.value, this.getSelectedConfiguration())) {
            document.getElementById("configuration-clashes-error").style.display = "block";
            return;
        }

        this.ctx.callAsync({
            method: 'POST',
            url: `/polarion/${this.ctx.extension}/rest/internal/settings/${this.ctx.setting}/names/${this.getSelectedConfiguration()}?scope=${this.ctx.scope}`,
            contentType: 'application/json',
            body: this.editConfigurationInput.value,
            onOk: () => {
                this.ctx.setCookie(ConfigurationsPane.SELECTED_CONFIGURATION_COOKIE + this.ctx.setting, this.editConfigurationInput.value);
                this.editConfigurationInput.value = "";
                this.editConfigurationPane.style.display = "none";
                this.configurationsPane.style.display = "block";
                this.loadConfigurationNames();
            },
            onError: () => document.getElementById("configuration-save-error").style.display = "block"
        });
    }

    nameClashes(newValue, oldValue) {
        for (let option of Array.from(this.configSelectElement.options)) {
            if (!option.classList.contains('parent')) { // Projects scope configurations can override ones from global scope
                if (option.value !== oldValue) { // Ignore persisted name of the option to be renamed itself
                    if (option.value === newValue) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    containsInvalidCharacters(value) {
        const regex = new RegExp(/[^a-zA-Z0-9\-_ ]+/);
        return regex.test(value);
    }

    deleteConfiguration() {
        if (confirm("Are you sure you want to delete this configuration?")) {
            this.setContentAreaEnabled(false);
            this.hideConfigurationErrors();
            const configurationToDelete = this.getSelectedConfiguration();
            const preDeleteCallback = this.preDeleteCallback ? this.preDeleteCallback(configurationToDelete) : Promise.resolve();
            preDeleteCallback.then(() => {
                this.ctx.callAsync({
                    method: 'DELETE',
                    url: `/polarion/${this.ctx.extension}/rest/internal/settings/${this.ctx.setting}/names/${configurationToDelete}?scope=${this.ctx.scope}`,
                    contentType: 'application/json',
                    onOk: () => this.loadConfigurationNames(),
                    onError: () => document.getElementById("configuration-delete-error").style.display = "block"
                })
            }).catch(() => {
                document.getElementById("configuration-delete-error").style.display = "block";
            })
        }
    }

    configurationChanged(selectedCheckbox) {
        this.hideConfigurationErrors();
        if (selectedCheckbox) {
            const configurationName = selectedCheckbox.value;

            this.loadConfigurationContent(configurationName);
            this.ctx.setCookie(ConfigurationsPane.SELECTED_CONFIGURATION_COOKIE + this.ctx.setting, configurationName);

            this.hideConfigurationNotes();
            let noteShown = false;
            if (this.ctx.scope === "" && configurationName === "Default") {
                document.getElementById('default-note').style.display = "block";
                noteShown = true;
            } else if (this.ctx.scope !== "" && selectedCheckbox.classList.contains('parent')) {
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
    }

    getSelectedConfiguration() {
        return this.configSelectElement.value;
    }

    loadConfigurationContent(configurationName) {
        this.setContentAreaEnabled(false);

        this.ctx.callAsync({
            method: 'GET',
            url: `/polarion/${this.ctx.extension}/rest/internal/settings/${this.ctx.setting}/names/${configurationName}/content?scope=${this.ctx.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                this.setConfigurationContentCallback(responseText);
                this.setContentAreaEnabled(true);
                this.ctx.readAndFillRevisions({
                    configurationName: configurationName,
                    revertToRevisionCallback: (responseText) => this.setConfigurationContentCallback(responseText)
                });
            },
            onError: () => this.setContentAreaEnabled(true)
        });
    }

    setContentAreaEnabled(enabled) {
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
    }

    hideConfigurationErrors() {
        document.querySelectorAll('.configuration-error').forEach(htmlInput => {
            htmlInput.style.display = "none";
        });
    }

    hideConfigurationNotes() {
        document.querySelectorAll('.note').forEach(htmlInput => {
            htmlInput.style.display = "none";
        });
    }
}
