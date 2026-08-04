const PAGE_SIZE = 50;

class SearchResultsGrid {
  constructor() {
    this.elements = {
      form: document.querySelector(".search-form"),
      input: document.querySelector('input[name="query"]'),
      container: document.getElementById("table-container"),
      wrapper: document.querySelector(".table-wrapper"),
      head: document.getElementById("table-head"),
      body: document.getElementById("table-body"),
      filterBtn: document.getElementById("filter-btn"),
      filterMenu: document.getElementById("filter-menu"),
      filterCount: document.getElementById("filter-count"),
    };

    this.state = {
      data: [],
      headers: [],
      visibleHeaders: new Set(),
      lastQuery: null,
      page: 1,
    };

    this.init();
  }

  init() {
    this.elements.form.addEventListener("submit", (e) => this.handleSubmit(e));
    this.elements.filterBtn.addEventListener("click", () =>
      this.toggleFilterMenu(),
    );
    document.addEventListener("click", (e) => this.handleOutsideClick(e));

    this.elements.body.addEventListener("click", (e) => {
      const td = e.target.closest("td.expandable");
      if (td) td.querySelector(".cell-content").classList.toggle("expanded");
    });

    this.elements.head.addEventListener("dragstart", (e) =>
      this.handleHeaderDragStart(e),
    );
    this.elements.head.addEventListener("dragover", (e) => e.preventDefault());
    this.elements.head.addEventListener("drop", (e) =>
      this.handleHeaderDrop(e),
    );
  }

  async handleSubmit(e) {
    e.preventDefault();
    const query = this.elements.input.value.trim();
    if (!query || query === this.state.lastQuery) return;

    this.state.lastQuery = query;
    this.state.page = 1;
    this.elements.input.value = "";
    this.elements.form.classList.add("loading");

    try {
      const res = await fetch(`/search?q=${encodeURIComponent(query)}`);
      if (!res.ok) throw new Error("Request failed");
      this.renderResults(await res.json());
    } catch (error) {
      console.error("Search error:", error);
    } finally {
      this.elements.form.classList.remove("loading");
    }
  }

  renderResults(data) {
    this.state.data = Array.isArray(data)
      ? data.slice(0, PAGE_SIZE)
      : data
        ? [data]
        : [];

    if (!this.state.data.length) {
      this.elements.container.style.display = "none";
      return;
    }

    // Filter out the _id key so it's ignored
    this.state.headers = Object.keys(this.state.data[0]).filter(
      (key) => key !== "_id",
    );
    this.state.visibleHeaders = new Set(this.state.headers);
    this.elements.container.style.display = "flex";

    this.renderHeadersOnly();

    while (
      this.elements.wrapper.scrollWidth > this.elements.wrapper.clientWidth &&
      this.state.visibleHeaders.size > 1
    ) {
      const lastVisible = [...this.state.visibleHeaders].pop();
      this.state.visibleHeaders.delete(lastVisible);
      this.renderHeadersOnly();
    }

    this.buildDropdown();
    this.renderTable();
  }

  renderHeadersOnly() {
    const tr = document.createElement("tr");

    for (const header of this.state.headers) {
      if (!this.state.visibleHeaders.has(header)) continue;
      const th = document.createElement("th");
      th.textContent = header.replace(/_/g, " ");
      th.draggable = true;
      th.dataset.header = header;
      tr.append(th);
    }

    this.elements.head.replaceChildren(tr);
  }

  handleHeaderDragStart(e) {
    const th = e.target.closest("th");
    if (!th) return;
    this.draggedHeader = th.dataset.header;
    e.dataTransfer.effectAllowed = "move";
  }

  handleHeaderDrop(e) {
    e.preventDefault();
    const th = e.target.closest("th");
    if (!th || !this.draggedHeader) return;

    const targetHeader = th.dataset.header;
    if (targetHeader === this.draggedHeader) return;

    const headers = this.state.headers;
    headers.splice(headers.indexOf(this.draggedHeader), 1);
    headers.splice(headers.indexOf(targetHeader), 0, this.draggedHeader);

    this.draggedHeader = null;
    this.renderTable();
  }

  buildDropdown() {
    this.elements.filterMenu.replaceChildren();

    this.state.headers.forEach((header) => {
      const label = document.createElement("label");
      label.className = "filter-label";

      const checkbox = document.createElement("input");
      checkbox.type = "checkbox";
      checkbox.checked = this.state.visibleHeaders.has(header);

      checkbox.addEventListener("change", (e) => {
        e.target.checked
          ? this.state.visibleHeaders.add(header)
          : this.state.visibleHeaders.delete(header);
        this.renderTable();
      });

      label.append(checkbox, header.replace(/_/g, " "));
      this.elements.filterMenu.append(label);
    });
  }

  renderTable() {
    this.elements.filterCount.textContent = `${this.state.visibleHeaders.size} of ${this.state.headers.length} columns`;
    this.renderHeadersOnly();

    const fragment = document.createDocumentFragment();

    for (const row of this.state.data) {
      const tr = document.createElement("tr");

      for (const header of this.state.headers) {
        if (!this.state.visibleHeaders.has(header)) continue;

        const td = document.createElement("td");
        const content = document.createElement("div");
        content.className = "cell-content";
        content.textContent = String(row[header] ?? "");

        td.append(content);
        tr.append(td);
      }

      fragment.append(tr);
    }

    this.elements.body.replaceChildren(fragment);
    this.markTruncatedCells();
  }

  markTruncatedCells() {
    this.elements.body.querySelectorAll(".cell-content").forEach((content) => {
      content.parentElement.classList.toggle(
        "expandable",
        content.scrollHeight > content.clientHeight,
      );
    });
  }

  toggleFilterMenu() {
    const isExpanded = this.elements.filterMenu.classList.toggle("show");
    this.elements.filterBtn.setAttribute("aria-expanded", isExpanded);
  }

  handleOutsideClick(e) {
    if (
      !this.elements.filterBtn.contains(e.target) &&
      !this.elements.filterMenu.contains(e.target)
    ) {
      this.elements.filterMenu.classList.remove("show");
      this.elements.filterBtn.setAttribute("aria-expanded", "false");
    }
  }
}

new SearchResultsGrid();
