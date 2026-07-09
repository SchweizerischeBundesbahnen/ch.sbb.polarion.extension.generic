<%@ page contentType="text/html;charset=UTF-8" %>

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<div class="notifications">
    <div id="newer-version-warning" class="alert alert-warning" style="display: none">
        <span class="alert-icon" role="img" aria-label="Warning"></span>
        <span>A newer plugin version installed since the data below was persisted which can lead to unexpected behaviour.
            Consider checking if persisted data is still compatible and/or relevant to a newer plugin version. <span style="color: #999999">This message will be hidden after the next save.</span>
        </span>
    </div>
    <div id="data-loading-error" class="alert alert-error" style="display: none">
        <span class="alert-icon" role="img" aria-label="Error"></span>
        <span>Error occurred loading data</span>
    </div>
    <div id="revisions-loading-error" class="alert alert-error" style="display: none">
        <span class="alert-icon" role="img" aria-label="Error"></span>
        <span>Error occurred loading list of revisions</span>
    </div>
</div>
