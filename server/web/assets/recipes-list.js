(function () {
    const searchInput = document.getElementById('recipe-search');
    const list = document.getElementById('recipe-list');
    const emptyFiltered = document.getElementById('recipe-list-empty-filtered');
    const filterBar = document.getElementById('course-type-filters');
    if (!searchInput || !list) return;

    let activeType = '';

    function normalize(value) {
        return (value || '').trim().toLocaleLowerCase('da-DK');
    }

    function recipeName(item) {
        const fromData = item.getAttribute('data-name');
        if (fromData) return normalize(fromData);
        const heading = item.querySelector('.recipe-meta h2');
        return heading ? normalize(heading.textContent) : '';
    }

    function applyFilters() {
        const query = normalize(searchInput.value);
        const items = list.querySelectorAll('.recipe-item');
        let visible = 0;
        const isSearching = query !== '';

        items.forEach((item) => {
            const name = recipeName(item);
            const courseType = normalize(item.getAttribute('data-course-type'));
            const matchesQuery = !isSearching || name.includes(query);
            const matchesType = activeType === '' || courseType === normalize(activeType);
            const show = matchesQuery && matchesType;
            item.classList.toggle('is-filtered-out', !show);
            item.hidden = !show;
            if (show) visible++;
        });

        list.classList.toggle('is-filtering', isSearching || activeType !== '');

        if (emptyFiltered) {
            emptyFiltered.hidden = visible > 0;
        }
    }

    searchInput.addEventListener('input', applyFilters);
    searchInput.addEventListener('search', applyFilters);

    if (filterBar) {
        filterBar.addEventListener('click', (event) => {
            const chip = event.target.closest('[data-course-type]');
            if (!chip || !filterBar.contains(chip)) return;

            const type = chip.getAttribute('data-course-type') || '';
            if (type !== '' && activeType === type) {
                activeType = '';
            } else {
                activeType = type;
            }

            filterBar.querySelectorAll('[data-course-type]').forEach((el) => {
                const chipType = el.getAttribute('data-course-type') || '';
                el.classList.toggle('is-active', chipType === activeType);
            });

            applyFilters();
        });
    }

    applyFilters();
})();
