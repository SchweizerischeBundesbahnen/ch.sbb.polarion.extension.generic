import ensureSharedStyles from '../../../main/resources/js/modules/ensureSharedStyles.js';
import { expect } from 'chai';
import { JSDOM } from 'jsdom';

describe('ensureSharedStyles', function () {
    let dom;

    const IDS = [
        'generic-control-tokens',
        'generic-checkbox-styles',
        'generic-radios-styles',
        'generic-inputs-styles',
        'generic-searchable-dropdown-styles',
        'generic-buttons-styles',
    ];
    const SD_ID = 'generic-searchable-dropdown-styles';

    beforeEach(function () {
        dom = new JSDOM('<!DOCTYPE html><html lang="en"><head></head><body></body></html>');
        global.window = dom.window;
        global.document = dom.window.document;
    });

    afterEach(function () {
        delete global.window;
        delete global.document;
    });

    it('injects a versioned <link> for each shared stylesheet', function () {
        ensureSharedStyles();
        IDS.forEach(id => {
            const link = document.getElementById(id);
            expect(link, id).to.exist;
            expect(link.tagName).to.equal('LINK');
            expect(link.rel).to.equal('stylesheet');
            expect(link.getAttribute('data-generic-version')).to.not.be.null;
        });
        expect(document.querySelectorAll('link[rel="stylesheet"]').length).to.equal(IDS.length);
        expect(document.getElementById(SD_ID).href).to.contain('searchable-dropdown.css');
    });

    it('is idempotent — a repeat call does not duplicate links', function () {
        ensureSharedStyles();
        ensureSharedStyles();
        expect(document.querySelectorAll('link[rel="stylesheet"]').length).to.equal(IDS.length);
    });

    it('replaces an unversioned copy (versioned beats unversioned)', function () {
        // simulate an older starter.js injection: same id, no data-generic-version
        const stale = document.createElement('link');
        stale.id = SD_ID;
        stale.rel = 'stylesheet';
        stale.href = 'http://old/searchable-dropdown.css';
        document.head.appendChild(stale);

        ensureSharedStyles();

        const links = document.querySelectorAll(`#${SD_ID}`);
        expect(links.length).to.equal(1); // replaced, not stacked
        expect(links[0].getAttribute('data-generic-version')).to.not.be.null; // now versioned
        expect(links[0].href).to.not.contain('old');
    });

    it('keeps a newer versioned copy (never downgrades)', function () {
        const newer = document.createElement('link');
        newer.id = SD_ID;
        newer.rel = 'stylesheet';
        newer.href = 'http://newer/searchable-dropdown.css';
        newer.setAttribute('data-generic-version', '99999999999999'); // far newer than the test build (0)
        document.head.appendChild(newer);

        ensureSharedStyles();

        const link = document.getElementById(SD_ID);
        expect(link.getAttribute('data-generic-version')).to.equal('99999999999999'); // untouched
        expect(link.href).to.contain('newer');
    });
});
