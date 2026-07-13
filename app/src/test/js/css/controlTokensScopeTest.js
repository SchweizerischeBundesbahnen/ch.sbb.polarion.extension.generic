import { expect } from 'chai';
import { readFileSync } from 'node:fs';

// mocha runs with cwd = app.
const css = readFileSync('src/main/resources/css/control-tokens.css', 'utf8');

describe('control-tokens.css token scoping', function () {

    it('declares the --sbb-* tokens on the shared UI scopes only (no :root)', function () {
        // The token block must open on every scope wrapper so that on a page where several extensions
        // load their own bundled generic at different versions, each extension's controls read the
        // tokens from the closest scoped ancestor (its own bundle) rather than a foreign declaration
        // that happened to load last.
        expect(css).to.match(
            /\.modal__container,\s*\.standard-admin-page,\s*\.form-wrapper,\s*\.sbb-ui\s*\{/
        );
    });

    it('does NOT declare the tokens on :root (issue #535 — a global :root re-enables clobbering)', function () {
        // Guard against reintroducing `:root,` (or a bare `:root {`) on the token block: a :root
        // declaration is global, so any control outside a scope wrapper would again read whichever
        // extension's tokens loaded last on the shared document page.
        expect(css).to.not.match(/:root\s*,\s*\.modal__container/);
        expect(css).to.not.match(/:root\s*\{[\s\S]*?--sbb-/);
    });

    it('actually defines tokens inside that scoped block', function () {
        const idx = css.search(/\.modal__container,\s*\.standard-admin-page,[\s\S]*?\.sbb-ui\s*\{/);
        expect(idx, 'scoped token block').to.be.greaterThan(-1);
        // A representative token appears right after the scope selector opens the block.
        expect(css.slice(idx)).to.contain('--sbb-control-height');
    });
});
