# Project State

## Session 4 — 2026-04-17

### SCSS Architecture — COMPLETE

ITCSS structure established under `ui.frontend/src/main/webpack/`:

```
site/
├── main.scss                     ← entry; ITCSS @import order
├── abstracts/
│   ├── _tokens.scss              ← Guardian design tokens (colors, type, spacing, z-index, motion)
│   ├── _breakpoints.scss         ← $breakpoints map + respond-to / respond-below / respond-between
│   ├── _functions.scss           ← rem(), fluid()  [uses sass:math]
│   └── _mixins.scss              ← visually-hidden, truncate, line-clamp, card-clickable,
│                                    focus-visible-ring, section-label, full-bleed, divider,
│                                    image-zoom, clearfix
├── base/
│   ├── _reset.scss               ← modern CSS reset (box-sizing, img block, reduced-motion)
│   ├── _root.scss                ← :root CSS custom properties bridged from tokens
│   └── _typography.scss         ← body/headings/links/hr + utility classes (.t-meta .t-label .on-dark)
└── layout/
    ├── _container.scss           ← .l-container (--narrow/--wide/--bleed) + .l-section (--tight/--alt)
    └── _grid.scss                ← .l-grid (--2/3/4col), .l-editorial (55/45), .l-sidebar, .l-section-header
components/
└── content/search/
    └── _searchbar.scss           ← .search-bar BEM block (default, --hero, --compact variants)
```

`main.ts` already imports `./main.scss` — no change needed.
Compiled output → `dist/clientlib-site/site.css` → `ui.apps/.../clientlibs/clientlib-site/` (category: `newspaper.site`).

### Known gaps / next steps
- [ ] Step 2: Rename `searchbar.xml` → `searchbar.html` (functional blocker)
- [ ] Step 3: Search Page Editable Template (`/conf/newspaper/.../templates/search-page/`)
- [ ] Step 4: `SearchPageModel` Sling Model + `search-page` page component HTL (mock data)
- [ ] Step 5: `/content/language-masters/en/search` content node in `ui.content`
- [ ] Future: Populate `_newsitem.scss` and `_newslisting.scss` once rebased on SCSS tokens

### Content path clarification
- CLAUDE.md states `/content/newspaper/language-masters/<lang>/...`
- Actual JCR XML shows `/content/language-masters/en/...` (no `/newspaper/` prefix)
- Using observed JCR path until confirmed otherwise
