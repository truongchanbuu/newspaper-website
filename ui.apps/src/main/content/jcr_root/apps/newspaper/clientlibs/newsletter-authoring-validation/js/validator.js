(function (window, document, Granite) {
    "use strict";

    var $ = Granite.$;
    var registry = $(window).adaptTo("foundation-registry");

    if (!registry) {
        return;
    }

    function getBooleanAttr(el, name, defaultValue) {
        var value = el.getAttribute("data-" + name);

        if (value === null || value === undefined || value === "") {
            return defaultValue;
        }

        return String(value).toLowerCase() === "true";
    }

    function getStringAttr(el, name, defaultValue) {
        var value = el.getAttribute("data-" + name);
        return value === null || value === undefined || value === "" ? defaultValue : value;
    }

    function parseUrl(value) {
        try {
            return new URL(value);
        } catch (e) {
            return null;
        }
    }

    function isEditorUrl(value) {
        return /\/editor\.html(\/|$)/.test(value);
    }

    function isLocalhost(hostname) {
        if (!hostname) {
            return false;
        }

        var normalized = String(hostname).toLowerCase();

        return normalized === "localhost" || normalized === "127.0.0.1" || normalized === "::1";
    }

    function validateEndpoint(el) {
        var value = (el.value || "").trim();

        if (!value) {
            return "Endpoint URL is required";
        }

        var url = parseUrl(value);

        if (!url) {
            return getStringAttr(el, "invalid-message", "Endpoint must be a valid HTTPS URL");
        }

        var allowHttp = getBooleanAttr(el, "allow-http", false);
        var allowLocalhost = getBooleanAttr(el, "allow-localhost", false);

        if (!allowHttp && url.protocol !== "https:") {
            return getStringAttr(el, "protocol-message", "Endpoint must start with https://");
        }

        if (!url.hostname) {
            return getStringAttr(el, "invalid-message", "Endpoint must be a valid HTTPS URL");
        }

        if (!allowLocalhost && isLocalhost(url.hostname)) {
            return getStringAttr(el, "localhost-message", "localhost is not allowed");
        }

        if (isEditorUrl(value)) {
            return getStringAttr(el, "editor-message", "Author/editor URLs are not allowed");
        }

        return;
    }

    registry.register("foundation.validation.validator", {
        selector: "[data-validation='newsletter-endpoint']",
        validate: function (el) {
            return validateEndpoint(el);
        }
    });
})(window, document, Granite);