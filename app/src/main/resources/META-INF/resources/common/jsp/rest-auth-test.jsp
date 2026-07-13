<%@ page import="ch.sbb.polarion.extension.generic.util.ExtensionInfo" %>
<%--
    Fragment (statically included by about.jsp) providing a manual X-Polarion-REST-Token
    authentication test. Shown only when the extension's debug mode is enabled.
--%>
<% String restAuthTestUrl = "/polarion/" + ExtensionInfo.getInstance().getContext().getExtensionContext() + "/rest/api/version"; %>

<h3>REST API authentication test</h3>

<p>
    Sends <code>GET <%= restAuthTestUrl %></code>
    with the current session's <code>X-Polarion-REST-Token</code> header, obtained via
    <code>top.getRestApiToken()</code>. Use it to verify that in-session REST authentication works.
    The token requires <code>com.siemens.polarion.rest.security.restApiToken.enabled=true</code> in
    <code>polarion.properties</code>.
</p>

<button type="button" id="rest-auth-test-button" class="sbb-btn sbb-btn--action">Test REST authentication</button>
<pre id="rest-auth-test-output" class="rest-auth-test-output" style="display: none;"></pre>

<script>
    (function () {
        const restApiUrl = "<%= restAuthTestUrl %>";
        const button = document.getElementById("rest-auth-test-button");
        const output = document.getElementById("rest-auth-test-output");

        function showResult(text, success) {
            output.style.display = "block";
            output.textContent = text;
            output.classList.remove("success", "failure");
            output.classList.add(success ? "success" : "failure");
        }

        button.addEventListener("click", function () {
            let token;
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
