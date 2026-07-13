<%@ page import="ch.sbb.polarion.extension.generic.configuration.ConfigurationStatus" %>
<%@ page import="ch.sbb.polarion.extension.generic.configuration.ConfigurationStatusProvider" %>
<%@ page import="ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration" %>
<%@ page import="ch.sbb.polarion.extension.generic.properties.ConfigurationProperties" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Context" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.VersionUtils" %>
<%@ page import="org.jetbrains.annotations.Nullable" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.nio.charset.StandardCharsets" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collection" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.List" %>
<%@ page import="org.jetbrains.annotations.NotNull" %>
<%@ page contentType="text/html; charset=UTF-8" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%!
    private static final String ABOUT_TABLE_ROW = "<tr><td>%s</td><td>%s</td></tr>";
    private static final String CONFIGURATION_PROPERTIES_TABLE_ROW = "<tr><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>";
    private static final String OBSOLETE_CONFIGURATION_PROPERTIES_TABLE_ROW = "<tr><td>%s</td><td>%s</td></tr>";
    private static final String CHECK_CONFIGURATION_TABLE_ROW = "<tr><td>%s</td><td>%s</td><td>%s</td></tr>";

    Context context = ExtensionInfo.getInstance().getContext();
    Version version = ExtensionInfo.getInstance().getVersion();
    ConfigurationProperties configurationProperties = CurrentExtensionConfiguration.getInstance().getExtensionConfiguration().getConfigurationProperties();
    ConfigurationProperties obsoleteConfigurationProperties = CurrentExtensionConfiguration.getInstance().getExtensionConfiguration().getObsoleteConfigurationProperties();
%>

<head>
    <title></title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../ui/generic/css/buttons.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../ui/generic/css/about.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../ui/generic/css/github-markdown-light.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
</head>

<body>
<div class="standard-admin-page about-page">
    <h1>About</h1>

    <div class="about-page-text">
        <img class="app-icon" src="../ui/images/app-icon.svg?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>" alt="" onerror="this.style.display='none'"/>

        <h3>Extension info</h3>

        <table>
            <thead>
            <tr>
                <th>Manifest entry</th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody>
            <%
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_NAME, version.getBundleName()));
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_VENDOR, version.getBundleVendor()));
                if (version.getSupportEmail() != null) {
                    String mailToLink = "<a target=\"_blank\" href=\"mailto:%s\">%s</a>".formatted(version.getSupportEmail(), version.getSupportEmail());
                    out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.SUPPORT_EMAIL, mailToLink));
                }
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.AUTOMATIC_MODULE_NAME, version.getAutomaticModuleName()));
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_VERSION, version.getBundleVersion()));
                out.println(ABOUT_TABLE_ROW.formatted(VersionUtils.BUNDLE_BUILD_TIMESTAMP, version.getBundleBuildTimestamp()));
            %>
            </tbody>
        </table>

        <h3>REST API authentication test</h3>

        <p>
            Sends <code>GET <%= "/polarion/" + context.getExtensionContext() + "/rest/api/version" %></code>
            with the current session's <code>X-Polarion-REST-Token</code> header, obtained via
            <code>top.getRestApiToken()</code>. Use it to verify that in-session REST authentication works.
            The token requires <code>com.siemens.polarion.rest.security.restApiToken.enabled=true</code> in
            <code>polarion.properties</code>.
        </p>

        <button type="button" id="rest-auth-test-button" class="sbb-btn sbb-btn--action">Test REST authentication</button>
        <pre id="rest-auth-test-output" class="rest-auth-test-output" style="display: none;"></pre>

        <script>
            (function () {
                var restApiUrl = "<%= "/polarion/" + context.getExtensionContext() + "/rest/api/version" %>";
                var button = document.getElementById("rest-auth-test-button");
                var output = document.getElementById("rest-auth-test-output");

                function showResult(text, success) {
                    output.style.display = "block";
                    output.textContent = text;
                    output.classList.remove("success", "failure");
                    output.classList.add(success ? "success" : "failure");
                }

                button.addEventListener("click", function () {
                    var token;
                    try {
                        token = top.getRestApiToken();
                    } catch (e) {
                        showResult("Unable to obtain a token via top.getRestApiToken(): " + e, false);
                        return;
                    }
                    if (!token) {
                        showResult("top.getRestApiToken() returned no token. Make sure the REST API token is enabled and you are logged in.", false);
                        return;
                    }

                    button.disabled = true;
                    showResult("Calling " + restApiUrl + " …", true);

                    fetch(restApiUrl, {
                        method: "GET",
                        headers: {
                            "Accept": "application/json",
                            "X-Polarion-REST-Token": token
                        }
                    }).then(function (response) {
                        return response.text().then(function (body) {
                            showResult("HTTP " + response.status + " " + response.statusText + "\n\n" + body, response.ok);
                        });
                    }).catch(function (e) {
                        showResult("Request failed: " + e, false);
                    }).finally(function () {
                        button.disabled = false;
                    });
                });
            })();
        </script>

        <h3>Extension configuration properties</h3>

        <table>
            <thead>
            <tr>
                <th>Configuration property</th>
                <th>Value</th>
                <th>Default</th>
                <th>Description</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<String> propertyKeys = new ArrayList<>(configurationProperties.keySet());
                Collections.sort(propertyKeys);

                for (String key : propertyKeys) {
                    ConfigurationProperties.Value configurationPropertiesValue = configurationProperties.getProperty(key);
                    @NotNull String value = configurationPropertiesValue.value();
                    @Nullable String defaultValue = configurationPropertiesValue.defaultValue();
                    @Nullable String description = configurationPropertiesValue.description();
                    String row = CONFIGURATION_PROPERTIES_TABLE_ROW.formatted(key, value, defaultValue == null ? "" : defaultValue, description == null ? "" : description);
                    out.println(row);
                }
            %>
            </tbody>
        </table>

        <% if (!obsoleteConfigurationProperties.isEmpty()) { %>
        <h3>Obsolete/non-valid configuration properties</h3>

        <table>
            <thead>
            <tr>
                <th>Configuration property</th>
                <th>Value</th>
            </tr>
            </thead>
            <tbody>
            <%
                List<String> obsoletePropertyKeys = new ArrayList<>(obsoleteConfigurationProperties.keySet());
                Collections.sort(obsoletePropertyKeys);

                for (String obsoleteKey : obsoletePropertyKeys) {
                    ConfigurationProperties.Value obsoleteConfigurationPropertiesValue = obsoleteConfigurationProperties.getProperty(obsoleteKey);
                    @NotNull String value = obsoleteConfigurationPropertiesValue.value();
                    String row = OBSOLETE_CONFIGURATION_PROPERTIES_TABLE_ROW.formatted(obsoleteKey, value);
                    out.println(row);
                }
            %>
            </tbody>
        </table>
        <% } %>

        <% Collection<ConfigurationStatus> configurationStatuses = ConfigurationStatusProvider.getAllStatuses(request.getParameter("scope")); %>
        <% if (!configurationStatuses.isEmpty()) { %>
        <h3>Extension configuration status</h3>

        <table>
            <thead>
            <tr>
                <th>Configuration</th>
                <th>Status</th>
                <th>Info</th>
            </tr>
            </thead>
            <tbody>
            <%
                for (ConfigurationStatus configurationStatus : configurationStatuses) {
                    String row = CHECK_CONFIGURATION_TABLE_ROW.formatted(configurationStatus.getName(), configurationStatus.getStatus().toHtml(), configurationStatus.getDetails());
                    out.println(row);
                }
            %>
            </tbody>
        </table>
        <% } %>

        <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>

        <article class="markdown-body">
            <%
                String extensionContext = context.getExtensionContext();
                try (InputStream inputStream = ExtensionInfo.class.getResourceAsStream("/webapp/" + extensionContext + "-admin/html/about.html")) {
                    if (inputStream != null) {
                        String configurationHelp = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        out.println(configurationHelp);
                    } else {
                        out.println("No help has been generated during build. Please check <a href=\"" + version.getProjectURL() + "/README.md\" target=\"_blank\">the online documentation</a>.");
                    }
                }
            %>
        </article>
    </div>
</div>
</body>
</html>
