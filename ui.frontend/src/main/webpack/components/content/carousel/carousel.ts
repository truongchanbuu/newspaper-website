// Newspaper Carousel — behavioural enhancements on top of Core Components carousel/v1.
// Core Components already handles: autoplay, slide switching, keyboard left/right arrows,
// indicators, and ARIA. This file adds: touch swipe + hover/focus pause.

class NewspaperCarousel {
    private static readonly SWIPE_THRESHOLD = 50;  // px horizontal before a swipe fires

    private readonly root: HTMLElement;
    private readonly cmpRoot: HTMLElement | null;
    private readonly prevBtn: HTMLButtonElement | null;
    private readonly nextBtn: HTMLButtonElement | null;

    private touchStartX = 0;
    private touchStartY = 0;
    private isSwiping   = false;

    constructor(root: HTMLElement) {
        this.root    = root;
        this.cmpRoot = root.querySelector<HTMLElement>('[data-cmp-is="carousel"]');
        this.prevBtn = root.querySelector<HTMLButtonElement>('.cmp-carousel__action--previous');
        this.nextBtn = root.querySelector<HTMLButtonElement>('.cmp-carousel__action--next');
    }

    public init(): void {
        if (!this.cmpRoot) return;
        this.bindSwipe();
        this.bindHoverPause();
    }

    // ── Touch swipe ───────────────────────────────────────────────────────────

    private bindSwipe(): void {
        this.cmpRoot!.addEventListener('touchstart', this.onTouchStart, { passive: true });
        this.cmpRoot!.addEventListener('touchmove',  this.onTouchMove,  { passive: true });
        this.cmpRoot!.addEventListener('touchend',   this.onTouchEnd,   { passive: true });
    }

    private readonly onTouchStart = (e: TouchEvent): void => {
        const t = e.touches[0];
        this.touchStartX = t.clientX;
        this.touchStartY = t.clientY;
        this.isSwiping   = true;
    };

    private readonly onTouchMove = (e: TouchEvent): void => {
        if (!this.isSwiping) return;
        // Cancel swipe if vertical scroll is dominant
        const dx = e.touches[0].clientX - this.touchStartX;
        const dy = e.touches[0].clientY - this.touchStartY;
        if (Math.abs(dy) > Math.abs(dx)) this.isSwiping = false;
    };

    private readonly onTouchEnd = (e: TouchEvent): void => {
        if (!this.isSwiping) return;
        this.isSwiping = false;
        const dx = e.changedTouches[0].clientX - this.touchStartX;
        if (Math.abs(dx) < NewspaperCarousel.SWIPE_THRESHOLD) return;
        dx < 0 ? this.nextBtn?.click() : this.prevBtn?.click();
    };

    // ── Hover / focus pause ───────────────────────────────────────────────────
    // Core Components carousel v1 exposes a 'cmp-carousel-autoplay-paused' state
    // by toggling a data attribute; we mirror this with a CSS class so the pause
    // indicator can be styled without touching Core Components internals.

    private bindHoverPause(): void {
        // Only wire up if the carousel has autoplay enabled
        const hasAutoplay = this.cmpRoot?.dataset.cmpCarouselAutoplay === 'true'
            || this.root.dataset.autoplay === 'true';
        if (!hasAutoplay) return;

        this.cmpRoot!.addEventListener('mouseenter', this.pause);
        this.cmpRoot!.addEventListener('mouseleave', this.resume);
        this.cmpRoot!.addEventListener('focusin',    this.pause);
        this.cmpRoot!.addEventListener('focusout',   this.resume);
    }

    private readonly pause = (): void => {
        this.root.classList.add('newspaper-carousel--paused');
        // Signal Core Components to pause via its documented custom event
        this.cmpRoot?.dispatchEvent(new CustomEvent('cmp-carousel-autoplay-pause', { bubbles: true }));
    };

    private readonly resume = (): void => {
        // Only resume if focus is fully outside the carousel
        if (this.cmpRoot?.contains(document.activeElement)) return;
        this.root.classList.remove('newspaper-carousel--paused');
        this.cmpRoot?.dispatchEvent(new CustomEvent('cmp-carousel-autoplay-play', { bubbles: true }));
    };
}

// ── Bootstrap ─────────────────────────────────────────────────────────────────

function initCarousels(): void {
    document.querySelectorAll<HTMLElement>('.newspaper-carousel').forEach(el => {
        if (el.dataset.ncInitialized === 'true') return;
        el.dataset.ncInitialized = 'true';
        new NewspaperCarousel(el).init();
    });
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initCarousels);
} else {
    initCarousels();
}

export { NewspaperCarousel, initCarousels };
export default NewspaperCarousel;
