(function () {
    function sectionIndex(container, attr) {
        return container.querySelectorAll('[' + attr + ']').length;
    }

    function cloneFromTemplate(id, replacements) {
        const tpl = document.getElementById(id);
        if (!tpl || !tpl.content.firstElementChild) return null;
        const node = tpl.content.firstElementChild.cloneNode(true);
        node.querySelectorAll('[name]').forEach((el) => {
            let name = el.getAttribute('name') || '';
            Object.keys(replacements).forEach((key) => {
                name = name.split(key).join(String(replacements[key]));
            });
            el.setAttribute('name', name);
        });
        return node;
    }

    function wireRemoveButtons(root) {
        root.querySelectorAll('[data-remove-row]').forEach((btn) => {
            if (btn.dataset.wired) return;
            btn.dataset.wired = '1';
            btn.addEventListener('click', () => {
                const row = btn.closest('[data-row]');
                if (row) row.remove();
            });
        });
        root.querySelectorAll('[data-remove-section]').forEach((btn) => {
            if (btn.dataset.wired) return;
            btn.dataset.wired = '1';
            btn.addEventListener('click', () => {
                const section = btn.closest('[data-ingredient-section], [data-instruction-section]');
                if (section) section.remove();
            });
        });
    }

    function wireIngredientSection(section) {
        if (section.dataset.wired) return;
        section.dataset.wired = '1';

        const list = section.querySelector('[data-ingredient-list]');
        const addBtn = section.querySelector('[data-add-ingredient]');
        const container = document.getElementById('ingredient-sections');
        if (!list || !addBtn || !container) return;

        addBtn.addEventListener('click', () => {
            const si = Array.from(container.querySelectorAll('[data-ingredient-section]')).indexOf(section);
            const ii = list.querySelectorAll('[data-row]').length;
            const row = cloneFromTemplate('tpl-ingredient-row', { '__SI__': si, '__II__': ii });
            if (!row) return;
            list.appendChild(row);
            wireRemoveButtons(row);
        });
        wireRemoveButtons(section);
    }

    function wireInstructionSection(section) {
        if (section.dataset.wired) return;
        section.dataset.wired = '1';

        const list = section.querySelector('[data-step-list]');
        const addBtn = section.querySelector('[data-add-step]');
        const container = document.getElementById('instruction-sections');
        if (!list || !addBtn || !container) return;

        addBtn.addEventListener('click', () => {
            const si = Array.from(container.querySelectorAll('[data-instruction-section]')).indexOf(section);
            const ii = list.querySelectorAll('[data-row]').length;
            const row = cloneFromTemplate('tpl-step-row', { '__SI__': si, '__II__': ii });
            if (!row) return;
            list.appendChild(row);
            wireRemoveButtons(row);
        });
        wireRemoveButtons(section);
    }

    const ingContainer = document.getElementById('ingredient-sections');
    const addIngSection = document.getElementById('add-ingredient-section');
    if (ingContainer && addIngSection) {
        ingContainer.querySelectorAll('[data-ingredient-section]').forEach(wireIngredientSection);
        addIngSection.addEventListener('click', () => {
            const si = sectionIndex(ingContainer, 'data-ingredient-section');
            const section = cloneFromTemplate('tpl-ingredient-section', { '__SI__': si });
            if (!section) return;
            ingContainer.appendChild(section);
            wireIngredientSection(section);
        });
    }

    const instContainer = document.getElementById('instruction-sections');
    const addInstSection = document.getElementById('add-instruction-section');
    if (instContainer && addInstSection) {
        instContainer.querySelectorAll('[data-instruction-section]').forEach(wireInstructionSection);
        addInstSection.addEventListener('click', () => {
            const si = sectionIndex(instContainer, 'data-instruction-section');
            const section = cloneFromTemplate('tpl-instruction-section', { '__SI__': si });
            if (!section) return;
            instContainer.appendChild(section);
            wireInstructionSection(section);
        });
    }
})();
