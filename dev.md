# Newspaper AEM — Dev Log

Design reference: [The Guardian — Behance](https://www.behance.net/gallery/223488419/The-Guardian)

---

## Content Structure
- Articles: `/content/lang-master/articles/YYYY/MM/DD/<article-name>`
  - `jcr:content` props: `jcr:title`, `description`, `section` (slug), `author`, `articleDate` (Calendar), `featuredImage` (DAM path), `featuredImageAlt`
- Sections: `/content/lang-master/en/<section>/` (e.g. tech, business, sport, culture, politics, environment, lifestyle)

---

## Session 1 — 2026-04-15

### Components built
| Component | Type | Path |
|-----------|------|------|
| `newsitem` | content | `ui.apps/.../components/content/newsitem` |
| `newslisting` | content | `ui.apps/.../components/content/newslisting` |
| `homepage` | structure | `ui.apps/.../components/structure/homepage` |

### `newsitem`
- Standalone card component. Authors fill title, trail text, link, section, variant, image, author, date via `_cq_dialog`.
- Sling Model: `NewsItemModel` — formats `articleDate` Calendar → `d MMM yyyy` / `yyyy-MM-dd`.
- Variants: `featured` (hero — full image 16:9, large headline, trail text visible) and `standard` (compact, trail hidden, image 4:3).
- Section label colored via CSS custom properties per slug.
- Clientlib category: `newspaper.newsitem`

### `newslisting`
- Section block. Dialog: `sectionTitle`, `section` (select), `articlesRootPath` (default `/content/lang-master/articles`), `maxItems` (default 7), `showSectionLink`.
- Sling Model: `NewsListingModel` — JCR SQL2 query on `[cq:PageContent]` filtered by `section` property, ordered by `articleDate DESC`.
- Returns: `getFeaturedArticle()` (first result) + `getSecondaryArticles()` (rest).
- Layout: section header bar (colored top border) + featured (55%) | secondary grid (45%) via CSS Grid. Responsive — single col on mobile.
- Shows authoring placeholder when no articles found.
- Clientlib category: `newspaper.newslisting` (depends on `newspaper.newsitem`)

### `homepage` (page component)
- Renders full HTML page. Includes header/footer via `data-sly-include` (those components are built by another team).
- Two parsys zones: `hero` (above fold) and `main` (stacked News Listing sections).
- Clientlib category: `newspaper.homepage` (depends on `newspaper.newslisting`)

### Clientlib dependency chain
```
newspaper.base
  └── newspaper.newsitem
        └── newspaper.newslisting
              └── newspaper.homepage
```
> `newspaper.base` — not created yet, assumed to exist (global resets, typography tokens).

---

## Session 2 — 2026-04-16

### Goal
- `cq:tags` as the canonical category mechanism; section = AEM page at `/content/lang-master/en/<slug>`
- News listing: data-driven cards (JCR query by `cq:tags`), all cards standard variant, author-configured thumb position and layout (stack / grid / horizontal)
- Each card fully clickable (CSS overlay pattern)
- Cards display: thumb + section tag (coloured, links to section page) + headline + summary (optional) + timeAgo

### Changes

#### Tag / category convention
- Tag namespace: `newspaper`, structure: `section/<slug>` → full ID e.g. `newspaper:section/tech`
- Tag corresponds to the AEM section page at `/content/lang-master/en/<slug>`
- Articles: `cq:tags = ["newspaper:section/tech"]` on `jcr:content`

#### `NewsItemModel` — reworked category handling
- Reads `cq:tags` (`String[]`) as primary category source
- `slugFromTagId("newspaper:section/tech")` → `"tech"` (package-private static, shared with NewsListingModel)
- Computes `section` slug, `sectionDisplay` label, `sectionLink` (`/content/lang-master/en/tech.html`)
- `thumbPosition` authored in dialog (top/bottom/left/right, default top)
- `timeAgo` computed (e.g. `3h ago`); null > 30 days → HTL falls back to `formattedDate`
- `computeTimeAgo(Calendar)` package-private static, reused by NewsListingModel

#### `NewsListingModel` — reworked
- `categoryTag` replaces `section` in dialog; stores full tag ID e.g. `newspaper:section/tech`
- Derives `section` slug via `slugFromTagId()`
- JCR SQL2 query changed: `content.[cq:tags] = 'newspaper:section/tech'` (multi-value equality)
- `ArticleItem.section` derived from article's own `cq:tags[0]`, fallback to listing's section
- Removed `getFeaturedArticle()` / `getSecondaryArticles()` → unified `getArticles()`
- New authored fields: `listingLayout` (stack/horizontal/grid), `thumbPosition`, `gridCols` (2/3/4)

#### `newsitem/_cq_dialog` — updated
- `section` select → `category` (name `./cq:tags`), values are tag IDs (`newspaper:section/tech`, …)
- `thumbPosition` select: Top / Bottom / Left / Right
- `description` field label updated to "Summary"

#### `newslisting/_cq_dialog` — rewritten (3 tabs)
- **Content tab**: `categoryTag` select (tag IDs), `sectionTitle` override, `maxItems`, `showSectionLink`
- **Layout tab**: `listingLayout` (Stack/Grid/Horizontal), `gridCols` (2/3/4), `thumbPosition` (all cards)
- **Advanced tab**: `articlesRootPath`

#### `newsitem/templates.html` — new file
- `data-sly-template.card` — params: `article`, `variant`, `thumbPosition`, `sectionLabel`, `sectionLink`
- Used by `newslisting` via `data-sly-use` + `data-sly-call`; eliminates duplicated markup

#### `newsitem/newsitem.html` — updated
- Uses `newsitem.sectionLink` (from model) instead of hardcoded path
- `news-item--thumb-${thumbPosition}` class
- Card-clickable: image `<a>` wrapper removed — the headline `::after` overlay covers the card
- timeAgo / formattedDate fallback

#### `newslisting/newslisting.html` — rewritten
- Single article loop for all layouts (`listing.articles`)
- Passes `listing.thumbPosition` to template (author-configured per listing)
- Layout/section/cols classes on outer `<section>`

#### `newsitem/clientlibs/css/newsitem.css` — updated
- **Card-clickable**: `.news-item__headline-link::after { inset: 0 }` overlays card; section label / meta sit on `z-index: 1`
- Thumb position modifiers: `--thumb-bottom`, `--thumb-left`, `--thumb-right`
- `min-width: 0` on `__body` prevents overflow in narrow flex/grid cells
- `.news-item__time-ago` style
- Mobile: left/right variants collapse to column below 480 px

#### `newslisting/clientlibs/css/newslisting.css` — rewritten
- **Stack** layout: `flex-direction: column`
- **Grid** layout: responsive, `grid-template-columns` stepped by breakpoint per `--cols-N` modifier
  - 2-col: 2 from 600 px up
  - 3-col: 1→2 (600 px)→3 (980 px)
  - 4-col: 1→2 (600 px)→4 (980 px)
  - Forced single-col below 480 px
- **Horizontal** layout: `overflow-x: auto`, `scroll-snap-type: x mandatory`, fixed card widths (220→280→320 px across breakpoints), thin scrollbar

---

---

## Session 3 — 2026-04-16

### Goal
Production-level SCSS architecture inside `ui.frontend` — organised, token-driven, mixin-powered.

### SCSS structure (`ui.frontend/src/main/webpack/`)

```
site/
├── main.scss                        ← entry; ITCSS import order
├── abstracts/
│   ├── _tokens.scss                 ← all design tokens (colours, type, spacing, motion, z-index)
│   ├── _breakpoints.scss            ← $breakpoints map + respond-to/respond-below/respond-between
│   ├── _functions.scss              ← section-color(), section-tint(), rem()
│   └── _mixins.scss                 ← visually-hidden, truncate, line-clamp, section-border-modifiers,
│                                       card-clickable, focus-visible-ring, full-bleed, clearfix
├── base/
│   ├── _reset.scss                  ← modern CSS reset (box-sizing, no margin, img block, reduced-motion)
│   ├── _root.scss                   ← :root CSS custom properties bridged from tokens
│   └── _typography.scss             ← body/headings/links/hr base + dark mode fallback
└── layout/
    ├── _container.scss              ← .l-container, --narrow, --wide, --bleed
    └── _grid.scss                   ← .l-editorial (55/45), .l-grid (2/3/4-col), .l-sidebar, .l-masonry
components/
├── _newsitem.scss                   ← full newsitem BEM block (replaces ui.apps clientlib CSS)
└── _newslisting.scss                ← full newslisting BEM block (stack/grid/horizontal layouts)
```

### Key design decisions

| Decision | Rationale |
|---|---|
| ITCSS import order | Abstracts (no output) → Base → Layout → Components → Site; tokens available to all component SCSS |
| `$sections` map + `@each` | Section colour modifiers generated automatically; no manual per-section CSS |
| `respond-to()` / `respond-below()` mixins | Named breakpoints prevent magic numbers; mobile-first default |
| `section-border-modifiers` mixin | Single call generates all header accent colours from the map |
| `card-clickable` mixin | Documents the `::after` overlay pattern; reusable for any future card types |
| `focus-visible-ring` mixin | Consistent keyboard focus (Guardian yellow) without mouse interference |
| Backward-compat aliases | `$color-foreground`, `$color-background`, `$color-link` aliases kept so `experiencefragment_header.scss` compiles unchanged |
| Old flat files emptied | `_variables.scss`, `_base.scss`, `styles/_variables.scss` now just comments; no duplicate declarations |

### Note on ui.apps clientlib CSS
`ui.apps/.../newsitem/clientlibs/css/newsitem.css` and `newslisting.css` are now superseded by the compiled `clientlib-site` output from `ui.frontend`. Those files should be emptied on next cleanup session to avoid specificity conflicts when both clientlibs are loaded.

---

## Known gaps / next sessions
- [ ] Article detail page template + component
- [ ] Hero component (large top-of-page feature story, full-bleed image)
- [ ] `newspaper.base` clientlib (CSS variables, font-face, global reset)
- [ ] `content-policy` / editable template config in `ui.content` (allow `newsitem`, `newslisting` in parsys)
- [ ] `ui.content` sample content for section pages
- [ ] Test query — add `section` property to a few article pages and verify listing renders
- [ ] Confirm header/footer component paths with the other team (currently assumed `/apps/newspaper/components/structure/header|footer`)
