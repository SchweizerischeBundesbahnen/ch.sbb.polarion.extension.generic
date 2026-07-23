import { expect } from 'chai';
import sinon from 'sinon';
import { JSDOM } from 'jsdom';

// dle-toolbar-starter.js is a plain IIFE that, at load time, reads `top` (the observer/order
// registries) and registers `window.GenericDleToolbarStarter`. So the globals it closes over must
// exist BEFORE the module is evaluated — hence a dynamic import() in before(), after wiring globals.
// `top === window` here models the non-iframe case (self === top). requestAnimationFrame is faked so
// the self-healing re-inject can be driven deterministically instead of waiting on a real frame.
describe('GenericDleToolbarStarter (dle-toolbar-starter.js)', function () {
    let dom, window, document, reg, rafCallbacks;

    // Full Polarion DLE toolbar sub-tree the selectors expect. Flags let each test omit a piece
    // (the toolbar row, the spacer cell, the rich-text area) to hit the early-return branches.
    function dleHtml({ toolbar = true, spacer = true, richText = true } = {}) {
        const spacerCell = spacer ? '<td width="100%"></td>' : '';
        const toolbarRow = toolbar
            ? `<div class="polarion-rte-ToolbarPanelWrapper">
                 <table class="polarion-dle-ToolbarPanel"><tbody>
                   <tr><td class="existing-tool"></td>${spacerCell}</tr>
                 </tbody></table>
               </div>`
            : '';
        const richTextPanel = richText
            ? `<div class="polarion-dle-SplitPanel">first</div>
               <div class="polarion-dle-SplitPanel">
                 <div class="rta-wrapper"><div class="rta-inner">
                   <div class="polarion-dle-RichTextArea"></div>
                 </div></div>
               </div>`
            : '';
        return `<div class="polarion-content-container"><div class="polarion-Container">
                  <div class="polarion-dle-Container"><div class="polarion-dle-Wrapper">
                    <div class="polarion-dle-RpcPanel"><div class="polarion-dle-MainDockPanel">
                      ${toolbarRow}${richTextPanel}
                    </div></div>
                  </div></div>
                </div></div>`;
    }

    // Rich Page (Live Report) sub-tree: the preview toolbar row plus the view/edit content marker
    // and, optionally, the collapsed-state "Expand Tools" handle.
    function rpeHtml({ toolbar = true, view = true, spacer = true, expandHandle = false, handleHidden = false } = {}) {
        const spacerCell = spacer ? '<td width="100%"></td>' : '';
        const toolbarRow = toolbar
            ? `<div class="polarion-rte-ToolbarPanelWrapper">
                 <table class="polarion-dle-ToolbarPanel"><tbody>
                   <tr><td class="existing-tool"></td>${spacerCell}</tr>
                 </tbody></table>
               </div>`
            : '';
        const content = view ? '<div class="polarion-rpe-view"></div>' : '<div class="polarion-rpe-edit"></div>';
        const handle = expandHandle
            ? `<div class="polarion-rpe-expandTools"${handleHidden ? ' style="display: none;"' : ''}><span>Expand Tools</span></div>`
            : '';
        return `<div class="polarion-content-container">
                  <div class="polarion-rpe-MainPanel">${toolbarRow}${content}${handle}</div>
                </div>`;
    }

    const cfg = (over = {}) => ({ markerId: 'my-btn', alternateHtml: '<button>A</button>', defaultHtml: '<button>D</button>', ...over });

    before(async function () {
        dom = new JSDOM('<!DOCTYPE html><html lang="en"><head></head><body></body></html>', { url: 'http://localhost/' });
        window = dom.window;
        document = window.document;
        global.window = window;
        global.top = window;                 // module reads bare `top`; self === top (not in an iframe)
        global.document = document;
        global.MutationObserver = window.MutationObserver;
        rafCallbacks = [];
        global.requestAnimationFrame = (cb) => rafCallbacks.push(cb);
        await import('../../../main/resources/js/dle-toolbar-starter.js');
        reg = window.__genericDleToolbarObservers; // the closed-over registry (same object reference)
    });

    after(function () {
        delete global.window;
        delete global.top;
        delete global.document;
        delete global.MutationObserver;
        delete global.requestAnimationFrame;
    });

    afterEach(function () {
        // Clear the closed-over registries by key (never reassign — the module captured the object).
        Object.keys(reg).forEach((k) => { try { reg[k].disconnect(); } catch { /* node gone */ } delete reg[k]; });
        const order = window.__genericDleToolbarOrder;
        if (order) Object.keys(order).forEach((k) => delete order[k]);
        // Release the shared auto-expand observer so each test starts from a clean slate.
        if (window.__genericRpeAutoExpandObserver) {
            window.__genericRpeAutoExpandObserver.disconnect();
            delete window.__genericRpeAutoExpandObserver;
        }
        document.head.innerHTML = '';
        document.body.innerHTML = '';
        rafCallbacks.length = 0;
        sinon.restore();
    });

    const flushObserver = () => new Promise((resolve) => setTimeout(resolve, 0));

    it('injects the alternate button as a <td> before the spacer cell', function () {
        document.body.innerHTML = dleHtml();
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });

        const cell = document.getElementById('my-btn');
        expect(cell).to.exist;
        expect(cell.tagName).to.equal('TD');
        expect(cell.innerHTML).to.equal('<button>A</button>');
        expect(cell.nextElementSibling.getAttribute('width')).to.equal('100%'); // sits before the spacer
    });

    it('is idempotent — a second inject with the button present is a no-op', function () {
        document.body.innerHTML = dleHtml();
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });
        starter.injectToolbar({ alternate: true });
        expect(document.querySelectorAll('#my-btn').length).to.equal(1);
    });

    it('does nothing in alternate mode when the toolbar row is not rendered yet', function () {
        document.body.innerHTML = dleHtml({ toolbar: false });
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });
        expect(document.getElementById('my-btn')).to.equal(null);
    });

    it('appends at the end and warns when the spacer cell is missing', function () {
        document.body.innerHTML = dleHtml({ spacer: false });
        const warn = sinon.stub(console, 'warn');
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });

        const row = document.querySelector('table.polarion-dle-ToolbarPanel tr');
        expect(row.lastElementChild.id).to.equal('my-btn'); // appended at the end
        expect(warn.calledOnce).to.be.true;
        expect(warn.firstCall.args[0]).to.contain('my-btn');
    });

    it('keeps a stable order — a lower-order button is inserted before a higher-order one', function () {
        document.body.innerHTML = dleHtml();
        window.GenericDleToolbarStarter.create(cfg({ markerId: 'btn-high', order: 10 })).injectToolbar({ alternate: true });
        window.GenericDleToolbarStarter.create(cfg({ markerId: 'btn-low', order: 0 })).injectToolbar({ alternate: true });

        const ids = [...document.querySelector('table.polarion-dle-ToolbarPanel tr').children].map((c) => c.id);
        expect(ids.indexOf('btn-low')).to.be.lessThan(ids.indexOf('btn-high')); // low sits before high
    });

    it('injects the default button as a div above the rich-text area', function () {
        document.body.innerHTML = dleHtml();
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({}); // not alternate → default path

        const box = document.getElementById('my-btn');
        expect(box.tagName).to.equal('DIV');
        expect(box.classList.contains('dleToolBarContainer')).to.be.true;
        expect(box.style.marginRight).to.equal('14px');
        // prepended into richTextArea.parentNode.parentNode (the .rta-wrapper)
        expect(box.parentElement.classList.contains('rta-wrapper')).to.be.true;
    });

    it('does nothing in default mode when the rich-text area is absent', function () {
        document.body.innerHTML = dleHtml({ richText: false });
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({});
        expect(document.getElementById('my-btn')).to.equal(null);
    });

    it('re-injects the button when the toolbar re-renders (self-healing observer)', async function () {
        document.body.innerHTML = dleHtml();
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });
        expect(document.getElementById('my-btn')).to.exist;

        document.getElementById('my-btn').remove();                      // GWT wipes the button
        document.querySelector('div.polarion-dle-Container').appendChild(document.createElement('span')); // re-render mutation
        await flushObserver();

        expect(rafCallbacks.length).to.be.greaterThan(0);                // a coalesced re-inject was queued
        rafCallbacks.forEach((cb) => cb());
        expect(document.getElementById('my-btn')).to.exist;              // healed
    });

    it('the observer stays idle while the button is still present', async function () {
        document.body.innerHTML = dleHtml();
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });

        document.querySelector('div.polarion-dle-Container').appendChild(document.createElement('span'));
        await flushObserver();
        expect(rafCallbacks.length).to.equal(0); // fast-path: button present → nothing scheduled
    });

    it('falls back to observing document.body when the stable ancestor is absent', async function () {
        document.body.innerHTML = ''; // no toolbar, no ancestor
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true }); // inject early-returns, observer still set up on body
        expect(reg['my-btn']).to.exist;

        document.body.appendChild(document.createElement('span'));
        await flushObserver();
        expect(rafCallbacks.length).to.be.greaterThan(0); // observing body works
    });

    it('destroy() disconnects the observer and drops it from the registry', function () {
        document.body.innerHTML = dleHtml();
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });
        expect(reg['my-btn']).to.exist;

        starter.destroy();
        expect(reg['my-btn']).to.be.undefined;
        starter.destroy(); // second destroy with nothing registered is harmless
    });

    it('re-installs the observer after destroy (observerSetUp resets)', function () {
        document.body.innerHTML = dleHtml();
        const starter = window.GenericDleToolbarStarter.create(cfg());
        starter.injectToolbar({ alternate: true });
        starter.destroy();
        starter.injectToolbar({ alternate: true }); // sets a fresh observer up again
        expect(reg['my-btn']).to.exist;
    });

    it('disconnects a leftover observer when the same markerId is re-created', function () {
        document.body.innerHTML = dleHtml();
        const first = window.GenericDleToolbarStarter.create(cfg());
        first.injectToolbar({ alternate: true });
        const firstObserver = reg['my-btn'];
        const spy = sinon.spy(firstObserver, 'disconnect');

        const second = window.GenericDleToolbarStarter.create(cfg());
        second.injectToolbar({ alternate: true });
        expect(spy.calledOnce).to.be.true;           // the previous observer was disconnected
        expect(reg['my-btn']).to.not.equal(firstObserver); // registry now holds the new one
    });

    it("throws on an unknown target", function () {
        expect(() => window.GenericDleToolbarStarter.create(cfg({ target: 'nope' }))).to.throw("unknown target 'nope'");
    });

    describe("richPagePreview target", function () {
        it('injects the button into the Rich Page toolbar row before the spacer cell', function () {
            document.body.innerHTML = rpeHtml();
            const starter = window.GenericDleToolbarStarter.create(cfg({ target: 'richPagePreview' }));
            starter.injectToolbar(); // no alternate flag — row injection is implied for this target

            const cell = document.getElementById('my-btn');
            expect(cell).to.exist;
            expect(cell.tagName).to.equal('TD');
            expect(cell.innerHTML).to.equal('<button>A</button>');
            expect(cell.nextElementSibling.getAttribute('width')).to.equal('100%'); // sits before the spacer
        });

        it('does not inject while the Rich Page is in edit mode (guard selector)', function () {
            document.body.innerHTML = rpeHtml({ view: false });
            const starter = window.GenericDleToolbarStarter.create(cfg({ target: 'richPagePreview' }));
            starter.injectToolbar();
            expect(document.getElementById('my-btn')).to.equal(null);
        });

        it('does nothing while the toolbar is still collapsed (row absent)', function () {
            document.body.innerHTML = rpeHtml({ toolbar: false, expandHandle: true });
            const starter = window.GenericDleToolbarStarter.create(cfg({ target: 'richPagePreview' }));
            starter.injectToolbar();
            expect(document.getElementById('my-btn')).to.equal(null);
        });

        it('injects via the observer once the toolbar gets expanded', async function () {
            document.body.innerHTML = rpeHtml({ toolbar: false, expandHandle: true });
            const starter = window.GenericDleToolbarStarter.create(cfg({ target: 'richPagePreview' }));
            starter.injectToolbar(); // nothing yet — row absent, but the observer is armed

            // "Expand Tools" clicked: GWT replaces the handle with the toolbar
            const panel = document.querySelector('.polarion-rpe-MainPanel');
            panel.querySelector('.polarion-rpe-expandTools').remove();
            panel.insertAdjacentHTML('afterbegin',
                `<div class="polarion-rte-ToolbarPanelWrapper">
                   <table class="polarion-dle-ToolbarPanel"><tbody>
                     <tr><td class="existing-tool"></td><td width="100%"></td></tr>
                   </tbody></table>
                 </div>`);
            await flushObserver();
            expect(rafCallbacks.length).to.be.greaterThan(0);
            rafCallbacks.forEach((cb) => cb());
            expect(document.getElementById('my-btn')).to.exist;
        });
    });

    describe('autoExpandRichPageTools', function () {
        it('clicks a visible "Expand Tools" handle on the initial call', function () {
            document.body.innerHTML = rpeHtml({ toolbar: false, expandHandle: true });
            const handle = document.querySelector('.polarion-rpe-expandTools');
            const clicked = sinon.spy();
            handle.addEventListener('click', clicked);

            window.GenericDleToolbarStarter.autoExpandRichPageTools();
            expect(clicked.calledOnce).to.be.true;
        });

        it('ignores a handle hidden by GWT (inline display:none)', function () {
            document.body.innerHTML = rpeHtml({ toolbar: false, expandHandle: true, handleHidden: true });
            const clicked = sinon.spy();
            document.querySelector('.polarion-rpe-expandTools').addEventListener('click', clicked);

            window.GenericDleToolbarStarter.autoExpandRichPageTools();
            expect(clicked.called).to.be.false;
        });

        it('clicks the handle when it appears later (SPA navigation)', async function () {
            document.body.innerHTML = '';
            window.GenericDleToolbarStarter.autoExpandRichPageTools();

            document.body.innerHTML = rpeHtml({ toolbar: false, expandHandle: true });
            const clicked = sinon.spy();
            document.querySelector('.polarion-rpe-expandTools').addEventListener('click', clicked);
            await flushObserver();
            expect(rafCallbacks.length).to.be.greaterThan(0);
            rafCallbacks.forEach((cb) => cb());
            expect(clicked.calledOnce).to.be.true;
        });

        it('is idempotent — repeated calls keep a single shared observer', function () {
            document.body.innerHTML = '';
            window.GenericDleToolbarStarter.autoExpandRichPageTools();
            const observer = window.__genericRpeAutoExpandObserver;
            window.GenericDleToolbarStarter.autoExpandRichPageTools();
            expect(window.__genericRpeAutoExpandObserver).to.equal(observer);
        });
    });

    it('injectStyles adds a stylesheet link once and injectScript adds a script once', function () {
        window.GenericDleToolbarStarter.injectStyles('sbb-css', '/x.css');
        window.GenericDleToolbarStarter.injectStyles('sbb-css', '/x.css'); // idempotent
        const link = document.getElementById('sbb-css');
        expect(link.tagName).to.equal('LINK');
        expect(link.rel).to.equal('stylesheet');
        expect(link.href).to.contain('/x.css');
        expect(document.querySelectorAll('#sbb-css').length).to.equal(1);

        window.GenericDleToolbarStarter.injectScript('sbb-js', '/x.js');
        window.GenericDleToolbarStarter.injectScript('sbb-js', '/x.js'); // idempotent
        const script = document.getElementById('sbb-js');
        expect(script.tagName).to.equal('SCRIPT');
        expect(script.getAttribute('type')).to.equal('text/javascript'); // default type
        expect(script.getAttribute('src')).to.equal('/x.js');
        expect(document.querySelectorAll('#sbb-js').length).to.equal(1);
    });
});
