class ResultsGrid {
  constructor(state, elements, onReorder) {
    this.state = state;
    this.elements = elements; // { wrapper, head, body }
    this.onReorder = onReorder;
    this.pinnedRows = new Set();

    // Header Drag & Drop Listeners
    this.elements.head.addEventListener("dragstart", (e) => this.handleHeaderDragStart(e));
    this.elements.head.addEventListener("dragover", (e) => e.preventDefault());
    this.elements.head.addEventListener("drop", (e) => this.handleHeaderDrop(e));

    // Cell Expansion Delegate
    this.elements.body.addEventListener("click", (e) => {
      const td = e.target.closest("td.expandable");
      if (td) td.querySelector(".cell-content").classList.toggle("expanded");
    });

    // Row Pinning Delegate (Double Click)
    this.elements.body.addEventListener("dblclick", (e) => this.handleRowDblClick(e));
  }

  handleRowDblClick(e) {
    const tr = e.target.closest("tr");
    if (!tr) return;

    const index = Number(tr.dataset.rowIndex);
    const row = this.state.data[index];
    if (!row) return;

    if (this.pinnedRows.has(row)) {
      this.pinnedRows.delete(row);
      this.render();
    } else {
      const headerHeight = this.elements.head.offsetHeight;
      let currentPinnedHeight = 0;
      this.elements.body.querySelectorAll("tr.pinned").forEach((pinnedTr) => {
        currentPinnedHeight += pinnedTr.offsetHeight;
      });

      const wrapperHeight = this.elements.wrapper.clientHeight;
      const newRowHeight = tr.offsetHeight;

      // Prevent pinning if sticky header + existing pinned rows + new row exceed visible grid height
      if (headerHeight + currentPinnedHeight + newRowHeight >= wrapperHeight) {
        return;
      }

      this.pinnedRows.add(row);
      this.render();
    }
  }

  getVisibleHeaders() {
    return this.state.headers.filter((h) => this.state.visibleHeaders.has(h));
  }

  renderHeadersOnly() {
    this.elements.head.innerHTML = `<tr>${this.getVisibleHeaders()
      .map((h) => `<th draggable="true" data-header="${h}">${h.replace(/_/g, " ")}</th>`)
      .join("")}</tr>`;
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
    const headers = this.state.headers;

    const fromIdx = headers.indexOf(this.draggedHeader);
    const toIdx = headers.indexOf(targetHeader);

    if (fromIdx === -1 || toIdx === -1 || fromIdx === toIdx) return;

    headers.splice(fromIdx, 1);
    headers.splice(toIdx, 0, this.draggedHeader);

    this.draggedHeader = null;
    this.onReorder();
  }

  fitHeadersToWidth() {
    this.renderHeadersOnly();
    while (
      this.elements.wrapper.scrollWidth > this.elements.wrapper.clientWidth &&
      this.state.visibleHeaders.size > 1
    ) {
      const lastVisible = [...this.state.visibleHeaders].pop();
      this.state.visibleHeaders.delete(lastVisible);
      this.renderHeadersOnly();
    }
  }

  render() {
    this.renderHeadersOnly();
    const visible = this.getVisibleHeaders();

    // Clean up stale pinned row references if state data refreshed
    this.pinnedRows = new Set([...this.pinnedRows].filter((r) => this.state.data.includes(r)));

    // Separate pinned and unpinned rows while preserving original data indices
    const rowsWithIndex = this.state.data.map((row, index) => ({ row, index }));
    const pinned = rowsWithIndex.filter(({ row }) => this.pinnedRows.has(row));
    const unpinned = rowsWithIndex.filter(({ row }) => !this.pinnedRows.has(row));

    const orderedRows = [...pinned, ...unpinned];

    this.elements.body.innerHTML = orderedRows
      .map(
        ({ row, index }) => `
        <tr data-row-index="${index}" class="${this.pinnedRows.has(row) ? "pinned" : ""}">
          ${visible.map((h) => `<td><div class="cell-content">${row[h] ?? ""}</div></td>`).join("")}
        </tr>`,
      )
      .join("");

    this.applyStickyPinnedOffsets();
    this.markTruncatedCells();
  }

  applyStickyPinnedOffsets() {
    let currentTop = this.elements.head.offsetHeight;
    const pinnedTrs = this.elements.body.querySelectorAll("tr.pinned");

    pinnedTrs.forEach((tr) => {
      tr.style.position = "sticky";
      tr.style.top = `${currentTop}px`;
      tr.style.zIndex = "2";
      currentTop += tr.offsetHeight;
    });
  }

  markTruncatedCells() {
    this.elements.body.querySelectorAll(".cell-content").forEach((content) => {
      content.parentElement.classList.toggle("expandable", content.scrollHeight > content.clientHeight);
    });
  }
}
