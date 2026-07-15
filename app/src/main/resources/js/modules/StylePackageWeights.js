import { enhanceNumericInput } from './NumericSpinner.js';

/**
 * Reusable "Style Package Weights" admin component, shared by the exporter extensions
 * (pdf-exporter, docx-exporter, ...). It renders the weighted, reorderable list of style packages
 * on the Polarion 2606 look (built on the --sbb-* control tokens; see css/style-package-weights.css)
 * and talks to each extension's own `rest/internal/settings/style-package/weights` endpoint through
 * the supplied ExtensionContext.
 *
 * Higher weight = higher position; the top item is the one pre-selected in the export panel dropdown.
 * A global-scoped entry shown in a narrower scope is read-only here (it is managed at the global
 * level) and only acts as a fixed reference point in the ordering.
 *
 *   import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';
 *   import StylePackageWeights from '../../ui/generic/js/modules/StylePackageWeights.js';
 *   const ctx = new ExtensionContext({ extension: 'pdf-exporter', scopeFieldId: 'scope' });
 *   new StylePackageWeights({ ctx, listId: 'sortable-list' });
 */
export default class StylePackageWeights {

    static ID_PREFIX = 'input.weight.';
    static GLOBAL_LOCK_TITLE = 'Global scope — defined at the global level and cannot be reordered here';

    static HANDLE_SVG = '<svg width="10" height="16" viewBox="0 0 10 16" fill="currentColor" aria-hidden="true">'
        + '<circle cx="2" cy="3" r="1.4"/><circle cx="8" cy="3" r="1.4"/><circle cx="2" cy="8" r="1.4"/>'
        + '<circle cx="8" cy="8" r="1.4"/><circle cx="2" cy="13" r="1.4"/><circle cx="8" cy="13" r="1.4"/></svg>';
    static LOCK_SVG = '<svg width="13" height="14" viewBox="0 0 14 16" fill="none" stroke="currentColor" stroke-width="1.3" aria-hidden="true">'
        + '<rect x="2.2" y="7" width="9.6" height="7" rx="1.2" fill="currentColor" stroke="none"/>'
        + '<path d="M4.3 7V4.8a2.7 2.7 0 0 1 5.4 0V7"/></svg>';
    static CARET_UP_SVG = '<svg viewBox="0 0 8 5" fill="currentColor" aria-hidden="true"><path d="M4 0l4 5H0z"/></svg>';
    static CARET_DOWN_SVG = '<svg viewBox="0 0 8 5" fill="currentColor" aria-hidden="true"><path d="M0 0h8L4 5z"/></svg>';

    /**
     * @param {ExtensionContext} ctx     configured with the owning extension and scope field
     * @param {string} listId            id of the <ul> that hosts the list
     * @param {boolean} bindToolbar      wire the standard save/cancel toolbar buttons (default true)
     * @param {boolean} autoLoad         load the weights immediately (default true)
     */
    constructor({ ctx, listId, bindToolbar = true, autoLoad = true }) {
        this.ctx = ctx;
        this.list = ctx.getElementById(listId);
        this.items = [];
        this.dragIndex = null;

        // This page has no "default" values and no per-configuration revisions.
        const defaultButton = ctx.getElementById("default-toolbar-button");
        if (defaultButton) {
            defaultButton.style.display = "none";
        }
        const revisionsButton = ctx.getElementById("revisions-toolbar-button");
        if (revisionsButton) {
            revisionsButton.style.display = "none";
        }

        if (bindToolbar) {
            ctx.onClick(
                'save-toolbar-button', () => this.save(),
                'cancel-toolbar-button', () => this.load()
            );
        }
        if (autoLoad) {
            this.load();
        }
    }

    load() {
        this.ctx.callAsync({
            method: 'GET',
            url: `/polarion/${this.ctx.extension}/rest/internal/settings/style-package/weights?scope=${this.ctx.scope}`,
            contentType: 'application/json',
            onOk: (responseText) => this.setData(responseText),
            onError: () => this.ctx.setLoadingErrorNotificationVisible(true)
        });
    }

    save() {
        const result = this.items
            .filter(item => !item.static)
            .map(item => ({ name: item.name, scope: this.ctx.scope, weight: item.weight }));

        this.ctx.callAsync({
            method: 'POST',
            url: `/polarion/${this.ctx.extension}/rest/internal/settings/style-package/weights`,
            contentType: 'application/json',
            body: JSON.stringify(result),
            onOk: () => this.ctx.showSaveSuccessAlert(),
            onError: () => this.ctx.showSaveErrorAlert()
        });
    }

    setData(jsonString) {
        this.items = JSON.parse(jsonString).map(item => {
            const isStatic = item.scope === "" && this.ctx.scope !== "";
            return {
                name: item.name,
                scope: item.scope,
                weight: item.weight,
                originalWeight: item.weight, // server snapshot, kept to preserve weight when it still fits
                static: isStatic
            };
        });
        this.render();
    }

    sortItems() {
        // Higher weight first; ties resolved alphabetically to keep a stable, predictable order.
        this.items.sort((a, b) => (b.weight - a.weight) || a.name.localeCompare(b.name));
    }

    render() {
        this.sortItems();
        this.list.innerHTML = '';

        this.items.forEach((item, index) => {
            const li = document.createElement("li");
            li.classList.add("weight-item");
            if (item.static) {
                li.classList.add("static");
            }

            // Left slot: lock (read-only global) or drag handle.
            const marker = document.createElement("span");
            if (item.static) {
                marker.className = "lock-marker";
                marker.title = StylePackageWeights.GLOBAL_LOCK_TITLE;
                marker.innerHTML = StylePackageWeights.LOCK_SVG;
            } else {
                marker.className = "drag-handle";
                marker.title = "Drag to reorder";
                marker.innerHTML = StylePackageWeights.HANDLE_SVG;
            }
            li.appendChild(marker);

            const name = document.createElement("span");
            name.className = "name";
            name.textContent = item.name;
            li.appendChild(name);

            // Weight input, wrapped by the shared 2606 caret spinner.
            const input = document.createElement("input");
            input.id = `${StylePackageWeights.ID_PREFIX}${item.name}`;
            input.type = "number";
            input.className = "weight-input";
            input.min = "0";
            input.max = "100";
            input.step = "0.1";
            input.value = item.weight;
            if (item.static) {
                input.readOnly = true;
            } else {
                input.addEventListener("change", () => this.commitWeight(item, input));
                input.addEventListener("keydown", (event) => {
                    if (event.key === "Enter") {
                        this.commitWeight(item, input);
                    }
                });
            }
            li.appendChild(input);
            enhanceNumericInput(input);

            li.appendChild(this.buildArrows(item, index));

            this.wireDrag(li, index, item);
            this.list.appendChild(li);
        });
    }

    buildArrows(item, index) {
        if (item.static) {
            const placeholder = document.createElement("span");
            placeholder.className = "reorder-arrows placeholder";
            return placeholder;
        }
        const box = document.createElement("span");
        box.className = "reorder-arrows";

        const up = document.createElement("button");
        up.type = "button";
        up.title = "Move up";
        up.innerHTML = StylePackageWeights.CARET_UP_SVG;
        up.disabled = index === 0;
        up.addEventListener("click", () => {
            if (this.placeAt(index, index - 1)) {
                this.render();
            }
        });

        const down = document.createElement("button");
        down.type = "button";
        down.title = "Move down";
        down.innerHTML = StylePackageWeights.CARET_DOWN_SVG;
        down.disabled = index === this.items.length - 1;
        down.addEventListener("click", () => {
            if (this.placeAt(index, index + 2)) {
                this.render();
            }
        });

        box.appendChild(up);
        box.appendChild(down);
        return box;
    }

    // Any row is a valid drop target (including a static global): the drop position is decided by the
    // top / bottom half of the hovered row, so a package can be placed directly above or below the
    // read-only global entry. Only non-static rows are draggable.
    wireDrag(li, index, item) {
        if (!item.static) {
            li.setAttribute("draggable", "true");
            li.addEventListener("dragstart", (event) => {
                this.dragIndex = index;
                li.classList.add("dragging");
                event.dataTransfer.effectAllowed = "move";
                try {
                    event.dataTransfer.setData("text/plain", String(index));
                } catch (ignored) { /* some browsers require a payload */ }
            });
            li.addEventListener("dragend", () => {
                li.classList.remove("dragging");
                this.clearDropIndicators();
                this.dragIndex = null;
            });
        }
        li.addEventListener("dragover", (event) => {
            if (this.dragIndex === null) {
                return;
            }
            event.preventDefault();
            event.dataTransfer.dropEffect = "move";
            this.clearDropIndicators();
            li.classList.add(this.isBottomHalf(event, li) ? "drop-below" : "drop-above");
        });
        li.addEventListener("dragleave", () => {
            li.classList.remove("drop-above", "drop-below");
        });
        li.addEventListener("drop", (event) => {
            if (this.dragIndex === null) {
                return;
            }
            event.preventDefault();
            const insertIndex = this.isBottomHalf(event, li) ? index + 1 : index;
            const changed = this.placeAt(this.dragIndex, insertIndex);
            this.dragIndex = null;
            this.clearDropIndicators();
            if (changed) {
                this.render();
            }
        });
    }

    isBottomHalf(event, element) {
        const rect = element.getBoundingClientRect();
        return (event.clientY - rect.top) > rect.height / 2;
    }

    clearDropIndicators() {
        this.list.querySelectorAll(".drop-above, .drop-below")
            .forEach(node => node.classList.remove("drop-above", "drop-below"));
    }

    // Move the item at fromIndex into the slot at insertIndex (0..length), recomputing its weight to
    // fit between its new neighbours. Works across a static global (which never moves). Returns true
    // when something actually changed.
    placeAt(fromIndex, insertIndex) {
        if (insertIndex === fromIndex || insertIndex === fromIndex + 1) {
            return false; // dropped back into its own slot
        }
        const moved = this.items[fromIndex];
        if (!moved || moved.static) {
            return false;
        }
        this.items.splice(fromIndex, 1);
        if (insertIndex > fromIndex) {
            insertIndex--;
        }
        insertIndex = Math.max(0, Math.min(this.items.length, insertIndex));
        this.items.splice(insertIndex, 0, moved);
        moved.weight = this.computeWeightForPosition(insertIndex);
        return true; // every caller re-renders, and render() re-sorts
    }

    // Weight that keeps the moved item at insertIndex: reuse its original weight when it still fits the
    // gap between the neighbours, otherwise place it in the middle of the gap (or just past the edge).
    computeWeightForPosition(insertIndex) {
        const items = this.items;
        if (items.length <= 1) {
            return items[insertIndex].weight;
        }
        const initial = items[insertIndex].originalWeight;
        let value;
        if (insertIndex === 0) {
            const next = items[1].weight;
            value = initial > next ? initial : next + 1;
        } else if (insertIndex === items.length - 1) {
            const prev = items[items.length - 2].weight;
            value = initial < prev ? initial : prev - 1;
        } else {
            const prev = items[insertIndex - 1].weight;
            const next = items[insertIndex + 1].weight;
            value = (initial > next && initial < prev)
                ? initial
                : parseFloat((prev + (next - prev) / 2).toFixed(1));
        }
        return Math.max(0, Math.min(100, value));
    }

    commitWeight(item, input) {
        StylePackageWeights.adjustWeight(input);
        item.weight = parseFloat(input.value);
        // A manually typed weight becomes the new preferred value, so a later reorder keeps it (via
        // computeWeightForPosition) instead of reverting to the server snapshot.
        item.originalWeight = item.weight;
        this.render();
    }

    /**
     * Normalise a manually typed weight: clamp to [0, 100], round to one decimal, fall back to 50 for
     * anything that does not match the NNN.N shape. Mutates and returns input.value.
     */
    static adjustWeight(input) {
        let value = parseFloat(input.value);

        if (value > 100) {
            value = 100;
        }
        if (value < 0) {
            value = 0;
        }
        if (value % 1 !== 0) {
            value = parseFloat(value.toFixed(1));
        }
        if (!/^\d{1,3}(\.\d)?$/.test(value)) {
            value = 50;
        }

        input.value = value;
    }
}
