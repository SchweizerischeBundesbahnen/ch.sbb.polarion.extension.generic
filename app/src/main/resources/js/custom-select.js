function SbbCustomSelect({selectContainer, label, changeListener, multiselect = false}) {
    this.selectContainer = selectContainer ? selectContainer : document.createElement('div');
    this.selectContainer.classList.add('sbb-custom-select');
    if (multiselect) {
        this.selectContainer.classList.add('multiselect');
    }

    this.selectBox = document.createElement('div');
    this.selectBox.classList.add('select-box');
    this.selectBox.addEventListener('click', () => this.toggleCheckboxContainer());
    this.selectContainer.appendChild(this.selectBox);

    this.selectElement = document.createElement('select');
    this.selectElement.setAttribute('id', this.selectContainer.getAttribute('id') + '_custom-select');
    this.selectBox.appendChild(this.selectElement);

    this.overSelect = document.createElement('div');
    this.overSelect.classList.add('over-select');
    this.selectBox.appendChild(this.overSelect);

    this.checkboxContainer = document.createElement('div');
    this.checkboxContainer.style.display = 'none';
    this.checkboxContainer.classList.add('checkboxes');
    this.selectContainer.appendChild(this.checkboxContainer);

    if (label) {
        label.htmlFor = this.selectElement.getAttribute('id');
    }

    this.changeListener = changeListener;
    this.mutiselect = multiselect;

    document.addEventListener("click",  (e) => {
        // Close select options if was clicked outside of select container
        if (this.checkboxContainer.style.display === "block" && !this.isChildElement(e.target)) {
            this.checkboxContainer.style.display = "none";
        }
    });
}

SbbCustomSelect.prototype.isChildElement = function(elementToCheck) {
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
};

SbbCustomSelect.prototype.empty = function() {
    this.checkboxContainer.innerHTML = "";
};

SbbCustomSelect.prototype.toggleCheckboxContainer = function () {
    this.checkboxContainer.style.display = this.checkboxContainer.style.display === "none" ? "block" : "none";
};

SbbCustomSelect.prototype.addOption = function(value, text) {
    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.value = value;
    checkbox.addEventListener("change", event => this.handleChange(event));
    const label = document.createElement("label");
    label.textContent = text ? text : value;
    label.prepend(checkbox);
    this.checkboxContainer.appendChild(label);

    return {checkbox: checkbox, label: label};
};

SbbCustomSelect.prototype.getAllCheckboxes = function() {
    return this.checkboxContainer.querySelectorAll('input[type="checkbox"]');
}

SbbCustomSelect.prototype.containsOption = function(optionValue) {
    for (const checkbox of this.getAllCheckboxes()) {
        if (checkbox.value === optionValue) {
            return true;
        }
    }
    return false;
}

SbbCustomSelect.prototype.getSelectedValue = function() {
    if (this.mutiselect) {
        const values = [];
        this.checkboxContainer.querySelectorAll('input[type="checkbox"]:checked').forEach(function (selectedCheckbox) {
            values.push(selectedCheckbox.value);
        });
        return values;
    } else {
        const selectedCheckbox = this.checkboxContainer.querySelector('input[type="checkbox"]:checked');
        return selectedCheckbox ? selectedCheckbox.value : "";
    }
};

SbbCustomSelect.prototype.getSelectedText = function() {
    if (this.mutiselect) {
        const texts = [];
        this.checkboxContainer.querySelectorAll('input[type="checkbox"]:checked').forEach(function (selectedCheckbox) {
            texts.push(selectedCheckbox.parentElement.textContent);
        });
        return texts;
    } else {
        const selectedCheckbox = this.checkboxContainer.querySelector('input[type="checkbox"]:checked');
        const selectedLabel = selectedCheckbox ? selectedCheckbox.parentElement : null;
        return selectedLabel ? selectedLabel.textContent : "";
    }
};

SbbCustomSelect.prototype.selectValue = function(value) {
    for (let checkbox of this.getAllCheckboxes()) {
        checkbox.checked = checkbox.value === value;
    }
    this.handleChange();
};

SbbCustomSelect.prototype.selectMultipleValues = function(values) {
    for (let checkbox of this.getAllCheckboxes()) {
        checkbox.checked = values && values.includes(checkbox.value);
    }
    this.handleChange();
};

// Using code like:
// this.selectElement.innerHTML = "<option>" + document.createTextNode(selectedCheckbox.parentElement.textContent).textContent + "</option>"
// results in XSS vulnerability. The code below solves this issue.
SbbCustomSelect.prototype.setSelectedOptionValue = function(optionText) {
    const optionElement = document.createElement("option");
    optionElement.textContent = optionText;
    this.selectElement.innerHTML = '';
    this.selectElement.appendChild(optionElement);
};

SbbCustomSelect.prototype.handleChange = function(event) {
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