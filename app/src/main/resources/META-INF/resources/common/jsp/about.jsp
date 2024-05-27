<%@ page import="ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration" %>
<%@ page import="ch.sbb.polarion.extension.generic.rest.model.Version" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%@ page import="ch.sbb.polarion.extension.generic.util.VersionUtils" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Properties" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! Version version = ExtensionInfo.getInstance().getVersion(); %>
<%! Properties properties = CurrentExtensionConfiguration.getInstance().getExtensionConfiguration().getProperties(); %>

<head>
    <title></title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
    <link rel="stylesheet" href="../ui/generic/css/about.css?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>">
</head>

<body>
<div class="standard-admin-page about-page">
    <h1>About</h1>

    <div class="about-page-text">
        <img class="app-icon" src="../ui/images/app-icon.svg?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>" alt="" onerror="this.style.display='none'"/>

        <h3>Version</h3>

        <table>
            <tbody>
            <%
                out.println("<tr><td>%s</td><td>%s</td></tr>".formatted(VersionUtils.BUNDLE_NAME, version.getBundleName()));
                out.println("<tr><td>%s</td><td>%s</td></tr>".formatted(VersionUtils.BUNDLE_VENDOR, version.getBundleVendor()));
                if (version.getSupportEmail() != null) {
                    out.println("<tr><td>%s</td><td><a target=\"_blank\" href=\"mailto:%s\">%s</a></td></tr>".formatted(VersionUtils.SUPPORT_EMAIL, version.getSupportEmail(), version.getSupportEmail()));
                }
                out.println("<tr><td>%s</td><td>%s</td></tr>".formatted(VersionUtils.AUTOMATIC_MODULE_NAME, version.getAutomaticModuleName()));
                out.println("<tr><td>%s</td><td>%s</td></tr>".formatted(VersionUtils.BUNDLE_VERSION, version.getBundleVersion()));
                out.println("<tr><td>%s</td><td>%s</td></tr>".formatted(VersionUtils.BUNDLE_BUILD_TIMESTAMP, version.getBundleBuildTimestamp()));
            %>
            </tbody>
        </table>

        <h3>Configuration properties</h3>

        <table>
            <tbody>
            <%
                Enumeration<?> propertyNames = properties.propertyNames();
                while (propertyNames.hasMoreElements()) {
                    String key = (String) propertyNames.nextElement();
                    String value = properties.getProperty(key);
                    String row = "<tr><td>%s</td><td>%s</td></tr>".formatted(key, value);
                    out.println(row);
                }
            %>
            </tbody>
        </table>

        <div id="configuration_help"></div>

        <script type="application/javascript">
            fetch("../ui/html/help/configuration.html?bundle=<%= version.getBundleBuildTimestampDigitsOnly() %>")
                .then((response) => {
                    if (response.ok) {
                        return response.text()
                    } else {
                        throw new Error("no configuration help found", {cause: response});
                    }
                })
                .then((html) => {
                    document.getElementById("configuration_help").innerHTML = html;
                })
                .catch((error) => {
                    console.warn(error);
                });
        </script>
    </div>
</div>
</body>
</html>