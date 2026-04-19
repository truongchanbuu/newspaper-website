class NewspaperCarousel {
    private static readonly SWIPE_THRESHOLD = 48;

    private readonly root: HTMLElement;
    private readonly cmpRoot: HTMLElement | null;
    private readonly prevBtn: HTMLButtonElement | null;
    private readonly nextBtn: HTMLButtonElement | null;
    private readonly pauseBtn: HTMLButtonElement | null;

    private touchStartX  = 0;
    private touchStartY  = 0;
    private swipeBlocked = false;

    constructor(root: HTMLElement) {
        this.root       = root;
        this.cmpRoot    = root.querySelector<HTMLElement>('[data-cmp-is="carousel"]');
        this.prevBtn    = root.querySelector<HTMLButtonElement>('.cmp-carousel__action--previous');
        this.nextBtn    = root.querySelector<HTMLButtonElement>('.cmp-carousel__action--next');
        this.pauseBtn   = root.querySelector<HTMLButtonElement>('.cmp-carousel__action--pause');
    }

    public init(): void {
        if (!this.cmpRoot) return;
        this.bindSwipe();
        this.bindHoverPause();
    }

    private bindSwipe(): void {
        this.cmpRoot!.addEventListener('touchstart', this.onTouchStart, { passive: true });
        this.cmpRoot!.addEventListener('touchmove',  this.onTouchMove,  { passive: true });
        this.cmpRoot!.addEventListener('touchend',   this.onTouchEnd,   { passive: true });
    }

    private readonly onTouchStart = (e: TouchEvent): void => {
        const t = e.touches[0];
        this.touchStartX  = t.clientX;
        this.touchStartY  = t.clientY;
        this.swipeBlocked = false;
    };

    private readonly onTouchMove = (e: TouchEvent): void => {
        if (this.swipeBlocked) return;
        const dx = e.touches[0].clientX - this.touchStartX;
        const dy = e.touches[0].clientY - this.touchStartY;
        if (Math.abs(dy) > Math.abs(dx) * 1.2) this.swipeBlocked = true;
    };

    private readonly onTouchEnd = (e: TouchEvent): void => {
        if (this.swipeBlocked) return;
        const dx = e.changedTouches[0].clientX - this.touchStartX;
        if (Math.abs(dx) < NewspaperCarousel.SWIPE_THRESHOLD) return;
        dx < 0 ? this.nextBtn?.click() : this.prevBtn?.click();
    };

    private bindHoverPause(): void {
        if (!this.pauseBtn) return;

        this.cmpRoot!.addEventListener('mouseenter', this.pause);
        this.cmpRoot!.addEventListener('mouseleave', this.tryResume);
        this.cmpRoot!.addEventListener('focusin',    this.pause);
        this.cmpRoot!.addEventListener('focusout',   this.tryResume);
    }

    private readonly pause = (): void => {
        if (this.root.dataset.ncHoverPaused) return;
        this.root.dataset.ncHoverPaused = 'true';
        this.pauseBtn?.click();
    };

    private readonly tryResume = (): void => {
        requestAnimationFrame(() => {
            if (this.root.contains(document.activeElement)) return;
            if (!this.root.dataset.ncHoverPaused) return;
            delete this.root.dataset.ncHoverPaused;
            this.pauseBtn?.click();
        });
    };
}

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
