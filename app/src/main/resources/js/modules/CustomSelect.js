export default class CustomSelect {
    constructor({selectContainer, label, changeListener, multiselect = false}) {
        this.selectContainer = selectContainer ? selectContainer : window.document.createElement('div');
        this.selectContainer.classList.add('sbb-custom-select'); // TODO rename class on non-module implementation removal
        if (multiselect) {
            this.selectContainer.classList.add('multiselect');
        }

        this.selectBox = window.document.createElement('div');
        this.selectBox.classList.add('select-box');
        this.selectBox.addEventListener('click', () => this.toggleCheckboxContainer());
        this.selectContainer.appendChild(this.selectBox);

        this.selectElement = window.document.createElement('select');
        this.selectElement.setAttribute('id', this.selectContainer.getAttribute('id') + '_custom-select');
        this.selectBox.appendChild(this.selectElement);

        this.overSelect = window.document.createElement('div');
        this.overSelect.classList.add('over-select');
        this.selectBox.appendChild(this.overSelect);

        this.checkboxContainer = window.document.createElement('div');
        this.checkboxContainer.style.display = 'none';
        this.checkboxContainer.classList.add('checkboxes');
        this.selectContainer.appendChild(this.checkboxContainer);

        if (label) {
            label.htmlFor = this.selectElement.getAttribute('id');
        }

        this.changeListener = changeListener;
        this.mutiselect = multiselect;

        window.document.addEventListener("click", (e) => {
            // Close select options if was clicked outside of select container
            if (this.checkboxContainer.style.display === "block" && !this.isChildElement(e.target)) {
                this.checkboxContainer.style.display = "none";
            }
        });
    }

    isChildElement(elementToCheck) {
        let element = elementToCheck;
        let nesting = 0;
        while (element && nesting < 5) {
            if (element === this.selectContainer) {
                return true;
            }
            element = element.parentElement;
            nesting++;
        }
        return false;
    }

    empty() {
        this.checkboxContainer.innerHTML = "";
    }

    toggleCheckboxContainer() {
        this.checkboxContainer.style.display = this.checkboxContainer.style.display === "none" ? "block" : "none";
    }

    addOption(value, text) {
        const checkbox = window.document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.value = value;
        checkbox.addEventListener("change", event => this.handleChange(event));
        const label = window.document.createElement("label");
        label.textContent = text ? text : value;
        label.prepend(checkbox);
        this.checkboxContainer.appendChild(label);

        return {checkbox: checkbox, label: label};
    }

    getAllCheckboxes() {
        return this.checkboxContainer.querySelectorAll('input[type="checkbox"]');
    }

    containsOption(optionValue) {
        for (const checkbox of this.getAllCheckboxes()) {
            if (checkbox.value === optionValue) {
                return true;
            }
        }
        return false;
    }

    getSelectedValue() {
        if (this.mutiselect) {
            return [...this.checkboxContainer.querySelectorAll('input[type="checkbox"]:checked')].map(cb => cb.value);
        } else {
            const selectedCheckbox = this.checkboxContainer.querySelector('input[type="checkbox"]:checked');
            return selectedCheckbox ? selectedCheckbox.value : "";
        }
    }

    getSelectedText() {
        if (this.mutiselect) {
            return [...this.checkboxContainer.querySelectorAll('input[type="checkbox"]:checked')].map(cb => cb.parentElement.textContent);
        } else {
            const selectedCheckbox = this.checkboxContainer.querySelector('input[type="checkbox"]:checked');
            const selectedLabel = selectedCheckbox ? selectedCheckbox.parentElement : null;
            return selectedLabel ? selectedLabel.textContent : "";
        }
    }

    selectValue(value) {
        for (let checkbox of this.getAllCheckboxes()) {
            checkbox.checked = checkbox.value === value;
        }
        this.handleChange();
    }

    selectMultipleValues(values) {
        for (let checkbox of this.getAllCheckboxes()) {
            checkbox.checked = values && values.includes(checkbox.value);
        }
        this.handleChange();
    }

// Using code like:
// this.selectElement.innerHTML = "<option>" + window.document.createTextNode(selectedCheckbox.parentElement.textContent).textContent + "</option>"
// results in XSS vulnerability. The code below solves this issue.
    setSelectedOptionValue(optionText) {
        const optionElement = window.document.createElement("option");
        optionElement.textContent = optionText;
        this.selectElement.innerHTML = '';
        this.selectElement.appendChild(optionElement);
    }

    handleChange(event) {
        if (this.mutiselect) {
            this.setSelectedOptionValue(this.getSelectedText().join(", "));
            if (this.changeListener) {
                this.changeListener(this.checkboxContainer.querySelectorAll('input[type="checkbox"]:checked'));
            }
        } else {
            if (this.checkboxContainer.querySelectorAll('input[type="checkbox"]:checked').length === 0) {
                if (event && event.target) {
                    event.target.checked = true; // Don't allow to clear selection
                }
            } else {
                const selectedCheckbox = event ? event.target : this.checkboxContainer.querySelector('input[type="checkbox"]:checked');
                if (selectedCheckbox && selectedCheckbox.checked) {
                    this.checkboxContainer.querySelectorAll('input[type="checkbox"]').forEach(function (checkbox) {
                        if (checkbox.checked && checkbox.value !== selectedCheckbox.value) {
                            checkbox.click(); // Don't allow multiple selection in case if multiselect mode wasn't explicitly switched on
                        }
                    });

                    this.setSelectedOptionValue(selectedCheckbox.parentElement.textContent);
                    this.checkboxContainer.querySelectorAll('label').forEach(function (label) {
                        label.classList.remove("selected");
                        if (label.textContent === selectedCheckbox.parentElement.textContent) {
                            label.classList.add("selected");
                        }
                    });

                    if (this.changeListener) {
                        this.changeListener(selectedCheckbox);
                    }
                }
            }
            this.checkboxContainer.style.display = "none";
        }
    }
}
