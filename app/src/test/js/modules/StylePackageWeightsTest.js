import StylePackageWeights from '../../../main/resources/js/modules/StylePackageWeights.js';
import { expect } from 'chai';
import { JSDOM } from 'jsdom';

describe('StylePackageWeights', function () {
    let dom, document, ctx;

    beforeEach(function () {
        dom = new JSDOM('<!DOCTYPE html><html lang="en"><body><ul id="weights-list"></ul></body></html>');
        global.window = dom.window;
        global.document = dom.window.document;
        global.Event = dom.window.Event;
        document = dom.window.document;
        ctx = {
            extension: 'test-exporter',
            scope: '',
            getElementById: (id) => document.getElementById(id)
        };
    });

    afterEach(function () {
        delete global.window;
        delete global.document;
        delete global.Event;
    });

    function component(items = []) {
        const instance = new StylePackageWeights({ ctx, listId: 'weights-list', bindToolbar: false, autoLoad: false });
        instance.items = items.map(i => ({
            name: i.name,
            scope: i.scope ?? '',
            weight: i.weight,
            originalWeight: i.originalWeight ?? i.weight,
            static: !!i.static
        }));
        return instance;
    }

    function names(instance) {
        return instance.items.map(i => i.name);
    }
    function weightOf(instance, name) {
        return instance.items.find(i => i.name === name).weight;
    }

    describe('adjustWeight', function () {
        it('clamps values above 100 down to 100', function () {
            const input = { value: '150' };
            StylePackageWeights.adjustWeight(input);
            expect(input.value).to.equal(100);
        });

        it('clamps negative values up to 0 (not the 50 fallback)', function () {
            const input = { value: '-5' };
            StylePackageWeights.adjustWeight(input);
            expect(input.value).to.equal(0);
        });

        it('rounds to one decimal', function () {
            const input = { value: '42.37' };
            StylePackageWeights.adjustWeight(input);
            expect(input.value).to.equal(42.4);
        });

        it('falls back to 50 for non-numeric input', function () {
            const input = { value: 'abc' };
            StylePackageWeights.adjustWeight(input);
            expect(input.value).to.equal(50);
        });

        it('leaves a valid value untouched', function () {
            const input = { value: '55' };
            StylePackageWeights.adjustWeight(input);
            expect(input.value).to.equal(55);
        });
    });

    describe('placeAt / computeWeightForPosition', function () {
        // Sorted list: A 90, B 75, C 60, G 42 (static global), M 20.
        function sortedList() {
            return component([
                { name: 'A', weight: 90 },
                { name: 'B', weight: 75 },
                { name: 'C', weight: 60 },
                { name: 'G', weight: 42, scope: '', static: true },
                { name: 'M', weight: 20 }
            ]);
        }

        it('moves an item up across the static global, recomputing its weight above it', function () {
            const c = sortedList();
            expect(c.placeAt(4, 3)).to.equal(true); // M up one rank, past G
            c.sortItems();
            expect(names(c)).to.eql(['A', 'B', 'C', 'M', 'G']);
            expect(weightOf(c, 'M')).to.equal(51); // midpoint of C(60) and G(42)
            expect(weightOf(c, 'G')).to.equal(42); // static global never changes
        });

        it('keeps the item weight when it still fits the target gap', function () {
            const c = component([
                { name: 'A', weight: 90 },
                { name: 'B', weight: 50, originalWeight: 50 },
                { name: 'C', weight: 40 }
            ]);
            // Move A (idx 0) between B and C — gap (50, 40); A's original 90 does not fit → midpoint 45.
            expect(c.placeAt(0, 2)).to.equal(true);
            c.sortItems();
            expect(weightOf(c, 'A')).to.equal(45);
        });

        it('returns false when dropped back into its own slot', function () {
            const c = sortedList();
            expect(c.placeAt(1, 1)).to.equal(false);
            expect(c.placeAt(1, 2)).to.equal(false); // insertIndex === fromIndex + 1
        });

        it('refuses to move a static global row', function () {
            const c = sortedList();
            expect(c.placeAt(3, 0)).to.equal(false);
            expect(names(c)).to.eql(['A', 'B', 'C', 'G', 'M']);
        });

        it('places at the very top with a weight above the current maximum', function () {
            const c = sortedList();
            expect(c.placeAt(2, 0)).to.equal(true); // C to the top
            c.sortItems();
            expect(names(c)[0]).to.equal('C');
            expect(weightOf(c, 'C')).to.equal(91); // A(90) + 1
        });

        it('places at the very bottom with a weight below the current minimum', function () {
            const c = sortedList();
            expect(c.placeAt(1, 5)).to.equal(true); // B to the bottom
            c.sortItems();
            expect(names(c)[names(c).length - 1]).to.equal('B');
            expect(weightOf(c, 'B')).to.equal(19); // M(20) - 1
        });

        it('keeps a single remaining item weight unchanged', function () {
            const c = component([{ name: 'Solo', weight: 33 }]);
            expect(c.computeWeightForPosition(0)).to.equal(33);
        });
    });

    describe('setData / render', function () {
        it('marks a global entry shown in a narrower scope as a read-only static row', function () {
            ctx.scope = 'myproject';
            const c = component();
            c.setData(JSON.stringify([
                { name: 'P', scope: 'myproject', weight: 70 },
                { name: 'GLOB', scope: '', weight: 40 }
            ]));
            const rows = [...document.querySelectorAll('#weights-list .weight-item')];
            expect(rows.length).to.equal(2);

            const globRow = rows.find(r => r.querySelector('.name').textContent === 'GLOB');
            expect(globRow.classList.contains('static')).to.equal(true);
            expect(globRow.querySelector('.lock-marker')).to.not.equal(null);
            expect(globRow.querySelector('.drag-handle')).to.equal(null);
            expect(globRow.querySelector('input').readOnly).to.equal(true);
            expect(globRow.getAttribute('draggable')).to.equal(null);

            const projRow = rows.find(r => r.querySelector('.name').textContent === 'P');
            expect(projRow.querySelector('.drag-handle')).to.not.equal(null);
            expect(projRow.getAttribute('draggable')).to.equal('true');
        });

        it('does not mark global entries as static in the global scope', function () {
            ctx.scope = '';
            const c = component();
            c.setData(JSON.stringify([{ name: 'G', scope: '', weight: 40 }]));
            const row = document.querySelector('#weights-list .weight-item');
            expect(row.classList.contains('static')).to.equal(false);
            expect(row.querySelector('.drag-handle')).to.not.equal(null);
        });

        it('renders rows sorted by weight desc then name and disables the boundary arrows', function () {
            const c = component();
            c.setData(JSON.stringify([
                { name: 'low', scope: '', weight: 10 },
                { name: 'high', scope: '', weight: 90 },
                { name: 'mid', scope: '', weight: 50 }
            ]));
            const rows = [...document.querySelectorAll('#weights-list .weight-item')];
            expect(rows.map(r => r.querySelector('.name').textContent)).to.eql(['high', 'mid', 'low']);

            const topArrows = rows[0].querySelectorAll('.reorder-arrows button');
            expect(topArrows[0].disabled).to.equal(true);  // up disabled at the top
            expect(topArrows[1].disabled).to.equal(false);

            const bottomArrows = rows[2].querySelectorAll('.reorder-arrows button');
            expect(bottomArrows[0].disabled).to.equal(false);
            expect(bottomArrows[1].disabled).to.equal(true); // down disabled at the bottom
        });
    });

    describe('load / save', function () {
        it('GETs the weights for the current scope and renders them on success', function () {
            ctx.scope = 'proj';
            let request;
            ctx.callAsync = (req) => { request = req; };
            const c = component();

            c.load();
            expect(request.method).to.equal('GET');
            expect(request.url).to.contain('/polarion/test-exporter/rest/internal/settings/style-package/weights');
            expect(request.url).to.contain('scope=proj');

            request.onOk(JSON.stringify([{ name: 'Z', scope: 'proj', weight: 33 }]));
            expect(c.items.map(i => i.name)).to.eql(['Z']);
            expect(document.querySelectorAll('#weights-list .weight-item').length).to.equal(1);
        });

        it('POSTs only the non-static rows with the current scope', function () {
            ctx.scope = 'proj';
            let request;
            ctx.callAsync = (req) => { request = req; };
            ctx.showSaveSuccessAlert = () => {};
            const c = component([
                { name: 'A', scope: 'proj', weight: 80 },
                { name: 'G', scope: '', weight: 40, static: true }
            ]);

            c.save();
            expect(request.method).to.equal('POST');
            expect(JSON.parse(request.body)).to.eql([{ name: 'A', scope: 'proj', weight: 80 }]);

            request.onOk(); // must not throw
        });
    });

    describe('commitWeight', function () {
        it('syncs originalWeight so a later reorder keeps the typed value', function () {
            const c = component([{ name: 'X', weight: 80 }]);
            const input = document.createElement('input');
            input.type = 'number';
            input.value = '75';
            c.commitWeight(c.items[0], input);
            expect(c.items[0].weight).to.equal(75);
            expect(c.items[0].originalWeight).to.equal(75);
        });
    });
});
