export class ColumnFilter {
  constructor(elements, { onToggle, onToggleAll }) {
    this.elements = elements;
    this.onToggle = onToggle;
    this.onToggleAll = onToggleAll;
    this.searchInput = this.elements.menu?.querySelector(".filter-search-input");
    this.chevron = this.elements.btn?.querySelector(".filter-chevron");
    this.spin = 0;
    this.initEventListeners();
  }

  initEventListeners() {
    if (this.elements.menu) {
      this.elements.menu.addEventListener("toggle", (e) => {
        const isOpen = e.newState === "open";
        this.elements.btn?.setAttribute("aria-expanded", String(isOpen));
        if (isOpen) this.positionMenu();
        else this.resetOptionFilter();
        this.spinChevron();
      });
    }

    if (this.searchInput) {
      const handleInput = () => this.filterOptionsInDom();
      this.searchInput.addEventListener("input", handleInput);
      this.searchInput.addEventListener("search", handleInput);
    }

    if (this.elements.selectAll) {
      this.elements.selectAll.addEventListener("change", (e) => this.onToggleAll(e.target.checked));
    }

    if (this.elements.options) {
      this.elements.options.addEventListener("change", (e) => {
        const checkbox = e.target.closest('input[type="checkbox"]');
        if (checkbox?.dataset.header) this.onToggle(checkbox.dataset.header, checkbox.checked);
      });
    }
  }

  positionMenu() {
    if (!this.elements.btn || !this.elements.menu) return;
    const alignRect = (this.elements.alignTo || this.elements.btn).getBoundingClientRect();
    const btnRect = this.elements.btn.getBoundingClientRect();
    this.elements.menu.style.top = `${btnRect.bottom + window.scrollY}px`;
    this.elements.menu.style.right = `${window.innerWidth - alignRect.right}px`;
  }

  spinChevron() {
    if (!this.chevron) return;
    this.spin += 180;
    this.chevron.style.setProperty("--spin", `${this.spin}deg`);
  }

  render(headers = [], visibleHeaders = new Set()) {
    if (this.elements.options) {
      this.elements.options.innerHTML = headers
        .map(
          (header) => `
        <label class="filter-label">
          <input type="checkbox" data-header="${header}" ${visibleHeaders.has(header) ? "checked" : ""}>
          <span>${header.replace(/_/g, " ")}</span>
        </label>`,
        )
        .join("");
    }

    if (this.elements.selectAll)
      this.elements.selectAll.checked = headers.length > 0 && visibleHeaders.size === headers.length;

    if (this.elements.count) {
      this.elements.count.textContent = `${visibleHeaders.size} of ${headers.length} columns`;
    }

    if (this.searchInput?.value) this.filterOptionsInDom();
  }

  filterOptionsInDom() {
    if (!this.searchInput || !this.elements.options) return;
    const query = this.searchInput.value.trim().toLowerCase();
    this.elements.options.querySelectorAll(".filter-label").forEach((label) => {
      label.hidden = !label.textContent.toLowerCase().includes(query);
    });
  }

  resetOptionFilter() {
    if (!this.searchInput) return;
    this.searchInput.value = "";
    this.filterOptionsInDom();
  }
}
