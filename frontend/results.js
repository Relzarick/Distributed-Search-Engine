export class ResultsGrid {
  constructor(elements, { onHeaderReorder, onHeaderAutoFit, onRowCopy }) {
    this.elements = elements;
    this.onHeaderReorder = onHeaderReorder;
    this.onHeaderAutoFit = onHeaderAutoFit;
    this.onRowCopy = onRowCopy;
    this.pinnedRows = new Set();
    this.draggedHeader = null;
    this.draggedRowId = null;
    this.currentData = [];
    this.currentHeaders = [];
    this.currentVisibleHeaders = new Set();

    // Mobile / Touch detection
    this.isMobile = window.matchMedia("(pointer: coarse)").matches || "ontouchstart" in window;

    if (!this.isMobile) this.initCopyContainer();

    this.initEventListeners();
  }

  initCopyContainer() {
    const frame = this.elements.wrapper?.parentElement;
    if (!frame) return;

    this.copyContainer = frame.querySelector(".pin-copy-container");
    if (!this.copyContainer) {
      this.copyContainer = document.createElement("div");
      this.copyContainer.className = "pin-copy-container";
      frame.insertBefore(this.copyContainer, this.elements.wrapper);
    }
  }

  initEventListeners() {
    if (this.elements.head) {
      this.elements.head.addEventListener("dragstart", (e) => this.handleHeaderDragStart(e));
      this.elements.head.addEventListener("dragover", (e) => e.preventDefault());
      this.elements.head.addEventListener("drop", (e) => this.handleHeaderDrop(e));
    }

    if (this.elements.wrapper && !this.isMobile) {
      // Re-sync copy icon positions as the user scrolls unpinned rows
      this.elements.wrapper.addEventListener("scroll", () => this.applyRowOffsets());
    }

    if (this.elements.body) {
      this.elements.body.addEventListener("click", (e) => {
        const td = e.target.closest("td.expandable");
        if (td) td.querySelector(".cell-content")?.classList.toggle("expanded");
      });

      this.elements.body.addEventListener("dblclick", (e) => this.handleRowDblClick(e));
      this.elements.body.addEventListener("dragstart", (e) => this.handleRowDragStart(e));
      this.elements.body.addEventListener("dragover", (e) => {
        if (this.draggedRowId !== null) e.preventDefault();
      });
      this.elements.body.addEventListener("drop", (e) => this.handleRowDrop(e));

      if (this.isMobile) {
        this.initMobileTouchEvents();
      } else {
        // Hover synchronization for any row to its floating copy button
        this.elements.body.addEventListener("mouseover", (e) => {
          const tr = e.target.closest("tr");
          if (!tr) return;
          const item = this.getCopyItem(tr.dataset.rowId);
          if (item && item.style.display !== "none") item.classList.add("is-active");
        });

        this.elements.body.addEventListener("mouseout", (e) => {
          const tr = e.target.closest("tr");
          if (!tr) return;
          this.getCopyItem(tr.dataset.rowId)?.classList.remove("is-active");
        });
      }
    }

    if (this.copyContainer && !this.isMobile) {
      this.copyContainer.addEventListener("click", (e) => {
        const btn = e.target.closest(".btn-pin-copy");
        if (!btn) return;
        const targetRow = this.currentData.find((r) => String(r.id) === btn.dataset.rowId);
        if (targetRow && this.onRowCopy) this.onRowCopy(targetRow);
      });
    }
  }

  initMobileTouchEvents() {
    let touchTimer = null;
    let startX = 0;
    let startY = 0;
    let longPressTriggered = false;
    let lastTapTime = 0;
    let lastTapRowId = null;

    this.elements.body.addEventListener(
      "touchstart",
      (e) => {
        const tr = e.target.closest("tr");
        if (!tr) return;

        longPressTriggered = false;
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;

        touchTimer = setTimeout(() => {
          longPressTriggered = true;
          const targetItem = this.getRenderedRows().find((item) => String(item.row.id) === tr.dataset.rowId);
          if (targetItem && this.onRowCopy) {
            this.onRowCopy(targetItem.row);
            if (navigator.vibrate) navigator.vibrate(50);
          }
        }, 500);
      },
      { passive: true },
    );

    this.elements.body.addEventListener(
      "touchmove",
      (e) => {
        if (!touchTimer) return;
        const moveX = e.touches[0].clientX;
        const moveY = e.touches[0].clientY;
        if (Math.hypot(moveX - startX, moveY - startY) > 10) {
          clearTimeout(touchTimer);
          touchTimer = null;
        }
      },
      { passive: true },
    );

    this.elements.body.addEventListener("touchend", (e) => {
      const tr = e.target.closest("tr");

      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }

      if (longPressTriggered) {
        if (e.cancelable) e.preventDefault();
        lastTapTime = 0;
        return;
      }

      if (tr) {
        const now = Date.now();
        const currentRowId = tr.dataset.rowId;

        if (now - lastTapTime < 300 && lastTapRowId === currentRowId) {
          if (e.cancelable) e.preventDefault();
          this.togglePinRow(tr);
          lastTapTime = 0;
          lastTapRowId = null;
        } else {
          lastTapTime = now;
          lastTapRowId = currentRowId;
        }
      }
    });

    this.elements.body.addEventListener("touchcancel", () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    });
  }

  togglePinRow(tr) {
    if (!tr) return;

    const targetItem = this.getRenderedRows().find((item) => String(item.row.id) === tr.dataset.rowId);
    if (!targetItem) return;

    if (this.pinnedRows.has(targetItem.row)) {
      this.pinnedRows.delete(targetItem.row);
      this.rerender();
      return;
    }

    const headerHeight = this.elements.head?.offsetHeight || 0;
    const currentPinnedHeight = Array.from(this.elements.body?.querySelectorAll("tr.pinned") || []).reduce(
      (sum, pTr) => sum + pTr.offsetHeight,
      0,
    );

    const wrapperHeight = this.elements.wrapper?.clientHeight || 0;
    if (headerHeight + currentPinnedHeight + tr.offsetHeight >= wrapperHeight) return;

    this.pinnedRows.add(targetItem.row);
    this.rerender();
  }

  handleRowDblClick(e) {
    const tr = e.target.closest("tr");
    this.togglePinRow(tr);
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

  handleRowDragStart(e) {
    const handle = e.target.closest("[data-pin-handle]");
    if (!handle) {
      e.preventDefault();
      return;
    }
    const tr = handle.closest("tr.pinned");
    if (!tr) return;

    this.draggedRowId = tr.dataset.rowId;
    e.dataTransfer.effectAllowed = "move";
  }

  handleRowDrop(e) {
    const tr = e.target.closest("tr.pinned");
    if (!tr || this.draggedRowId === null) return;
    e.preventDefault();

    const targetId = tr.dataset.rowId;
    if (targetId === this.draggedRowId) {
      this.draggedRowId = null;
      return;
    }

    const pinnedArray = [...this.pinnedRows];
    const fromIdx = pinnedArray.findIndex((row) => String(row.id) === this.draggedRowId);
    const toIdx = pinnedArray.findIndex((row) => String(row.id) === targetId);
    this.draggedRowId = null;

    if (fromIdx === -1 || toIdx === -1 || fromIdx === toIdx) return;

    const [moved] = pinnedArray.splice(fromIdx, 1);
    pinnedArray.splice(toIdx, 0, moved);

    this.pinnedRows = new Set(pinnedArray);
    this.rerender();
  }

  getVisibleHeaders(headers, visibleHeaders) {
    return headers.filter((h) => visibleHeaders.has(h));
  }

  renderHeadersOnly(visible) {
    if (!this.elements.head) return;
    this.elements.head.innerHTML = `<tr>${visible.map((h) => `<th draggable="true" data-header="${h}" class="draggable-handle">${h.replace(/_/g, " ")}</th>`).join("")}</tr>`;
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

  getCopyItem(rowId) {
    return this.copyContainer?.querySelector(`.pin-copy-item[data-row-id="${rowId}"]`) || null;
  }

  rerender() {
    this.render(this.currentData, this.currentHeaders, this.currentVisibleHeaders);
  }

  render(data = [], headers = [], visibleHeaders = new Set()) {
    this.currentData = data;
    this.currentHeaders = headers;
    this.currentVisibleHeaders = visibleHeaders;

    const visible = this.getVisibleHeaders(headers, visibleHeaders);
    this.renderHeadersOnly(visible);

    if (this.elements.body) {
      this.elements.body.innerHTML = this.getRenderedRows()
        .map(({ row, isPinned }) => {
          const cellsHtml = visible
            .map((h, i) => {
              const isHandle = isPinned && i === 0;
              const handleAttrs = isHandle ? ` draggable="true" data-pin-handle class="pin-cell draggable-handle"` : "";
              return `<td${handleAttrs}><div class="cell-content">${row[h] ?? ""}</div></td>`;
            })
            .join("");

          return `<tr data-row-id="${row.id}" class="${isPinned ? "pinned" : ""}">${cellsHtml}</tr>`;
        })
        .join("");
    }

    if (!this.isMobile) {
      this.renderCopyContainer();
    }
    this.applyRowOffsets();
    this.markTruncatedCells();
  }

  renderCopyContainer() {
    if (!this.copyContainer) return;
    this.copyContainer.innerHTML = this.getRenderedRows()
      .map(
        ({ row }) => `
        <div class="pin-copy-item" data-row-id="${row.id}">
          <button type="button" class="btn-pin-copy" data-row-id="${row.id}" aria-label="Copy row">
            <span class="icon icon-copy" aria-hidden="true"></span>
          </button>
        </div>`,
      )
      .join("");
  }

  applyRowOffsets() {
    if (!this.elements.head || !this.elements.body || !this.elements.wrapper) return;
    let currentTop = this.elements.head.offsetHeight;

    // Sticky offsets for pinned rows inside the table
    this.elements.body.querySelectorAll("tr.pinned").forEach((tr) => {
      Object.assign(tr.style, { position: "sticky", top: `${currentTop}px`, zIndex: "2" });
      currentTop += tr.offsetHeight;
    });

    if (this.isMobile || !this.copyContainer) return;

    const wrapperRect = this.elements.wrapper.getBoundingClientRect();
    const frameRect = this.elements.wrapper.parentElement.getBoundingClientRect();

    // Bounds check: lower limit is sticky headers/pinned rows, upper limit is bottom of wrapper
    const visibleTopLimit = wrapperRect.top + currentTop;
    const visibleBottomLimit = wrapperRect.bottom;

    this.elements.body.querySelectorAll("tr").forEach((tr) => {
      const copyItem = this.getCopyItem(tr.dataset.rowId);
      if (!copyItem) return;

      const trRect = tr.getBoundingClientRect();
      const top = trRect.top - frameRect.top;
      const height = trRect.height;

      copyItem.style.top = `${top}px`;
      copyItem.style.height = `${height}px`;

      // Check if row is currently visible inside the scroll area
      const isRowVisible = trRect.bottom > visibleTopLimit && trRect.top < visibleBottomLimit;

      if (!isRowVisible) {
        copyItem.classList.remove("is-active");
        copyItem.style.display = "none";
      } else {
        copyItem.style.display = "";
      }
    });
  }

  markTruncatedCells() {
    if (!this.elements.body) return;
    requestAnimationFrame(() => {
      this.elements.body.querySelectorAll(".cell-content").forEach((content) => {
        content.closest("td")?.classList.toggle("expandable", content.scrollHeight > content.clientHeight);
      });
    });
  }
}
