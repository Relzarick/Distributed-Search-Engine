class ResultsGrid {
  constructor(state, elements, onReorder) {
    this.state = state;
    this.elements = elements; // { wrapper, head, body }
    this.onReorder = onReorder;

    this.elements.head.addEventListener("dragstart", (e) =>
      this.handleHeaderDragStart(e),
    );
    this.elements.head.addEventListener("dragover", (e) => e.preventDefault());
    this.elements.head.addEventListener("drop", (e) =>
      this.handleHeaderDrop(e),
    );

    this.elements.body.addEventListener("click", (e) => {
      const td = e.target.closest("td.expandable");
      if (td) td.querySelector(".cell-content").classList.toggle("expanded");
    });
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
}
