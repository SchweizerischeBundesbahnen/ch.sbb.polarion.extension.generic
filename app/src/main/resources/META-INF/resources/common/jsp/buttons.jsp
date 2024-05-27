<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page contentType="text/html;charset=UTF-8" %>

<%! Version version = ExtensionInfo.getInstance().getVersion();%>

<div class="actions-pane">
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
            <img class="button-image" alt="Save" title="Save data" src="/polarion/ria/images/actions/save.gif?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">Save
        </button>
        <button id="cancel-toolbar-button" class="toolbar-button" type="submit" onclick="${param.cancelFunction}">
            <img class="button-image" alt="Cancel" title="Cancel editing and revert to last persisted state"
                 src="/polarion/ria/images/actions/cancel.gif?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">Cancel
        </button>
        <button id="default-toolbar-button" class="toolbar-button" type="submit" onclick="${param.defaultFunction}">
            <img class="button-image" alt="Default" title="Load default values" src="/polarion/ria/images/actions/revert.gif?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">Default
        </button>
        <button id="revisions-toolbar-button" class="toolbar-button" type="submit" onclick="SbbCommon.toggleRevisions()">
            <img class="button-image" alt="Revisions" title="Toggle list of revisions" src="/polarion/ria/images/actions/select_revision.gif?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">Revisions
        </button>
    </div>
    <div class="action-alerts inline-flex">
        <div id="action-error" class="alert alert-error" style="display: none"></div>
        <div id="action-success" class="alert alert-success" style="display: none"></div>
    </div>
</div>