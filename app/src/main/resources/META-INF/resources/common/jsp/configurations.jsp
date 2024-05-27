<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<p>There can be multiple named <span class="configuration-label">configuration</span>s. Please, chose one you would like to modify in dropdown below.
    Be aware that "Default" <span class="configuration-label">configuration</span> on global scope can't be deleted or renamed.</p>
<div class="input-group">
    <div id="configurations-pane">
        <label id="configurations-label"><span class="configuration-label-capitalized">Configuration</span>:</label>
        <div id="configurations-select"></div>
        <div class="action-buttons">
            <button class="toolbar-button" onclick="Configurations.editConfiguration()">
                <img class="button-image" src="/polarion/ria/images/actions/edit.gif?bundle=<%= bundleTimestamp %>">Rename
            </button>
            <button class="toolbar-button" onclick="Configurations.deleteConfiguration()">
                <img class="button-image" src="/polarion/ria/images/actions/delete.gif?bundle=<%= bundleTimestamp %>">Delete
            </button>
            <button class="new-configuration toolbar-button" style="display: inline-block" onclick="Configurations.newConfiguration()">
                <img class="button-image" src="/polarion/ria/images/control/tablePlus.png?bundle=<%= bundleTimestamp %>">Add new
            </button>
        </div>
        <div id="default-note" class="note" style="display: none">Default <span class="configuration-label">configuration</span> can't be renamed or deleted</div>
        <div id="global-note" class="note" style="display: none">This is a <span class="configuration-label">configuration</span> inherited from a global scope,
            to rename or delete it on project scope you need first to save it on this level</div>
        <div id="configurations-load-error" class="configuration-error" style="display: none">There was an error loading <span class="configuration-label">configuration</span>s</div>
        <div id="configuration-delete-error" class="configuration-error" style="display: none">There was an error deleting <span class="configuration-label">configuration</span></div>
    </div>
    <div id="edit-configuration-pane" style="display: none">
        <label for="new-configuration-input" class="new-configuration">New <span class="configuration-label-capitalized">Configuration</span>:</label>
        <input type="text" id="new-configuration-input" class="new-configuration" maxlength="40" />
        <label for="edit-configuration-input" class="edit-configuration">Edit <span class="configuration-label-capitalized">Configuration</span>:</label>
        <input type="text" id="edit-configuration-input" class="edit-configuration" maxlength="40" />
        <div class="action-buttons">
            <button class="toolbar-button" onclick="Configurations.cancelEditConfiguration()">
                <img class="button-image" src="/polarion/ria/images/actions/cancel.gif?bundle=<%= bundleTimestamp %>">Cancel
            </button>
            <button class="new-configuration toolbar-button" onclick="Configurations.saveConfiguration()">
                <img class="button-image" src="/polarion/ria/images/actions/save.gif?bundle=<%= bundleTimestamp %>">Save
            </button>
            <button class="edit-configuration toolbar-button" onclick="Configurations.updateConfiguration()">
                <img class="button-image" src="/polarion/ria/images/actions/save.gif?bundle=<%= bundleTimestamp %>">Update
            </button>
        </div>
        <div id="configuration-clashes-error" class="configuration-error" style="display: none">There is already a <span class="configuration-label">configuration</span> with such name in this scope</div>
        <div id="invalid-value-error" class="configuration-error" style="display: none">Only alphanumeric characters, hyphens and spaces are allowed</div>
        <div id="configuration-save-error" class="configuration-error" style="display: none">There was an error saving <span class="configuration-label">configuration</span></div>
    </div>
</div>