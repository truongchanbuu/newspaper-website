type PopupFrequencyMode = "session" | "local" | "none";

interface NewsletterSignupPayload {
    name: string;
    email: string;
    newsletterId?: string;
    placement?: string;
    campaignSource?: string;
}

class NewsletterSignup {
    private static readonly FETCH_TIMEOUT_MS = 8_000;

    private readonly root: HTMLElement;
    private readonly form: HTMLFormElement | null;
    private readonly nameInput: HTMLInputElement | null;
    private readonly emailInput: HTMLInputElement | null;
    private readonly nameError: HTMLElement | null;
    private readonly emailError: HTMLElement | null;
    private readonly successMessage: HTMLElement | null;
    private readonly errorMessage: HTMLElement | null;
    private readonly submitButton: HTMLButtonElement | HTMLInputElement | null;
    private readonly dialog: HTMLElement | null;
    private readonly backdrop: HTMLElement | null;
    private readonly closeButton: HTMLElement | null;

    private readonly endpoint: string;
    private readonly variant: string;
    private readonly isPopup: boolean;
    private readonly autoOpenEnabled: boolean;
    private readonly autoOpenDelay: number;
    private readonly frequencyMode: PopupFrequencyMode;
    private readonly frequencyDays: number;
    private readonly openTriggerSelector: string;

    private lastFocusedElement: Element | null = null;
    private autoOpenTimer: number | null = null;

    constructor(root: HTMLElement) {
        this.root = root;
        this.form         = root.querySelector<HTMLFormElement>('[data-cmp-hook-newsletter="form"]');
        this.nameInput    = root.querySelector<HTMLInputElement>('[data-cmp-hook-newsletter="name"]');
        this.emailInput   = root.querySelector<HTMLInputElement>('[data-cmp-hook-newsletter="email"]');
        this.nameError    = root.querySelector<HTMLElement>('[data-cmp-hook-newsletter="name-error"]');
        this.emailError   = root.querySelector<HTMLElement>('[data-cmp-hook-newsletter="email-error"]');
        this.successMessage = root.querySelector<HTMLElement>('[data-cmp-hook-newsletter="success"]');
        this.errorMessage   = root.querySelector<HTMLElement>('[data-cmp-hook-newsletter="error"]');
        this.submitButton   = root.querySelector<HTMLButtonElement | HTMLInputElement>('[data-cmp-hook-newsletter="submit"]');
        this.dialog       = root.querySelector<HTMLElement>('[data-cmp-hook-newsletter="dialog"]');
        this.backdrop     = root.querySelector<HTMLElement>('[data-cmp-hook-newsletter="backdrop"]');
        this.closeButton  = root.querySelector<HTMLElement>('[data-cmp-hook-newsletter="close"]');

        this.endpoint            = root.dataset.cmpEndpoint?.trim() ?? "";
        this.variant             = root.dataset.cmpVariant?.trim() ?? "footer";
        this.autoOpenEnabled     = root.dataset.cmpAutoOpen === "true";
        this.autoOpenDelay       = Number(root.dataset.cmpAutoOpenDelay ?? "3000");
        this.frequencyMode       = this.parseFrequencyMode(root.dataset.cmpFrequencyMode);
        this.frequencyDays       = Number(root.dataset.cmpFrequencyDays ?? "7");
        this.openTriggerSelector = root.dataset.cmpOpenTriggerSelector?.trim() ?? "";

        // Pre-computed once — variant and DOM refs are immutable after construction
        this.isPopup = this.variant === "popup" && !!this.dialog && !!this.backdrop;
    }

    public init(): void {
        if (!this.form) return;

        this.bindForm();
        this.bindFieldInteractions();

        if (this.isPopup) {
            this.bindPopup();
            this.scheduleAutoOpen();
        }
    }

    // ── Binding ───────────────────────────────────────────────────────────────

    private bindForm(): void {
        this.form?.addEventListener("submit", this.handleSubmit);
    }

    private bindFieldInteractions(): void {
        this.nameInput?.addEventListener("input",  () => this.clearFieldError(this.nameInput,  this.nameError));
        this.emailInput?.addEventListener("input", () => this.clearFieldError(this.emailInput, this.emailError));
    }

    private bindPopup(): void {
        this.closeButton?.addEventListener("click", this.closePopup);
        this.backdrop?.addEventListener("click",    this.closePopup);
        document.addEventListener("keydown",        this.handleDocumentKeydown);

        if (this.openTriggerSelector) {
            document.addEventListener("click", (event) => {
                const trigger = (event.target as Element | null)?.closest(this.openTriggerSelector);
                if (!trigger) return;
                event.preventDefault();
                this.openPopup();
            });
        }
    }

    private scheduleAutoOpen(): void {
        if (!this.autoOpenEnabled || this.shouldSuppressPopup()) return;

        this.autoOpenTimer = window.setTimeout(
            () => this.openPopup(),
            Math.max(0, this.autoOpenDelay)
        );
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    private readonly handleSubmit = async (event: Event): Promise<void> => {
        event.preventDefault();
        this.hideGlobalMessages();

        if (!this.validate()) return;

        if (!this.endpoint) {
            this.showGlobalError("Missing endpoint configuration.");
            return;
        }

        const payload    = this.buildPayload();
        const controller = new AbortController();
        const timeoutId  = window.setTimeout(
            () => controller.abort(),
            NewsletterSignup.FETCH_TIMEOUT_MS
        );

        this.setLoading(true);

        try {
            const response = await fetch(this.endpoint, {
                method:  "POST",
                headers: { "Content-Type": "application/json", Accept: "application/json" },
                body:    JSON.stringify(payload),
                signal:  controller.signal,
            });

            if (!response.ok) {
                throw new Error(`Newsletter signup failed with status ${response.status}`);
            }

            this.handleSuccess();
        } catch (error) {
            this.handleError(error);
        } finally {
            window.clearTimeout(timeoutId);
            this.setLoading(false);
        }
    };

    // ── Popup open / close ────────────────────────────────────────────────────

    private openPopup(): void {
        if (!this.isPopup || !this.dialog || !this.backdrop) return;

        this.lastFocusedElement  = document.activeElement;
        this.dialog.hidden       = false;
        this.backdrop.hidden     = false;
        this.root.classList.add("is-open");

        // rAF ensures focus is applied after the browser has painted the dialog
        requestAnimationFrame(() => {
            (this.nameInput ?? this.emailInput ?? this.closeButton ?? this.dialog)?.focus();
        });
    }

    private readonly closePopup = (): void => {
        if (!this.isPopup || !this.dialog || !this.backdrop) return;

        this.dialog.hidden   = true;
        this.backdrop.hidden = true;
        this.root.classList.remove("is-open");
        this.rememberPopupDismissal();

        if (this.lastFocusedElement instanceof HTMLElement) {
            this.lastFocusedElement.focus();
        }
    };

    private readonly handleDocumentKeydown = (event: KeyboardEvent): void => {
        if (!this.isPopup || !this.root.classList.contains("is-open") || !this.dialog) return;

        if (event.key === "Escape") {
            event.preventDefault();
            this.closePopup();
            return;
        }

        if (event.key === "Tab") this.trapFocus(event);
    };

    private trapFocus(event: KeyboardEvent): void {
        if (!this.dialog) return;

        const focusable = Array.from(
            this.dialog.querySelectorAll<HTMLElement>(
                'a[href],button:not([disabled]),input:not([disabled]),select:not([disabled]),textarea:not([disabled]),[tabindex]:not([tabindex="-1"])'
            )
        ).filter((el) => !el.hasAttribute("hidden"));

        if (!focusable.length) return;

        const first  = focusable[0];
        const last   = focusable[focusable.length - 1];
        const active = document.activeElement;

        if (event.shiftKey && active === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && active === last) {
            event.preventDefault();
            first.focus();
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private validate(): boolean {
        let isValid = true;

        const name  = this.nameInput?.value.trim()  ?? "";
        const email = this.emailInput?.value.trim() ?? "";

        if (this.nameInput?.required && !name) {
            this.showFieldError(this.nameInput, this.nameError,
                this.nameInput.dataset.requiredMessage || "Please enter your name");
            isValid = false;
        } else if (this.nameInput) {
            this.clearFieldError(this.nameInput, this.nameError);
        }

        if (this.emailInput?.required && !email) {
            this.showFieldError(this.emailInput, this.emailError,
                this.emailInput.dataset.requiredMessage || "Please enter your email address");
            isValid = false;
        } else if (this.emailInput && email && !this.isValidEmail(email)) {
            this.showFieldError(this.emailInput, this.emailError,
                this.emailInput.dataset.invalidMessage || "Please enter a valid email address");
            isValid = false;
        } else if (this.emailInput) {
            this.clearFieldError(this.emailInput, this.emailError);
        }

        return isValid;
    }

    private isValidEmail(value: string): boolean {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
    }

    // ── Payload ───────────────────────────────────────────────────────────────

    private buildPayload(): NewsletterSignupPayload {
        // form is guaranteed non-null here — only called after validate() inside handleSubmit
        const formData = new FormData(this.form!);
        return {
            name:           String(formData.get("name")  ?? "").trim(),
            email:          String(formData.get("email") ?? "").trim(),
            newsletterId:   this.getOptionalFormValue(formData, "newsletterId"),
            placement:      this.getOptionalFormValue(formData, "placement"),
            campaignSource: this.getOptionalFormValue(formData, "campaignSource"),
        };
    }

    private getOptionalFormValue(formData: FormData, key: string): string | undefined {
        const value = String(formData.get(key) ?? "").trim();
        return value || undefined;
    }

    // ── State updates ─────────────────────────────────────────────────────────

    private handleSuccess(): void {
        this.root.classList.remove("is-error");
        this.root.classList.add("is-success");
        if (this.successMessage) this.successMessage.hidden = false;
        if (this.errorMessage)   this.errorMessage.hidden   = true;
        this.form?.reset();
        if (this.isPopup) this.rememberPopupDismissal();
    }

    private handleError(error: unknown): void {
        console.error("[newsletter-signup] submit error", error);
        this.showGlobalError();
    }

    private showGlobalError(message?: string): void {
        this.root.classList.remove("is-success");
        this.root.classList.add("is-error");
        if (this.errorMessage) {
            if (message) this.errorMessage.textContent = message;
            this.errorMessage.hidden = false;
        }
        if (this.successMessage) this.successMessage.hidden = true;
    }

    private hideGlobalMessages(): void {
        this.root.classList.remove("is-error", "is-success");
        if (this.successMessage) this.successMessage.hidden = true;
        if (this.errorMessage)   this.errorMessage.hidden   = true;
    }

    private setLoading(isLoading: boolean): void {
        this.root.classList.toggle("is-loading", isLoading);
        // Native `disabled` communicates state to AT — no aria-disabled needed
        if (this.submitButton) this.submitButton.disabled = isLoading;
        if (this.nameInput)    this.nameInput.disabled    = isLoading;
        if (this.emailInput)   this.emailInput.disabled   = isLoading;
    }

    private showFieldError(input: HTMLInputElement, errorEl: HTMLElement | null, message: string): void {
        input.setAttribute("aria-invalid", "true");
        if (errorEl) {
            errorEl.textContent = message;
            errorEl.hidden      = false;
        }
    }

    private clearFieldError(input: HTMLInputElement | null, errorEl: HTMLElement | null): void {
        input?.removeAttribute("aria-invalid");
        if (errorEl) {
            errorEl.textContent = "";
            errorEl.hidden      = true;
        }
    }

    // ── Popup suppression (frequency) ─────────────────────────────────────────

    private shouldSuppressPopup(): boolean {
        if (this.frequencyMode === "none") return false;

        const key = this.getPopupStorageKey();
        try {
            if (this.frequencyMode === "session") {
                return window.sessionStorage.getItem(key) === "1";
            }
            if (this.frequencyMode === "local") {
                const raw = window.localStorage.getItem(key);
                if (!raw) return false;
                const expiresAt = Number(raw);
                return Number.isFinite(expiresAt) && Date.now() < expiresAt;
            }
        } catch (error) {
            console.warn("[newsletter-signup] storage unavailable", error);
        }
        return false;
    }

    private rememberPopupDismissal(): void {
        const key = this.getPopupStorageKey();
        try {
            if (this.frequencyMode === "session") {
                window.sessionStorage.setItem(key, "1");
                return;
            }
            if (this.frequencyMode === "local") {
                const expiresAt = Date.now() + this.frequencyDays * 24 * 60 * 60 * 1000;
                window.localStorage.setItem(key, String(expiresAt));
            }
        } catch (error) {
            console.warn("[newsletter-signup] storage unavailable", error);
        }
    }

    private getPopupStorageKey(): string {
        return `${this.root.id}-dismissed`;
    }

    private parseFrequencyMode(value?: string): PopupFrequencyMode {
        if (value === "session" || value === "local" || value === "none") return value;
        return "session";
    }
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────

function initNewsletterSignup(): void {
    document.querySelectorAll<HTMLElement>('[data-cmp-is="newsletter-signup"]').forEach((el) => {
        if (el.dataset.cmpInitialized === "true") return;
        el.dataset.cmpInitialized = "true";
        new NewsletterSignup(el).init();
    });
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initNewsletterSignup);
} else {
    initNewsletterSignup();
}

export { NewsletterSignup, initNewsletterSignup };
export default NewsletterSignup;
