class ColumnFilter {
  constructor(state, elements, onChange) {
    this.state = state;
    this.elements = elements;
    this.onChange = onChange;

    // Internal DOM query
    this.searchInput = this.elements.menu.querySelector(".filter-search-input");

    // Sync ARIA state and reset search whenever popover closes
    this.elements.menu.addEventListener("toggle", (e) => {
      const isOpen = e.newState === "open";
      this.elements.btn.setAttribute("aria-expanded", isOpen);

      if (!isOpen) this.resetSearch();
    });

    // Real-time search filtering on keypress ('input') and clear button ('search')
    if (this.searchInput) {
      this.searchInput.addEventListener("input", () => this.filterOptions());
      this.searchInput.addEventListener("search", () => this.filterOptions());
    }

    // Select all handler
    this.elements.selectAll.addEventListener("change", (e) =>
      this.handleSelectAll(e),
    );

    // Event delegation for individual column checkboxes
    this.elements.options.addEventListener("change", (e) => {
      if (!e.target.matches('input[type="checkbox"]')) return;

      const header = e.target.dataset.header;
      e.target.checked
        ? this.state.visibleHeaders.add(header)
        : this.state.visibleHeaders.delete(header);

      this.syncSelectAll();
      this.onChange();
    });
  }

  build() {
    this.syncSelectAll();

    this.elements.options.innerHTML = this.state.headers
      .map(
        (header) => `
        <label class="filter-label" data-header="${header}">
          <input type="checkbox" data-header="${header}" ${this.state.visibleHeaders.has(header) ? "checked" : ""}>
          ${header.replace(/_/g, " ")}
        </label>`,
      )
      .join("");

    // Re-apply current search query if menu is rebuilt while typing
    if (this.searchInput?.value) this.filterOptions();
  }

  filterOptions() {
    const query = this.searchInput.value.trim().toLowerCase();
    const labels = this.elements.options.querySelectorAll(".filter-label");

    labels.forEach((label) => {
      const match = label.textContent.toLowerCase().includes(query);
      // Inline style overrides .filter-label { display: flex } in CSS
      label.style.display = match ? "" : "none";
    });
  }

  resetSearch() {
    if (!this.searchInput) return;
    this.searchInput.value = "";
    this.filterOptions();
  }

  handleSelectAll(e) {
    const checked = e.target.checked;
    this.state.visibleHeaders = checked
      ? new Set(this.state.headers)
      : new Set();

    this.elements.options
      .querySelectorAll('input[type="checkbox"]')
      .forEach((cb) => (cb.checked = checked));

    this.onChange();
  }

  syncSelectAll() {
    this.elements.selectAll.checked =
      this.state.headers.length > 0 &&
      this.state.visibleHeaders.size === this.state.headers.length;
  }

  updateCount() {
    this.elements.count.textContent = `${this.state.visibleHeaders.size} of ${this.state.headers.length} columns`;
  }
}
