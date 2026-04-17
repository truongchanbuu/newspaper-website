# Project Rules for Claude Code

You are working on an AEM 6.5.24 On-Prem project built with the standard Maven Archetype:
- core
- ui.apps
- ui.content
- ui.frontend

_**We cannot run the AEM server for this project and every code must be only logically righ**_

## Operating mode
- Do not assume code is runnable.
- Reason statically from the codebase and official documentation.
- When uncertain, state the assumption explicitly and propose the safest AEM-native approach.

## Required documentation workflow
For any AEM-related change, you must use Context7 MCP first to fetch the latest official documentation before editing code.
Always retrieve docs for:
- Editable Templates
- AEM Core Components proxy patterns
- HTL
- Sling Models
- OSGi services/configuration

Use the retrieved docs as the primary source of truth.

## AEM architecture rules
- Use Editable Templates only.
- Use Configuration-as-Code only.
- Put template / policy / model definitions in JCR XML under ui.content.
- Use Core Components via proxy components whenever possible.
- Use Content Fragments for editorial/news data.
- Keep presentation logic in Sling Models or OSGi services, not in HTL.
- Keep HTL lean, semantic, and component-driven.

## Current Structure of News Pages

- /content/newspaper/language-masters/<lang>/<primaryTag> => This is using for nav and primary tag and the 'en' is the language master page.
- /content/newspaper/language-masters/<lang>/articles/<yyyy>/<mm>/<dd>/<article-name> => This is centralized articles for each lang.
- The primary tag is stored inside the node property of the page

## UI rules
- Match the Guardian-inspired editorial layout from the provided reference.
- Dense, high-information, news-site composition.
- Single accent theme: Guardian Orange.
- Prefer grid-based sections, strong hierarchy, compact cards, and minimal whitespace waste.

## THE GUARDIAN REDESIGN - TECHNICAL DESIGN SPECIFICATION

### 1. DESIGN PHILOSOPHY: EDITORIAL MINIMALISM
- Concept: A modern, digital-first interpretation of traditional newspaper layouts.
- Primary Strategy: High signal-to-noise ratio. Using whitespace and typographic scale instead of borders, shadows, or boxes to define content boundaries.
- Character: Authoritative, clean, sophisticated, and fast-loading.

### 2. COLOR SYSTEM (HEX CODES)

#### A. Neutrals & Canvas
- Canvas Primary: #FFFFFF (White) - The base for all content sections.
- Canvas Secondary: #F7F7F8 (Ghost White) - Used for section backgrounds to provide subtle grouping.
- Divider/Rule: #DCDCDC (Gainsboro) - Used for thin horizontal 1px lines.
- Footer Background: #1A1A1A (Eerie Black) - Deep charcoal to anchor the page.

#### B. Typography
- Heading Primary: #121212 (Charleston Green) - High contrast for headlines.
- Body Copy: #333333 (Jet) - Reduced contrast for comfortable long-form reading.
- Metadata/Captions: #767676 (Gray) - Used for timestamps, authors, and image credits.
- Footer Text: #A1A1A1 (Silver) - Muted gray for legibility on dark backgrounds.

#### C. Functional Accents
- Category/Semantic Red: #C70000 (US Flag Red) - Used for tags (News, World, Sport, etc.) and urgent live indicators.
- Interaction/CTA Blue: #1E40B0 (Royal Blue) - Reserved strictly for "Subscribe" and "Support" buttons.
- Link Hover: #0056B3 (Star Command Blue) - Standard interaction state for text links.

#### D. Atmospheric Header Gradient
This is a multi-stop linear gradient at 135 degrees.
- Stop 1 (Top Left): #E2EBF5 (Periwinkle Blue)
- Stop 2 (Center): #EEE5F0 (Magnolia)
- Stop 3 (Bottom Right): #FDF3F0 (Seashell/Peach)

### 3. TYPOGRAPHY GUIDELINES
- Hierarchy: Stark contrast between headline sizes. Primary headlines should be ~250% larger than body text.
- Headlines: Strong, bold serif or heavy sans-serif with tight letter-spacing (approx -0.02em).
- Labels: All-caps or small-caps in 'Category Red' for immediate domain identification.
- Content Clamping: Article previews should be clamped to 2-3 lines max to maintain vertical rhythm.

### 4. LAYOUT & GRID ARCHITECTURE
- Global Grid: 12-column fluid grid with standard gutters (~24px to 32px).
- Section Rhythms:
    - Feature Sections (Top): Asymmetrical. One large visual (6-8 columns) paired with a vertical list (4 columns).
    - Category Feeds (Middle): Symmetrical 3-column or 4-column "card" rows.
    - Video/Multimedia: Full-width dark background strips to break the vertical white flow.
- Padding: Heavy vertical padding between major sections (~64px to 80px) to prevent cognitive overload.

### 5. COMPONENT LOGIC
- Article Cards: Borderless and shadowless. Images are top-aligned. Headlines follow immediately below.
- Images: Aspect ratios are strictly standardized (16:9 for most, 3:2 for features) to ensure horizontal alignment across rows.
- Navigation: Minimalist top bar with a centered logo. Primary categories are listed in a sub-navigation bar using #333333 text.
- Buttons: Primary CTAs use #1E40B0 background with #FFFFFF text and a slight border-radius (2px-4px) for a "professional" rather than "playful" feel.

### 6. INSTRUCTIONS FOR AI GENERATION
- When generating CSS/SCSS: Map the hex codes to semantic variables (e.g., $color-brand-primary, $bg-main).
- When generating Layouts: Avoid <div> nesting for borders; use margins and paddings to create "invisible" containers.
- Accessibility: Ensure the #767676 metadata color passes AA contrast ratios on #FFFFFF backgrounds by increasing weight if necessary.

## Code style rules
- Preserve existing conventions in the repo.
- Do not rewrite unrelated files.
- Make minimal, targeted changes.
- Before editing, summarize the current repo state and the exact files you intend to touch.
- After each step, stop and wait for confirmation.
- At the end of every response, include a short state snapshot:
    - what changed
    - what remains
    - any assumptions

## Session persistence
Maintain continuity across sessions by keeping these files up to date:
- docs/project-state.md
- docs/decisions.md
- docs/component-map.md
- docs/notes.md

Whenever you finish a step, update the relevant state file content in your response plan before moving on.