// Build step for issue #528: bake the control-icon SVGs into control-tokens.css as self-contained
// base64 data-URIs, so the .svg files under images/ are the single source of truth and no base64 is
// hand-maintained in the CSS.
//
// The source control-tokens.css declares each icon token with an `url(inline:<path>.svg)` placeholder;
// this script (run from app/ via `npm run build:css`, wired into frontend-maven-plugin at
// process-classes) rewrites the copy in target/classes, replacing every placeholder with the base64
// of the referenced .svg. Paths are resolved relative to target/classes (where both css/ and
// images/ have been copied by process-resources), matching the runtime layout in the jar.
//
// Idempotent: only `inline:` placeholders whose path ends in .svg are touched, so re-running on
// already-resolved CSS is a no-op and prose like the `inline:` example in the header comment is left
// alone. Zero dependencies (Node builtins only). Encoding is verbatim base64 — no SVG minification,
// so what ships is exactly the bytes of the .svg file.

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

// Matches real icon placeholders (paths ending in .svg), tolerating optional quotes and whitespace
// inside url(...). This deliberately skips prose (the header comment mentions "inline:" but never
// inside a url(...)).
const PLACEHOLDER = /url\(\s*(['"]?)inline:([^'")]+\.svg)\1\s*\)/g;
// Any inline: left inside a url(...) after replacement — e.g. a placeholder whose path does not end
// in .svg, or one this regex still could not parse — must fail the build rather than ship a broken
// URL that browsers try to fetch (the icon would silently disappear).
const LEFTOVER = /url\(\s*['"]?inline:[^)]*\)/;

/**
 * Replace every `url(inline:<path>.svg)` placeholder in `css` with a base64 data-URI.
 * @param {string} css       stylesheet text
 * @param {(relPath: string) => Buffer|Uint8Array} readSvg  resolves a placeholder path to raw SVG bytes
 * @returns {{ css: string, count: number }} rewritten CSS and how many placeholders were replaced
 * @throws if any inline: placeholder remains inside a url(...) after replacement
 */
export function inlineSvgDataUris(css, readSvg) {
    let count = 0;
    const out = css.replace(PLACEHOLDER, (_match, _quote, rawPath) => {
        const b64 = Buffer.from(readSvg(rawPath.trim())).toString('base64');
        count++;
        return `url("data:image/svg+xml;base64,${b64}")`;
    });
    const leftover = out.match(LEFTOVER);
    if (leftover) {
        throw new Error(`unresolved inline: placeholder would ship (not a *.svg path?): ${leftover[0]}`);
    }
    return { css: out, count };
}

/** CLI: rewrite the given CSS file in place, resolving placeholders against its parent's parent dir. */
export function inlineSvgTokensFile(cssPath) {
    if (!existsSync(cssPath)) {
        throw new Error(`CSS not found: ${cssPath}`);
    }
    // images/ sits next to css/ under target/classes; resolve `inline:` paths against that parent.
    const baseDir = resolve(dirname(cssPath), '..');
    const { css, count } = inlineSvgDataUris(readFileSync(cssPath, 'utf8'), (relPath) => {
        const svgPath = resolve(baseDir, relPath);
        if (!existsSync(svgPath)) {
            throw new Error(`referenced SVG not found: ${relPath} (resolved ${svgPath})`);
        }
        return readFileSync(svgPath);
    });
    if (count === 0) {
        console.log(`[inline-svg-tokens] no inline: placeholders in ${cssPath} (already resolved) — nothing to do`);
    } else {
        writeFileSync(cssPath, css);
        console.log(`[inline-svg-tokens] inlined ${count} SVG icon(s) into ${cssPath}`);
    }
    return count;
}

// Run as a script (not when imported by tests).
if (process.argv[1] && resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
    const cssPath = process.argv[2] || 'target/classes/css/control-tokens.css';
    try {
        inlineSvgTokensFile(cssPath);
    } catch (e) {
        console.error(`[inline-svg-tokens] ${e.message}`);
        process.exit(1);
    }
}
