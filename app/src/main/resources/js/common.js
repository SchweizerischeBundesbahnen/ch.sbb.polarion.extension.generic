const DEFAULT_CONFIGURATION = 'Default';

const SbbCommon = {

    extension: "",
    setting: "",
    scope: "",

    init: function ({extension, setting = "", scope = "", initCodeInput = false, propertiesHighlighting = false}) {
        this.extension = extension;
        this.setting = setting;
        this.scope = scope;
        if (initCodeInput) {
            codeInput.registerTemplate("code-input", codeInput.templates.prism(Prism), []);
            if (propertiesHighlighting) {
                Prism.languages.properties = {
                    comment: /^[ \t]*[#!].*$/m,
                    //ORIGINAL value: {pattern: /(^[ \t]*(?:\\(?:\r\n|[\s\S])|[^\\\s:=])+(?: *[=:] *(?! )| ))(?:\\(?:\r\n|[\s\S])|[^\\\r\n])+/m, lookbehind: !0, alias: "attr-value"},
                    value: {pattern: /(^[ \t]*(?:\\[\s\S]|[^\\=])+(?: *= *(?! )| ))(?:\\(?:\r\n|[\s\S])|[^\\\r\n])+/m, lookbehind: !0, alias: "attr-value"},
                    //ORIGINAL key: {pattern: /^[ \t]*(?:\\(?:\r\n|[\s\S])|[^\\\s:=])+(?= *[=:]| )/m, alias: "attr-name"},
                    key: {pattern: /^[ \t]*(?:\\[\s\S]|[^\\=])+(?= *[=:]| )/m, alias: "attr-name"},
                    punctuation: /[=:]/
                };
            }
        }
    },

    getValueById: function (id) {
        const elem = document.getElementById(id);
        return elem && elem.value;
    },

    setValueById: function (id, value) {
        const elem = document.getElementById(id);
        if (elem) {
            elem.value = value;
        }
    },

    getCheckboxValueById: function (id) {
        const elem = document.getElementById(id);
        return elem && elem.checked;
    },

    setCheckboxValueById: function (id, checked) {
        const elem = document.getElementById(id);
        if (elem) {
            elem.checked = checked;
        }
    },

    showActionAlert: function ({containerId, message, hideAlertByTimeout = true}) {
        this.hideActionAlerts();

        const container = document.getElementById(containerId);
        if (container && message) {
            container.innerHTML = message;
            container.style.display = 'inline-block';
        }

        if (hideAlertByTimeout) {
            setTimeout(function () {
                SbbCommon.hideActionAlerts();
            }, 5000);
        }
    },

    showRevertedToRevisionAlert(revision) {
        this.showActionAlert({
            containerId: 'action-success',
            message: `Data reverted to revision <span class="revision-number">${revision.name}</span>. Don't forget to save the data before leaving.`
        });
    },

    showRevertedToDefaultAlert() {
        this.showActionAlert({
            containerId: 'action-success',
            message: `Default value set. Don't forget to save the data before leaving.`
        });
    },

    showSaveSuccessAlert() {
        this.showActionAlert({
            containerId: 'action-success',
            message: `Data successfully saved.`
        });
    },

    showSaveErrorAlert() {
        this.showActionAlert({
            containerId: 'action-error',
            message: `Error occurred during saving the data.`
        });
    },

    hideActionAlerts: function() {
        document.querySelectorAll('.action-alerts .alert').forEach(alertDiv => {
            alertDiv.style.display = 'none';
        });
    },

    setNewerVersionNotificationVisible: function (visible) {
        document.getElementById('newer-version-warning').style.display = (visible ? 'block' : 'none')
    },

    setLoadingErrorNotificationVisible: function (visible) {
        document.getElementById('data-loading-error').style.display = (visible ? 'block' : 'none')
    },

    setRevisionsLoadingErrorNotificationVisible: function (visible) {
        document.getElementById('revisions-loading-error').style.display = (visible ? 'block' : 'none')
    },

    cancelEdit: function () {
        if (confirm("Are you sure you want to cancel editing and revert all changes made?")) {
            window.location.reload();
        }
    },

    callAsync: function ({method, url, contentType, body, onOk, onError}) {
        const xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        if (contentType) {
            xhr.setRequestHeader('Content-Type', contentType);
        }
        xhr.send(body);

        xhr.onreadystatechange = function () {
            if (xhr.readyState === 4) {
                if (xhr.status === 200 || xhr.status === 204) {
                    onOk(xhr.responseText);
                } else {
                    const error = xhr.responseText && JSON.parse(xhr.responseText);
                    const errorMessage = error && (error.message ? error.message : xhr.responseText);
                    if (onError === undefined) {
                        try {
                            SbbCommon.showActionAlert({containerId: 'action-error', message: errorMessage});
                        } catch {
                            alert('Error occurred (' + xhr.responseText + ').');
                        }
                    } else {
                        onError(xhr.status, errorMessage)
                    }
                }
            }
        };
        xhr.onerror = function () {
            onError === undefined ? alert('Error occurred') : onError();
        };
    },

    // we have to allow overriding settingName coz some extensions may have several settings on the single page
    readAndFillRevisions: function ({setting, configurationName = DEFAULT_CONFIGURATION, revertToRevisionCallback}) {
        const currentSetting = (setting ? setting : this.setting);
        this.setRevisionsLoadingErrorNotificationVisible(false);
        this.callAsync({
            method: 'GET',
            url: `/polarion/${this.extension}/rest/internal/settings/${currentSetting}/names/${configurationName}/revisions?scope=${this.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                const revisions = JSON.parse(responseText);
                const tableBody = document.getElementById('revisions-table').getElementsByTagName('tbody')[0];
                tableBody.innerHTML = '';
                for (const revision of revisions) {
                    const button = SbbCommon.createRevertToRevisionButton();
                    button.addEventListener("click", function () {
                        SbbCommon.hideActionAlerts();
                        SbbCommon.callAsync({
                            method: 'GET',
                            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${currentSetting}/names/${configurationName}/content?scope=${SbbCommon.scope}&revision=${revision.name}`,
                            contentType: 'application/json',
                            onOk: (responseText) => {
                                if (revertToRevisionCallback !== undefined) {
                                    revertToRevisionCallback(responseText);
                                }
                                SbbCommon.showRevertedToRevisionAlert(revision);
                            }
                        });
                    });

                    SbbCommon.createRevisionRow(tableBody, revision, button);
                }
                if (revisions.length === 0) {
                    let cell = tableBody.insertRow().insertCell();
                    cell.setAttribute("colspan", "6")
                    cell.className = 'empty-message';
                    cell.appendChild(document.createTextNode("No saved revisions yet."));
                }
            },
            onError: () => SbbCommon.setRevisionsLoadingErrorNotificationVisible(true)
        });
    },

    createRevisionRow: function (tableBody, revision, revertToRevisionButton) {
        const row = tableBody.insertRow();
        row.insertCell().appendChild(document.createTextNode(SbbCommon.insertRevisionSpaces(revision.name)));
        row.insertCell().appendChild(document.createTextNode(revision.baseline === undefined ? '' : revision.baseline));
        row.insertCell().appendChild(document.createTextNode(revision.date));
        row.insertCell().appendChild(document.createTextNode(revision.author));
        row.insertCell().appendChild(document.createTextNode(revision.description));
        row.insertCell().appendChild(revertToRevisionButton);
        return row;
    },

    createRevertToRevisionButton: function () {
        const button = document.createElement("button");
        button.title = 'Revert data to this revision';
        button.className = 'revert-to-revision-button';
        button.innerHTML = '<img src="/polarion/ria/images/actions/arrow_reopen.png" />';
        return button;
    },

    toggleRevisions: function () {
        const revisionsPanel = document.getElementById('revisions-expand-container');
        revisionsPanel.style.display = (window.getComputedStyle(revisionsPanel).display === 'none' ? 'block' : 'none');
    },

    //for better UX insert spaces after each 3rd digit (e.g. '12345' -> '12 345'), in order to do this we are:
    // 1) reversing string
    // 2) inserting spaces
    // 3) reversing the result back
    insertRevisionSpaces: function (revisionName) {
        return revisionName.split("").reverse().join("").match(/.{1,3}/g).join(' ').split("").reverse().join("");
    },

    setCookie: function (name, value, daysToExpire = 1) {
        let expires = '';
        if (daysToExpire) {
            const date = new Date();
            date.setTime(date.getTime() + (daysToExpire * 24 * 60 * 60 * 1000));
            expires = '; expires=' + date.toUTCString();
        }
        document.cookie = name + '=' + encodeURIComponent(value) + expires + '; path=/';
    },

    getCookie: function (name) {
        const nameEQ = name + '=';
        const cookiesArray = document.cookie.split(';');
        for (let i = 0; i < cookiesArray.length; i++) {
            let cookie = cookiesArray[i];
            while (cookie.charAt(0) === ' ') {
                cookie = cookie.substring(1, cookie.length);
            }
            if (cookie.indexOf(nameEQ) === 0) {
                return decodeURIComponent(cookie.substring(nameEQ.length, cookie.length));
            }
        }
        return null;
    },

    deleteCookie: function (name) {
        this.setCookie(name, '', -1);
    },

    selectOptionByValue: function (id, optionValue) {
        const selectElement = document.getElementById(id);
        if (!selectElement) {
            console.error('Select element with the provided ID not found.');
            return;
        }

        selectElement.value = optionValue;
    }

};