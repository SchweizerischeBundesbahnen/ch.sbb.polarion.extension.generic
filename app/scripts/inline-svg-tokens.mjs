// Build step for issue #528: bake the control-icon SVGs into control-tokens.css as self-contained
// base64 data-URIs, so the .svg files under images/ are the single source of truth and no base64 is
// hand-maintained in the CSS.
//
// The source control-tokens.css declares each icon token with an `url(inline:<path>)` placeholder;
// this script (run from app/ via `npm run build:css`, wired into frontend-maven-plugin at
// process-classes) rewrites the copy in target/classes, replacing every placeholder with the base64
// of the referenced .svg. Paths are resolved relative to target/classes (where both css/ and
// images/ have been copied by process-resources), matching the runtime layout in the jar.
//
// Idempotent: only `inline:` placeholders are touched, so re-running on already-resolved CSS is a
// no-op. Zero dependencies (Node builtins only). Encoding is verbatim base64 — no SVG minification,
// so what ships is exactly the bytes of the .svg file.

import { readFileSync, writeFileSync, existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const cssPath = process.argv[2] || 'target/classes/css/control-tokens.css';

if (!existsSync(cssPath)) {
    console.error(`[inline-svg-tokens] CSS not found: ${cssPath}`);
    process.exit(1);
}

// images/ sits next to css/ under target/classes; resolve `inline:` paths against that parent.
const baseDir = resolve(dirname(cssPath), '..');
const css = readFileSync(cssPath, 'utf8');

// Only real icon placeholders (paths ending in .svg) are rewritten — this deliberately skips prose
// like the `url(inline:<path>)` example in the file header comment.
let count = 0;
const out = css.replace(/url\(inline:([^)]+\.svg)\)/g, (_match, rawPath) => {
    const svgPath = resolve(baseDir, rawPath.trim());
    if (!existsSync(svgPath)) {
        console.error(`[inline-svg-tokens] referenced SVG not found: ${rawPath} (resolved ${svgPath})`);
        process.exit(1);
    }
    const b64 = readFileSync(svgPath).toString('base64');
    count++;
    return `url("data:image/svg+xml;base64,${b64}")`;
});

if (count === 0) {
    console.log(`[inline-svg-tokens] no inline: placeholders in ${cssPath} (already resolved) — nothing to do`);
} else {
    writeFileSync(cssPath, out);
    console.log(`[inline-svg-tokens] inlined ${count} SVG icon(s) into ${cssPath}`);
}
