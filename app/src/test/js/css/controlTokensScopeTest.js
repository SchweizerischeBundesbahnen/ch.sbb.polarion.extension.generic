import { expect } from 'chai';
import { readFileSync } from 'node:fs';

// mocha runs with cwd = app.
const css = readFileSync('src/main/resources/css/control-tokens.css', 'utf8');

describe('control-tokens.css token scoping', function () {

    it('declares the --sbb-* tokens on the shared UI scopes, not only :root', function () {
        // The token block must open on every scope wrapper so that on a page where several extensions
        // load their own bundled generic at different versions, each extension's controls read the
        // tokens from the closest scoped ancestor (its own bundle) rather than a foreign :root that
        // happened to load last. Regressing this to `:root {` alone reintroduces the clobbering.
        expect(css).to.match(
            /:root,\s*\.modal__container,\s*\.standard-admin-page,\s*\.form-wrapper,\s*\.sbb-ui\s*\{/
        );
    });

    it('actually defines tokens inside that scoped block', function () {
        const idx = css.search(/:root,\s*\.modal__container,[\s\S]*?\.sbb-ui\s*\{/);
        expect(idx, 'scoped token block').to.be.greaterThan(-1);
        // A representative token appears right after the scope selector opens the block.
        expect(css.slice(idx)).to.contain('--sbb-control-height');
    });
});
