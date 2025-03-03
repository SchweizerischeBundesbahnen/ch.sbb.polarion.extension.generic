export default class ExtensionContext {

    static DEFAULT = "Default";

    constructor ({extension = "", setting = "", rootComponentSelector = "", scope = "", scopeFieldId, initCodeInput = false, propertiesHighlighting = false}) {
        this.extension = extension;
        this.setting = setting;
        this.rootComponentSelector = rootComponentSelector;
        this.scope = scopeFieldId ? this.getValueById(scopeFieldId) : scope;
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
    }

    getElementById(elementId) {
        return document.querySelector(`${this.rootComponentSelector} #${elementId}`);
    }

    getJQueryElement(selector) {
        return $(`${this.rootComponentSelector} ${selector}`);
    }

    querySelector(selector) {
        return document.querySelector(`${this.rootComponentSelector} ${selector}`);
    }

    querySelectorAll(selector) {
        return document.querySelectorAll(`${this.rootComponentSelector} ${selector}`);
    }

    getValueById(elementId) {
        return this.getElementById(elementId).value;
    }

    setValueById(elementId, value) {
        this.getElementById(elementId).value = value
    }

    getValue(elementId) {
        return this.getValueById(elementId);
    }

    setValue(elementId, value) {
        this.setValueById(elementId, value);
    }

    getCheckboxValueById(elementId) {
        return this.getElementById(elementId).checked;
    }

    setCheckboxValueById(elementId, checked) {
        this.getElementById(elementId).checked = !!checked;
    }

    getCheckbox(elementId) {
        return this.getCheckboxValueById(elementId);
    }

    setCheckbox(elementId, checked) {
        this.setCheckboxValueById(elementId, checked);
    }

    setSelector(elementId, value) {
        const selector = this.getElementById(elementId);
        selector.value = this.containsOption(selector, value) ? value : ExtensionContext.DEFAULT;
    }

    selectOptionByValue(id, optionValue) {
        this.getElementById(id).value = optionValue;
    }

    displayIf(elementId, condition, displayStyle = "block") {
        this.getElementById(elementId).style.display = condition ? displayStyle : "none";
    }

    visibleIf(elementId, condition) {
        this.getElementById(elementId).style.visibility = condition ? "visible" : "hidden";
    }

    containsOption(selectElement, option) {
        return [...selectElement.options].map(o => o.value).includes(option);
    }

    isWindowDefined() {
        return typeof window !== 'undefined';
    }

    /**
     * Assign click listener. May accept several pairs of elementId + eventListener like:
     * onClick(
     *     "button1", listener1,
     *     "button2", listener2,
     *     "button3", listener3
     *     );
     */
    onClick(...pairedArgs) {
        this.addListeners("click", ...pairedArgs);
    }

    /**
     * Assign change listener. May accept several pairs of elementId + eventListener like:
     * onClick(
     *     "select1", listener1,
     *     "select2", listener2,
     *     "select3", listener3
     *     );
     */
    onChange(...pairedArgs) {
        this.addListeners("change", ...pairedArgs);
    }

    /**
     * Assign blur event listener. May accept several pairs of elementId + eventListener like:
     * onClick(
     *     "input1", listener1,
     *     "input2", listener2,
     *     "input3", listener3
     *     );
     */
    onBlur(...pairedArgs) {
        this.addListeners("blur", ...pairedArgs);
    }

    /**
     * @param action 'click', 'change' etc.
     * @param pairedArgs pairs of elementId + eventListener
     */
    addListeners(action, ...pairedArgs) {
        for (let i = 0; i < pairedArgs.length; i += 2) {
            const elementId = pairedArgs[i];
            const listener = pairedArgs[i + 1];

            if (typeof elementId === "string" && typeof listener === "function") {
                let element = this.getElementById(elementId);
                if (element) {
                    element.removeAttribute(`on${action}`); // remove inline handler
                    element.addEventListener(action, listener);
                } else {
                    console.log(`WARNING: cannot add listener - there is no element with id '${elementId}'`);
                }
            } else {
                console.error("Invalid argument pair at index", i, ":", elementId, listener);
            }
        }
    }

    showActionAlert({containerId, message, hideAlertByTimeout = true}) {
        this.hideActionAlerts();

        const container = document.getElementById(containerId);
        if (container && message) {
            container.innerHTML = message;
            container.style.display = 'inline-block';
        }

        if (hideAlertByTimeout) {
            setTimeout(() => {
                this.hideActionAlerts();
            }, 5000);
        }
    }

    showRevertedToRevisionAlert(revision) {
        this.showActionAlert({
            containerId: 'action-success',
            message: `Data reverted to revision <span class="revision-number">${revision.name}</span>. Don't forget to save the data before leaving.`
        });
    }

    showRevertedToDefaultAlert() {
        this.showActionAlert({
            containerId: 'action-success',
            message: `Default value set. Don't forget to save the data before leaving.`
        });
    }

    showSaveSuccessAlert() {
        this.showActionAlert({
            containerId: 'action-success',
            message: `Data successfully saved.`
        });
    }

    showSaveErrorAlert() {
        this.showActionAlert({
            containerId: 'action-error',
            message: `Error occurred during saving the data.`
        });
    }

    hideActionAlerts() {
        this.querySelectorAll('.action-alerts .alert').forEach(alertDiv => {
            alertDiv.style.display = 'none';
        });
    }

    setNewerVersionNotificationVisible(visible) {
        this.getElementById('newer-version-warning').style.display = (visible ? 'block' : 'none')
    }

    setLoadingErrorNotificationVisible(visible) {
        this.getElementById('data-loading-error').style.display = (visible ? 'block' : 'none')
    }

    setRevisionsLoadingErrorNotificationVisible(visible) {
        this.getElementById('revisions-loading-error').style.display = (visible ? 'block' : 'none')
    }

    cancelEdit() {
        if (confirm("Are you sure you want to cancel editing and revert all changes made?")) {
            window.location.reload();
        }
    }

    callAsync({method, url, contentType, responseType, body, onOk, onError}) {
        // restrict directory traversal attempts
        if (url.includes('..')) {
            const errorMessage = 'directory traversal restricted';
            onError === undefined ? alert(errorMessage) : onError(undefined, errorMessage);
            return;
        }
        const xhr = new XMLHttpRequest();
        xhr.open(method, url, true);
        if (contentType) {
            xhr.setRequestHeader('Content-Type', contentType);
        }
        if (responseType) {
            xhr.responseType = responseType;
        }
        xhr.send(body);

        xhr.onreadystatechange = () => {
            if (xhr.readyState === 4) {
                if ([200, 202, 204].includes(xhr.status)) {
                    onOk(this.getStringIfTextResponse(xhr), xhr);
                } else {
                    const responseText = this.getStringIfTextResponse(xhr);
                    const error = responseText && JSON.parse(responseText);
                    const errorMessage = error && (error.message ? error.message : responseText);
                    if (onError === undefined) {
                        try {
                            this.showActionAlert({containerId: 'action-error', message: errorMessage});
                        } catch {
                            alert('Error occurred (' + xhr.responseText + ').');
                        }
                    } else {
                        onError(xhr.status, errorMessage, xhr)
                    }
                }
            }
        };
        xhr.onerror = function () {
            onError === undefined ? alert('Error occurred') : onError(xhr.status, undefined, xhr);
        };
    }

    // if the responseType isn't empty or 'text' then getting xhr.responseType will fail
    // with "Uncaught InvalidStateError: Failed to read the 'responseText' property from 'XMLHttpRequest'"
    getStringIfTextResponse(xhr) {
        return xhr.responseType === '' || xhr.responseType === 'text' ? xhr.responseText : undefined;
    }

    // initiate file download
    downloadBlob(blob, fileName) {
        const objectURL = (window.URL ? window.URL : window.webkitURL).createObjectURL(blob);
        const link = document.createElement("a");
        link.href = objectURL;
        link.download = fileName
        link.target = "_blank";
        link.click();
        link.remove();
        setTimeout(() => URL.revokeObjectURL(objectURL), 100);
    }

    // we have to allow overriding settingName coz some extensions may have several settings on the single page
    readAndFillRevisions({setting, configurationName = ExtensionContext.DEFAULT, revertToRevisionCallback}) {
        const currentSetting = (setting ? setting : this.setting);
        this.setRevisionsLoadingErrorNotificationVisible(false);
        this.callAsync({
            method: 'GET',
            url: `/polarion/${this.extension}/rest/internal/settings/${currentSetting}/names/${configurationName}/revisions?scope=${this.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => {
                const revisions = JSON.parse(responseText);
                const tableBody = this.getElementById('revisions-table').getElementsByTagName('tbody')[0];
                tableBody.innerHTML = '';
                for (const revision of revisions) {
                    const button = this.createRevertToRevisionButton();
                    button.addEventListener("click", () => {
                        this.hideActionAlerts();
                        this.callAsync({
                            method: 'GET',
                            url: `/polarion/${this.extension}/rest/internal/settings/${currentSetting}/names/${configurationName}/content?scope=${this.scope}&revision=${revision.name}`,
                            contentType: 'application/json',
                            onOk: (responseText) => {
                                if (revertToRevisionCallback !== undefined) {
                                    revertToRevisionCallback(responseText);
                                }
                                this.showRevertedToRevisionAlert(revision);
                            }
                        });
                    });

                    this.createRevisionRow(tableBody, revision, button);
                }
                if (revisions.length === 0) {
                    let cell = tableBody.insertRow().insertCell();
                    cell.setAttribute("colspan", "6")
                    cell.className = 'empty-message';
                    cell.appendChild(document.createTextNode("No saved revisions yet."));
                }
            },
            onError: () => this.setRevisionsLoadingErrorNotificationVisible(true)
        });
    }

    createRevisionRow(tableBody, revision, revertToRevisionButton) {
        const row = tableBody.insertRow();
        row.insertCell().appendChild(document.createTextNode(this.insertRevisionSpaces(revision.name)));
        row.insertCell().appendChild(document.createTextNode(revision.baseline === undefined ? '' : revision.baseline));
        row.insertCell().appendChild(document.createTextNode(revision.date));
        row.insertCell().appendChild(document.createTextNode(revision.author));
        row.insertCell().appendChild(document.createTextNode(revision.description));
        row.insertCell().appendChild(revertToRevisionButton);
        return row;
    }

    createRevertToRevisionButton() {
        const button = document.createElement("button");
        button.title = 'Revert data to this revision';
        button.className = 'revert-to-revision-button';
        button.innerHTML = '<img src="/polarion/ria/images/actions/arrow_reopen.png" />';
        return button;
    }

    toggleRevisions() {
        const revisionsPanel = document.getElementById('revisions-expand-container');
        revisionsPanel.style.display = (window.getComputedStyle(revisionsPanel).display === 'none' ? 'block' : 'none');
    }

    //for better UX insert spaces after each 3rd digit (e.g. '12345' -> '12 345'), in order to do this we are:
    // 1) reversing string
    // 2) inserting spaces
    // 3) reversing the result back
    insertRevisionSpaces(revisionName) {
        return revisionName.split("").reverse().join("").match(/.{1,3}/g).join(' ').split("").reverse().join("");
    }

    setCookie(name, value, daysToExpire = 1) {
        let expires = '';
        if (daysToExpire) {
            const date = new Date();
            date.setTime(date.getTime() + (daysToExpire * 24 * 60 * 60 * 1000));
            expires = '; expires=' + date.toUTCString();
        }
        document.cookie = name + '=' + encodeURIComponent(value) + expires + '; path=/';
    }

    getCookie(name) {
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
    }

    deleteCookie(name) {
        this.setCookie(name, '', -1);
    }

}
