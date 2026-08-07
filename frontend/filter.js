export class BaseFilter {
  constructor(elements) {
    this.elements = elements;
    this.repositionHandler = null;
    this.resizeObserver = null;
  }

  initToggleListener(onOpen, onClose) {
    this.elements.menu?.addEventListener("toggle", (e) => {
      const isOpen = e.newState === "open";
      this.elements.btn?.setAttribute("aria-expanded", String(isOpen));

      if (isOpen) {
        this.startPositioning();
        onOpen?.(e);
      } else {
        this.stopPositioning();
        onClose?.(e);
      }
    });
  }

  startPositioning() {
    this.repositionHandler = () => this.positionMenu();
    window.addEventListener("resize", this.repositionHandler);
    window.addEventListener("scroll", this.repositionHandler, true);

    if (window.ResizeObserver && this.elements.alignTo) {
      this.resizeObserver = new ResizeObserver(this.repositionHandler);
      this.resizeObserver.observe(this.elements.alignTo);
    }

    this.positionMenu();
  }

  stopPositioning() {
    if (this.repositionHandler) {
      window.removeEventListener("resize", this.repositionHandler);
      window.removeEventListener("scroll", this.repositionHandler, true);
      this.repositionHandler = null;
    }
    if (this.resizeObserver) {
      this.resizeObserver.disconnect();
      this.resizeObserver = null;
    }
  }

  positionMenu() {
    if (!this.elements.menu || !this.elements.btn) return;

    const viewportWidth = document.documentElement.clientWidth;

    // Use alignTo target (e.g., .table-frame) if available, fallback to button
    const hasAlignTo = Boolean(this.elements.alignTo?.getClientRects().length);
    const alignEl = hasAlignTo ? this.elements.alignTo : this.elements.btn;
    const alignRect = alignEl.getBoundingClientRect();

    // 1. Align Y-position directly to the top edge of the table element
    const marginTop = 0; // Set to 0 for flush alignment, or add px offset if needed
    const top = alignRect.top + marginTop;

    // 2. Account for scrollbar width inside the table wrapper
    const scrollEl = alignEl.querySelector(".table-wrapper") || alignEl;
    let scrollbarWidth = 0;

    if (scrollEl) {
      const style = window.getComputedStyle(scrollEl);
      const borderLeft = parseFloat(style.borderLeftWidth) || 0;
      const borderRight = parseFloat(style.borderRightWidth) || 0;
      const widthDiff = scrollEl.offsetWidth - scrollEl.clientWidth - borderLeft - borderRight;
      scrollbarWidth = Math.max(0, widthDiff);
    }

    // 3. Align X-position to the right edge of the table element
    const baseRight = Math.max(0, viewportWidth - alignRect.right);
    const right = baseRight + scrollbarWidth;

    Object.assign(this.elements.menu.style, {
      position: "fixed",
      top: `${top}px`,
      right: `${right}px`,
      left: "auto",
      bottom: "auto",
      margin: "0",
      transform: "none",
    });
  }
}

export class ColumnFilter extends BaseFilter {
  constructor(elements, { onToggle, onToggleAll } = {}) {
    super(elements);
    this.onToggle = onToggle;
    this.onToggleAll = onToggleAll;
    this.searchInput = this.elements.menu?.querySelector(".filter-search-input");
    this.chevron = this.elements.btn?.querySelector(".filter-chevron");
    this.spin = 0;
    this.initEventListeners();
  }

  initEventListeners() {
    this.initToggleListener(
      () => this.spinChevron(),
      () => {
        this.spinChevron();
        this.resetSearchFilter();
      },
    );

    const handleInput = () => this.filterOptionsInDom();
    this.searchInput?.addEventListener("input", handleInput);
    this.searchInput?.addEventListener("search", handleInput);

    this.elements.selectAll?.addEventListener("change", (e) => {
      this.onToggleAll?.(e.target.checked);
      this.elements.menu?.dispatchEvent(
        new CustomEvent("column-filter-toggle-all", { detail: { checked: e.target.checked }, bubbles: true }),
      );
    });

    this.elements.options?.addEventListener("change", (e) => {
      const checkbox = e.target.closest('input[type="checkbox"]');
      if (checkbox?.dataset.header) {
        this.onToggle?.(checkbox.dataset.header, checkbox.checked);
        this.elements.menu?.dispatchEvent(
          new CustomEvent("column-filter-toggle", {
            detail: { header: checkbox.dataset.header, checked: checkbox.checked },
            bubbles: true,
          }),
        );
      }
    });
  }

  spinChevron() {
    this.spin += 180;
    if (this.chevron) this.chevron.style.setProperty("--spin", `${this.spin}deg`);
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

    if (this.elements.count) this.elements.count.textContent = `${visibleHeaders.size} of ${headers.length} columns`;

    if (this.searchInput?.value) this.filterOptionsInDom();
  }

  filterOptionsInDom() {
    if (!this.searchInput || !this.elements.options) return;
    const query = this.searchInput.value.trim().toLowerCase();

    this.elements.options.querySelectorAll(".filter-label").forEach((label) => {
      label.hidden = !label.textContent.toLowerCase().includes(query);
    });
  }

  resetSearchFilter() {
    if (!this.searchInput) return;
    this.searchInput.value = "";
    this.filterOptionsInDom();
  }
}

export class ValueFilter extends BaseFilter {
  constructor(elements, { schema = new Map() } = {}) {
    super(elements);
    this.schema = schema instanceof Map ? schema : new Map(Object.entries(schema));
    this.filters = new Map();
    this.activeHeaders = new Set();
    this.headers = [];
    this.container = null;
    this.initEventListeners();
  }

  initEventListeners() {
    this.initToggleListener(null, () => this.resetSearchFilter());

    this.elements.menu?.addEventListener("click", (e) => this.handleMenuClick(e));

    this.elements.menu?.addEventListener("change", (e) => {
      const operator = e.target.closest(".value-field-operator");
      if (operator) return this.handleOperatorChange(operator.dataset.header, operator.value);

      const input = e.target.closest(".value-field-input");
      if (input) this.handleInputChange(input.dataset.header, input.dataset.role, input.value);
    });

    this.elements.menu?.addEventListener("input", (e) => {
      const searchInput = e.target.closest(".filter-search-input");
      if (searchInput) return this.handleSearchInput(searchInput.value);

      const input = e.target.closest(".value-field-input");
      if (input) this.handleInputChange(input.dataset.header, input.dataset.role, input.value);
    });
  }

  resetSearchFilter() {
    const searchInput = this.elements.menu?.querySelector(".filter-search-input");
    if (!searchInput) return;
    searchInput.value = "";
    this.handleSearchInput("");
  }

  dispatchFilterChange(header, filter) {
    this.elements.menu?.dispatchEvent(
      new CustomEvent("value-filter-change", { detail: { header, filter }, bubbles: true }),
    );
  }

  createDefaultFilter(type) {
    return {
      mode: type === "string" ? "contains" : "eq",
      value: type === "string" ? "" : null,
      min: null,
      max: null,
    };
  }

  escapeAttr(val) {
    return String(val ?? "").replace(/"/g, "&quot;");
  }

  handleSearchInput(term) {
    if (!this.container) return;
    const lowerTerm = term.toLowerCase();

    for (const child of this.container.children) {
      const header = child.dataset.header || "";
      const displayString = header.replace(/_/g, " ").toLowerCase();
      child.style.display = displayString.includes(lowerTerm) ? "" : "none";
    }
  }

  handleMenuClick(e) {
    const refreshBtn = e.target.closest(".value-refresh-btn");
    if (refreshBtn) {
      e.stopPropagation();
      return this.handleResetHeader(refreshBtn.dataset.header);
    }

    const chip = e.target.closest(".value-chip");
    if (!chip) return;

    const header = chip.dataset.header;
    const isActive = !this.activeHeaders.has(header);
    const type = this.schema.get(header) || "string";

    isActive ? this.activeHeaders.add(header) : this.activeHeaders.delete(header);

    if (isActive) this.filters.set(header, this.createDefaultFilter(type));
    else this.filters.delete(header);

    this.dispatchFilterChange(header, this.filters.get(header) || null);
    this.render();
  }

  handleResetHeader(header) {
    const type = this.schema.get(header) || "string";
    if (!this.filters.has(header)) return;

    this.filters.set(header, this.createDefaultFilter(type));
    this.dispatchFilterChange(header, this.filters.get(header));
    this.render();
  }

  handleOperatorChange(header, mode) {
    const filter = this.filters.get(header) || {};
    this.filters.set(header, {
      mode,
      value: filter.value ?? "",
      min: filter.min ?? null,
      max: filter.max ?? null,
    });
    this.dispatchFilterChange(header, this.filters.get(header));
    this.render();
  }

  handleInputChange(header, role, rawValue) {
    const filter = this.filters.get(header);
    if (!filter) return;

    const type = this.schema.get(header);
    if (rawValue === "") filter[role] = type === "string" ? "" : null;
    else filter[role] = type === "string" || type === "date" ? rawValue : Number(rawValue);

    this.dispatchFilterChange(header, filter);
  }

  buildInputsHtml(header, filter, type) {
    if (!filter) return "";
    const safeHeader = this.escapeAttr(header);

    if (type === "string") {
      return `<input type="text" class="value-field-input" data-header="${safeHeader}" data-role="value" value="${this.escapeAttr(filter.value)}">`;
    }

    const inputType = type === "date" ? "date" : "number";
    const stepAttr = type === "number" ? ' step="any"' : "";

    if (filter.mode === "range") {
      return `
        <input type="${inputType}"${stepAttr} class="value-field-input" data-header="${safeHeader}" data-role="min" value="${this.escapeAttr(filter.min)}">
        <input type="${inputType}"${stepAttr} class="value-field-input" data-header="${safeHeader}" data-role="max" value="${this.escapeAttr(filter.max)}">`;
    }

    return `<input type="${inputType}"${stepAttr} class="value-field-input" data-header="${safeHeader}" data-role="value" value="${this.escapeAttr(filter.value)}">`;
  }

  buildTypedFieldHtml(header, isActive, type) {
    const filter = this.filters.get(header);
    const safeHeader = this.escapeAttr(header);

    const operatorOptions =
      type === "string"
        ? `
          <option value="contains" ${filter?.mode === "contains" ? "selected" : ""}>~</option>
          <option value="eq" ${filter?.mode === "eq" ? "selected" : ""}>=</option>`
        : `
          <option value="eq" ${filter?.mode === "eq" ? "selected" : ""}>=</option>
          <option value="lt" ${filter?.mode === "lt" ? "selected" : ""}>&lt;</option>
          <option value="gt" ${filter?.mode === "gt" ? "selected" : ""}>&gt;</option>
          <option value="range" ${filter?.mode === "range" ? "selected" : ""}>~</option>`;

    const childHtml = isActive
      ? `
        <div class="value-field-child" data-header="${safeHeader}">
          <select class="value-field-operator" data-header="${safeHeader}">${operatorOptions}</select>
          <div class="value-field-inputs" data-header="${safeHeader}">${this.buildInputsHtml(header, filter, type)}</div>
        </div>`
      : "";

    const refreshBtnHtml = isActive
      ? `
        <button type="button" class="value-refresh-btn" data-header="${safeHeader}" title="Reset field">
          <span class="icon icon-refresh"></span>
        </button>`
      : "";

    return `
      <div class="value-field${isActive ? " is-active" : ""}" data-header="${safeHeader}">
        <div class="value-field-header">
          <button type="button" class="value-chip" data-header="${safeHeader}">
            <span class="value-chip-label">
              <span>${header.replace(/_/g, " ")}</span>
              <svg class="filter-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                <polyline points="6 9 12 15 18 9"></polyline>
              </svg>
            </span>
          </button>
          ${refreshBtnHtml}
        </div>
        ${childHtml}
      </div>`;
  }

  initContainer() {
    this.elements.menu.innerHTML = `
      <div class="filter-search">
        <input type="text" class="filter-search-input" placeholder="search..." aria-label="Search values">
      </div>
      <div class="value-fields-container"></div>
    `;
    this.container = this.elements.menu.querySelector(".value-fields-container");
  }

  render(headers) {
    if (headers) this.headers = headers;
    if (!this.container) this.initContainer();

    this.container.innerHTML = this.headers
      .map((header) => {
        const type = this.schema.get(header) || "string";
        const isActive = this.activeHeaders.has(header);
        return this.buildTypedFieldHtml(header, isActive, type);
      })
      .join("");

    const currentSearch = this.elements.menu.querySelector(".filter-search-input")?.value;
    if (currentSearch) this.handleSearchInput(currentSearch);
  }
}
