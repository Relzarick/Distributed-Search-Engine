class ResultsGrid {
  constructor(state, elements, onReorder) {
    this.state = state;
    this.elements = elements; // { wrapper, head, body }
    this.onReorder = onReorder;

    // Header Drag & Drop Listeners
    this.elements.head.addEventListener("dragstart", (e) => this.handleHeaderDragStart(e));
    this.elements.head.addEventListener("dragover", (e) => e.preventDefault());
    this.elements.head.addEventListener("drop", (e) => this.handleHeaderDrop(e));

    // Cell Expansion Delegate
    this.elements.body.addEventListener("click", (e) => {
      const td = e.target.closest("td.expandable");
      if (td) td.querySelector(".cell-content").classList.toggle("expanded");
    });
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

    // Move dragged item directly to target index regardless of drag direction
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

    this.elements.body.innerHTML = this.state.data
      .map(
        (row) => `
        <tr>
          ${visible.map((h) => `<td><div class="cell-content">${row[h] ?? ""}</div></td>`).join("")}
        </tr>`,
      )
      .join("");

    this.markTruncatedCells();
  }

  markTruncatedCells() {
    this.elements.body.querySelectorAll(".cell-content").forEach((content) => {
      content.parentElement.classList.toggle("expandable", content.scrollHeight > content.clientHeight);
    });
  }
}
