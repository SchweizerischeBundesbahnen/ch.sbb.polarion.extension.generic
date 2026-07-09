import { expect } from 'chai';
import { readFileSync, existsSync } from 'node:fs';
import { resolve } from 'node:path';
import { inlineSvgDataUris } from '../../../../scripts/inline-svg-tokens.mjs';

// Source control-tokens.css + images/ live under src/main/resources; mocha runs with cwd = app.
const CSS_PATH = 'src/main/resources/css/control-tokens.css';
const RES_DIR = 'src/main/resources';

describe('inline-svg-tokens (issue #528 build step)', function () {

    describe('inlineSvgDataUris', function () {
        const fakeSvg = Buffer.from('<svg>#fff</svg>');
        const read = () => fakeSvg;

        it('replaces an inline: placeholder with the base64 data-URI of the referenced SVG', function () {
            const { css, count } = inlineSvgDataUris('--x: url(inline:images/a.svg);', read);
            expect(count).to.equal(1);
            expect(css).to.equal(`--x: url("data:image/svg+xml;base64,${fakeSvg.toString('base64')}");`);
            expect(css).to.not.contain('inline:');
        });

        it('replaces every placeholder and reports the count', function () {
            const { css, count } = inlineSvgDataUris(
                '--a: url(inline:images/a.svg); --b: url(inline:images/b/c.svg);', read);
            expect(count).to.equal(2);
            expect(css).to.not.contain('inline:');
            expect((css.match(/data:image\/svg\+xml;base64,/g) || []).length).to.equal(2);
        });

        it('is idempotent — already-resolved CSS (no placeholders) is returned unchanged with count 0', function () {
            const resolved = '--x: url("data:image/svg+xml;base64,PHN2Zz48L3N2Zz4=");';
            const { css, count } = inlineSvgDataUris(resolved, read);
            expect(count).to.equal(0);
            expect(css).to.equal(resolved);
        });

        it('only rewrites placeholders whose path ends in .svg (leaves prose/examples alone)', function () {
            const input = '/* example: url(inline:<path>) */ --x: url(inline:images/a.svg);';
            const { css, count } = inlineSvgDataUris(input, read);
            expect(count).to.equal(1);
            expect(css).to.contain('url(inline:<path>)'); // the comment example is untouched
            expect(css).to.contain('data:image/svg+xml;base64,'); // the real token is inlined
        });

        it('passes the trimmed placeholder path to the reader', function () {
            const seen = [];
            inlineSvgDataUris('--x: url(inline:images/checkbox/checked.svg);', (p) => {
                seen.push(p);
                return fakeSvg;
            });
            expect(seen).to.deep.equal(['images/checkbox/checked.svg']);
        });

        it('propagates an error from the reader (e.g. a missing SVG) rather than swallowing it', function () {
            const boom = () => { throw new Error('referenced SVG not found: images/missing.svg'); };
            expect(() => inlineSvgDataUris('--x: url(inline:images/missing.svg);', boom))
                .to.throw(/referenced SVG not found/);
        });
    });

    // Guards on the real stylesheet — these keep the single-source invariant from silently regressing.
    describe('control-tokens.css source invariants', function () {
        const css = readFileSync(CSS_PATH, 'utf8');

        it('carries no hand-maintained base64 icon data-URIs (icons live only as .svg files)', function () {
            expect(css).to.not.contain('data:image/svg+xml;base64,');
        });

        it('every inline:*.svg placeholder resolves to an existing file under images/', function () {
            const paths = [...css.matchAll(/url\(inline:([^)]+\.svg)\)/g)].map(m => m[1].trim());
            expect(paths.length, 'expected inline: icon placeholders in control-tokens.css').to.be.greaterThan(0);
            const missing = paths.filter(p => !existsSync(resolve(RES_DIR, p)));
            expect(missing, `missing SVG files for placeholders: ${missing.join(', ')}`).to.be.empty;
        });
    });
});
