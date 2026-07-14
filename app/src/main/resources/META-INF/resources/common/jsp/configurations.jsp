<p>There can be multiple named <span class="configuration-label">configuration</span>s. Please, choose one you would like to modify in dropdown below.
    <span id="default-cannot-be-deleted-note">Be aware that "Default" <span class="configuration-label">configuration</span> on global scope can't be deleted or renamed.</span></p>
<%-- sbb-ui makes this panel self-contained: its .toolbar-button controls and .sbb-icon-* glyphs read
     --sbb-* tokens (control-tokens.css) even if a page ever includes this fragment outside a
     .standard-admin-page / other scope wrapper. Post-#535 the tokens are no longer on :root, so an
     unscoped include would render the buttons and their icons unstyled. --%>
<div class="input-group common-configuration-panel sbb-ui">
    <div id="configurations-pane">
        <label id="configurations-label"><span class="configuration-label-capitalized">Configuration</span>:</label>
        <select id="configurations-select"></select>
        <div class="action-buttons">
            <button id="configurations-button-edit" class="toolbar-button" onclick="Configurations.editConfiguration()">
                <span class="button-image sbb-icon-edit" role="img" aria-label="Rename"></span>Rename
            </button>
            <button id="configurations-button-delete" class="toolbar-button" onclick="Configurations.deleteConfiguration()">
                <span class="button-image sbb-icon-delete" role="img" aria-label="Delete"></span>Delete
            </button>
            <button id="configurations-button-create" class="new-configuration toolbar-button" style="display: inline-flex" onclick="Configurations.newConfiguration()">
                <span class="sbb-icon-table-plus" role="img" aria-label="Add" style="margin-right:5px"></span>Add new
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
            <button id="configurations-button-cancel-edit" class="toolbar-button" onclick="Configurations.cancelEditConfiguration()">
                <span class="button-image sbb-icon-cancel" role="img" aria-label="Cancel"></span>Cancel
            </button>
            <button id="configurations-button-save" class="new-configuration toolbar-button" onclick="Configurations.saveConfiguration()">
                <span class="button-image sbb-icon-save" role="img" aria-label="Save"></span>Save
            </button>
            <button id="configurations-button-update" class="edit-configuration toolbar-button" onclick="Configurations.updateConfiguration()">
                <span class="button-image sbb-icon-save" role="img" aria-label="Update"></span>Update
            </button>
        </div>
        <div id="configuration-clashes-error" class="configuration-error" style="display: none">There is already a <span class="configuration-label">configuration</span> with such name in this scope</div>
        <div id="invalid-value-error" class="configuration-error" style="display: none">Only alphanumeric characters, hyphens and spaces are allowed</div>
        <div id="configuration-save-error" class="configuration-error" style="display: none">There was an error saving <span class="configuration-label">configuration</span></div>
    </div>
</div>
