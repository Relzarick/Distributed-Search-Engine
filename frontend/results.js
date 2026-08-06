export class ResultsGrid {
  constructor(elements, { onHeaderReorder, onHeaderAutoFit }) {
    this.elements = elements;
    this.onHeaderReorder = onHeaderReorder;
    this.onHeaderAutoFit = onHeaderAutoFit;
    this.pinnedRows = new Set();
    this.draggedHeader = null;
    this.currentData = [];
    this.currentHeaders = [];
    this.currentVisibleHeaders = new Set();
    this.initEventListeners();
  }

  initEventListeners() {
    if (this.elements.head) {
      this.elements.head.addEventListener("dragstart", (e) => this.handleHeaderDragStart(e));
      this.elements.head.addEventListener("dragover", (e) => e.preventDefault());
      this.elements.head.addEventListener("drop", (e) => this.handleHeaderDrop(e));
    }

    if (this.elements.body) {
      this.elements.body.addEventListener("click", (e) => {
        const td = e.target.closest("td.expandable");
        if (td) td.querySelector(".cell-content")?.classList.toggle("expanded");
      });

      this.elements.body.addEventListener("dblclick", (e) => this.handleRowDblClick(e));
    }
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
    const headers = [...this.currentHeaders];
    const fromIdx = headers.indexOf(this.draggedHeader);
    const toIdx = headers.indexOf(targetHeader);

    if (fromIdx === -1 || toIdx === -1 || fromIdx === toIdx) return;

    headers.splice(fromIdx, 1);
    headers.splice(toIdx, 0, this.draggedHeader);
    this.draggedHeader = null;
    this.onHeaderReorder(headers);
  }

  handleRowDblClick(e) {
    const tr = e.target.closest("tr");
    if (!tr) return;

    const targetItem = this.getRenderedRows().find((item) => String(item.row.id) === tr.dataset.rowId);
    if (!targetItem) return;

    if (this.pinnedRows.has(targetItem.row)) {
      this.pinnedRows.delete(targetItem.row);
      this.render(this.currentData, this.currentHeaders, this.currentVisibleHeaders);
      return;
    }

    const headerHeight = this.elements.head?.offsetHeight || 0;
    let currentPinnedHeight = 0;
    this.elements.body?.querySelectorAll("tr.pinned").forEach((pTr) => (currentPinnedHeight += pTr.offsetHeight));

    const wrapperHeight = this.elements.wrapper?.clientHeight || 0;
    if (headerHeight + currentPinnedHeight + tr.offsetHeight >= wrapperHeight) return;

    this.pinnedRows.add(targetItem.row);
    this.render(this.currentData, this.currentHeaders, this.currentVisibleHeaders);
  }

  getVisibleHeaders(headers, visibleHeaders) {
    return headers.filter((h) => visibleHeaders.has(h));
  }

  renderHeadersOnly(visible) {
    if (!this.elements.head) return;
    this.elements.head.innerHTML = `<tr>${visible.map((h) => `<th draggable="true" data-header="${h}">${h.replace(/_/g, " ")}</th>`).join("")}</tr>`;
  }

  fitHeadersToWidth(headers, visibleHeaders) {
    if (!this.elements.wrapper || this.elements.wrapper.clientWidth === 0) return;

    const visibleSet = new Set(visibleHeaders);
    this.render(this.currentData, headers, visibleSet);

    let changed = false;
    while (
      this.elements.wrapper.clientWidth > 0 &&
      this.elements.wrapper.scrollWidth > this.elements.wrapper.clientWidth &&
      visibleSet.size > 1
    ) {
      visibleSet.delete([...visibleSet].pop());
      this.render(this.currentData, headers, visibleSet);
      changed = true;
    }

    if (changed) this.onHeaderAutoFit(visibleSet);
  }

  getRenderedRows() {
    const pinnedList = Array.from(this.pinnedRows).map((row) => ({ row, isPinned: true }));
    const unpinnedList = this.currentData
      .filter((row) => !this.pinnedRows.has(row))
      .map((row) => ({ row, isPinned: false }));
    return [...pinnedList, ...unpinnedList];
  }

  render(data = [], headers = [], visibleHeaders = new Set()) {
    this.currentData = data;
    this.currentHeaders = headers;
    this.currentVisibleHeaders = visibleHeaders;

    const visible = this.getVisibleHeaders(headers, visibleHeaders);
    this.renderHeadersOnly(visible);

    if (this.elements.body) {
      this.elements.body.innerHTML = this.getRenderedRows()
        .map(
          ({ row, isPinned }) => `
        <tr data-row-id="${row.id}" class="${isPinned ? "pinned" : ""}">
          ${visible.map((h) => `<td><div class="cell-content">${row[h] ?? ""}</div></td>`).join("")}
        </tr>`,
        )
        .join("");
    }

    this.applyStickyPinnedOffsets();
    this.markTruncatedCells();
  }

  applyStickyPinnedOffsets() {
    if (!this.elements.head || !this.elements.body) return;
    let currentTop = this.elements.head.offsetHeight;

    this.elements.body.querySelectorAll("tr.pinned").forEach((tr) => {
      tr.style.position = "sticky";
      tr.style.top = `${currentTop}px`;
      tr.style.zIndex = "2";
      currentTop += tr.offsetHeight;
    });
  }

  markTruncatedCells() {
    if (!this.elements.body) return;
    requestAnimationFrame(() => {
      this.elements.body.querySelectorAll(".cell-content").forEach((content) => {
        content.parentElement?.classList.toggle("expandable", content.scrollHeight > content.clientHeight);
      });
    });
  }
}
