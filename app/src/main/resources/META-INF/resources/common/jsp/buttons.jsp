<%@ page contentType="text/html;charset=UTF-8" %>

<div class="actions-pane hide-on-edit-configuration">
    <div id="revisions-expand-container">
        <table id="revisions-table">
            <thead>
            <tr>
                <th style="width: 10%;">Revision</th>
                <th style="width: 15%;">Baseline name</th>
                <th style="width: 10%;">Date</th>
                <th style="width: 20%;">Author</th>
                <th style="width: 35%;">Comment</th>
                <th style="width: 10%;">Actions</th>
            </tr>
            </thead>
            <tbody>
            </tbody>
        </table>
    </div>

    <div class="action-buttons inline-flex">
        <button id="save-toolbar-button" class="toolbar-button" type="submit" onclick="${param.saveFunction}">
            <span class="button-image sbb-icon-save" role="img" aria-label="Save" title="Save data"></span>Save
        </button>
        <button id="cancel-toolbar-button" class="toolbar-button" type="submit" onclick="${param.cancelFunction}">
            <span class="button-image sbb-icon-cancel" role="img" aria-label="Cancel"
                  title="Cancel editing and revert to last persisted state"></span>Cancel
        </button>
        <button id="default-toolbar-button" class="toolbar-button" type="submit" onclick="${param.defaultFunction}">
            <span class="button-image sbb-icon-revert" role="img" aria-label="Default" title="Load default values"></span>Default
        </button>
        <button id="revisions-toolbar-button" class="toolbar-button" type="submit" onclick="SbbCommon.toggleRevisions()">
            <span class="button-image sbb-icon-select-revision" role="img" aria-label="Revisions" title="Toggle list of revisions"></span>Revisions
        </button>
    </div>
    <div class="action-alerts inline-flex">
        <div id="action-error" class="alert alert-error" style="display: none"></div>
        <div id="action-success" class="alert alert-success" style="display: none"></div>
    </div>
</div>
