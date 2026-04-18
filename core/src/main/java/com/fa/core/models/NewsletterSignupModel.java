package com.fa.core.models;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import javax.annotation.PostConstruct;

@Model(adaptables = SlingHttpServletRequest.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class NewsletterSignupModel {

    private static final String VARIANT_POPUP = "popup";
    private static final String VARIANT_FOOTER = "footer";

    private static final String METHOD_POST = "POST";

    private static final String DEFAULT_TITLE = "Stay informed";
    private static final String DEFAULT_DESCRIPTION = "Subscribe to receive the latest news and updates.";
    private static final String DEFAULT_FORM_ARIA_LABEL = "Newsletter signup form";

    private static final String DEFAULT_NAME_LABEL = "Name";
    private static final String DEFAULT_NAME_PLACEHOLDER = "Enter your name";
    private static final String DEFAULT_NAME_REQUIRED_MESSAGE = "Please enter your name.";

    private static final String DEFAULT_EMAIL_LABEL = "Email";
    private static final String DEFAULT_EMAIL_PLACEHOLDER = "Enter your email address";
    private static final String DEFAULT_EMAIL_REQUIRED_MESSAGE = "Please enter your email address";
    private static final String DEFAULT_EMAIL_INVALID_MESSAGE = "Please enter a valid email address";

    private static final String DEFAULT_SUBMIT_LABEL = "Subscribe";
    private static final String DEFAULT_PRIVACY_TEXT = "By subscribing, you agree to receive newsletter updates";
    private static final String DEFAULT_SUCCESS_MESSAGE = "Thank you for subscribing";
    private static final String DEFAULT_ERROR_MESSAGE = "Something went wrong. Please try again later.";

    private static final String DEFAULT_CLOSE_ARIA_LABEL = "Close newsletter signup dialog";
    private static final String DEFAULT_CLOSE_BUTTON_LABEL = "Close";

    private static final String DEFAULT_FREQUENCY_MODE = "session";

    private static final String DEFAULT_TRIGGER_SELECTOR = "open-newsletter";

    private static final long DEFAULT_AUTO_OPEN_DELAY = 2000L;
    private static final long DEFAULT_FREQUENCY_DAYS = 30L;

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private Resource resource;

    @ValueMapValue
    private String variant;

    @ValueMapValue
    private String title;

    @ValueMapValue
    private String description;

    @ValueMapValue
    private String endpoint;

    @ValueMapValue
    private String method;

    @ValueMapValue
    private String formAriaLabel;

    @ValueMapValue
    private String nameLabel;

    @ValueMapValue
    private String namePlaceholder;

    @ValueMapValue
    private Boolean requireName;

    @ValueMapValue
    private String nameRequiredMessage;

    @ValueMapValue
    private String emailLabel;

    @ValueMapValue
    private String emailPlaceholder;

    @ValueMapValue
    private Boolean requireEmail;

    @ValueMapValue
    private String emailRequiredMessage;

    @ValueMapValue
    private String emailInvalidMessage;

    @ValueMapValue
    private Boolean trackingFieldsEnabled;

    @ValueMapValue
    private String newsletterId;

    @ValueMapValue
    private String placement;

    @ValueMapValue
    private String campaignSource;

    @ValueMapValue
    private String submitLabel;

    @ValueMapValue
    private String privacyText;

    @ValueMapValue
    private String successMessage;

    @ValueMapValue
    private String errorMessage;

    @ValueMapValue
    private Boolean autoOpenEnabled;

    @ValueMapValue
    private Long autoOpenDelay;

    @ValueMapValue
    private String frequencyMode;

    @ValueMapValue
    private Long frequencyDays;

    @ValueMapValue
    private String openTriggerSelector;

    @ValueMapValue
    private String closeAriaLabel;

    @ValueMapValue
    private String closeButtonLabel;

    private String id;
    private boolean configured;
    private boolean popupVariant;

    @PostConstruct
    protected void init() {
        variant = normalizeVariant(variant);
        popupVariant = VARIANT_POPUP.equals(variant);

        title = StringUtils.defaultIfBlank(title, DEFAULT_TITLE);
        description = StringUtils.defaultIfBlank(description, DEFAULT_DESCRIPTION);

        endpoint = StringUtils.trimToEmpty(endpoint);
        configured = StringUtils.isNotBlank(endpoint);

        method = StringUtils.upperCase(StringUtils.defaultIfBlank(method, METHOD_POST));
        formAriaLabel = StringUtils.defaultIfBlank(formAriaLabel, DEFAULT_FORM_ARIA_LABEL);

        nameLabel = StringUtils.defaultIfBlank(nameLabel, DEFAULT_NAME_LABEL);
        namePlaceholder = StringUtils.defaultIfBlank(namePlaceholder, DEFAULT_NAME_PLACEHOLDER);
        requireName = requireName == null ? Boolean.TRUE : requireName;
        nameRequiredMessage = StringUtils.defaultIfBlank(nameRequiredMessage, DEFAULT_NAME_REQUIRED_MESSAGE);

        emailLabel = StringUtils.defaultIfBlank(emailLabel, DEFAULT_EMAIL_LABEL);
        emailPlaceholder = StringUtils.defaultIfBlank(emailPlaceholder, DEFAULT_EMAIL_PLACEHOLDER);
        requireEmail = requireEmail == null ? Boolean.TRUE : requireEmail;
        emailRequiredMessage = StringUtils.defaultIfBlank(emailRequiredMessage, DEFAULT_EMAIL_REQUIRED_MESSAGE);
        emailInvalidMessage = StringUtils.defaultIfBlank(emailInvalidMessage, DEFAULT_EMAIL_INVALID_MESSAGE);

        trackingFieldsEnabled = trackingFieldsEnabled == null ? Boolean.TRUE : trackingFieldsEnabled;

        submitLabel = StringUtils.defaultIfBlank(submitLabel, DEFAULT_SUBMIT_LABEL);
        privacyText = StringUtils.defaultIfBlank(privacyText, DEFAULT_PRIVACY_TEXT);
        successMessage = StringUtils.defaultIfBlank(successMessage, DEFAULT_SUCCESS_MESSAGE);
        errorMessage = StringUtils.defaultIfBlank(errorMessage, DEFAULT_ERROR_MESSAGE);

        autoOpenEnabled = autoOpenEnabled != null && autoOpenEnabled;
        autoOpenDelay = autoOpenDelay == null || autoOpenDelay < 0 ? DEFAULT_AUTO_OPEN_DELAY : autoOpenDelay;

        frequencyMode = StringUtils.defaultIfBlank(frequencyMode, DEFAULT_FREQUENCY_MODE);
        frequencyDays = frequencyDays == null || frequencyDays < 1 ? DEFAULT_FREQUENCY_DAYS : frequencyDays;

        openTriggerSelector = StringUtils.defaultIfBlank(openTriggerSelector, DEFAULT_TRIGGER_SELECTOR);

        closeAriaLabel = StringUtils.defaultIfBlank(closeAriaLabel, DEFAULT_CLOSE_ARIA_LABEL);
        closeButtonLabel = StringUtils.defaultIfBlank(closeButtonLabel, DEFAULT_CLOSE_BUTTON_LABEL);

        id = buildId(resource);
    }

    private String normalizeVariant(final String rawVariant) {
        return VARIANT_POPUP.equalsIgnoreCase(rawVariant) ? VARIANT_POPUP : VARIANT_FOOTER;
    }

    private String buildId(final Resource currentResource) {
        if (currentResource == null || StringUtils.isBlank(currentResource.getPath())) {
            return "newsletter-signup";
        }
        return "newsletter-signup-" + currentResource.getPath()
                .replaceAll("^/|/$", "")
                .replaceAll("[^A-Za-z0-9]+", "-");
    }

    public String getId() {
        return id;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getVariant() {
        return variant;
    }

    public boolean isPopupVariant() {
        return popupVariant;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getMethod() {
        return method;
    }

    public String getFormAriaLabel() {
        return formAriaLabel;
    }

    public String getNameLabel() {
        return nameLabel;
    }

    public String getNamePlaceholder() {
        return namePlaceholder;
    }

    public boolean isRequireName() {
        return requireName;
    }

    public String getNameRequiredMessage() {
        return nameRequiredMessage;
    }

    public String getEmailLabel() {
        return emailLabel;
    }

    public String getEmailPlaceholder() {
        return emailPlaceholder;
    }

    public boolean isRequireEmail() {
        return requireEmail;
    }

    public String getEmailRequiredMessage() {
        return emailRequiredMessage;
    }

    public String getEmailInvalidMessage() {
        return emailInvalidMessage;
    }

    public boolean isTrackingFieldsEnabled() {
        return trackingFieldsEnabled;
    }

    public String getNewsletterId() {
        return newsletterId;
    }

    public String getPlacement() {
        return placement;
    }

    public String getCampaignSource() {
        return campaignSource;
    }

    public String getSubmitLabel() {
        return submitLabel;
    }

    public String getPrivacyText() {
        return privacyText;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isAutoOpenEnabled() {
        return popupVariant && autoOpenEnabled;
    }

    public long getAutoOpenDelay() {
        return autoOpenDelay;
    }

    public String getFrequencyMode() {
        return frequencyMode;
    }

    public long getFrequencyDays() {
        return frequencyDays;
    }

    public String getOpenTriggerSelector() {
        if (StringUtils.isBlank(openTriggerSelector)) {
            return "";
        }

        String value = openTriggerSelector.trim();

        if (value.startsWith(".") || value.startsWith("#") || value.startsWith("[")) {
            return value;
        }

        return "." + value;
    }

    public String getCloseAriaLabel() {
        return closeAriaLabel;
    }

    public String getCloseButtonLabel() {
        return closeButtonLabel;
    }
}