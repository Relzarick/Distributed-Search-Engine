class ColumnFilter {
  constructor(state, elements, onChange) {
    this.state = state;
    this.elements = elements; // { btn, menu, selectAll, options, count }
    this.onChange = onChange;

    this.elements.btn.addEventListener("click", () => this.toggleMenu());
    document.addEventListener("click", (e) => this.handleOutsideClick(e));
    this.elements.selectAll.addEventListener("change", (e) =>
      this.handleSelectAll(e),
    );
  }

  build() {
    this.elements.selectAll.checked = true;
    this.elements.options.replaceChildren();

    this.state.headers.forEach((header) => {
      const label = document.createElement("label");
      label.className = "filter-label";
      label.dataset.header = header;

      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.checked = this.state.visibleHeaders.has(header);

      checkbox.addEventListener("change", (e) => {
        e.target.checked
          ? this.state.visibleHeaders.add(header)
          : this.state.visibleHeaders.delete(header);

        this.elements.selectAll.checked =
          this.state.visibleHeaders.size === this.state.headers.length;

        this.onChange();
      });

      label.append(checkbox, header.replace(/_/g, " "));
      this.elements.options.append(label);
    });
  }

  handleSelectAll(e) {
    this.state.visibleHeaders = e.target.checked
      ? new Set(this.state.headers)
      : new Set();

    this.elements.options
      .querySelectorAll('input[type="checkbox"]')
      .forEach((checkbox) => {
        checkbox.checked = e.target.checked;
      });

    this.onChange();
  }

  updateCount() {
    this.elements.count.textContent = `${this.state.visibleHeaders.size} of ${this.state.headers.length} columns`;
  }

  toggleMenu() {
    const isExpanded = this.elements.menu.classList.toggle("show");
    this.elements.btn.setAttribute("aria-expanded", isExpanded);
  }

  handleOutsideClick(e) {
    if (
      !this.elements.btn.contains(e.target) &&
      !this.elements.menu.contains(e.target)
    ) {
      this.elements.menu.classList.remove("show");
      this.elements.btn.setAttribute("aria-expanded", "false");
    }
  }
}
