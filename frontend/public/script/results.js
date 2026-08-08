class TableRenderer {
  static getVisibleHeaders(headers, visibleHeaders) {
    return headers.filter((h) => visibleHeaders.has(h));
  }

  static renderHeaders(headEl, visibleHeaders) {
    if (!headEl) return;
    headEl.innerHTML = `<tr>${visibleHeaders
      .map((h) => `<th draggable="true" data-header="${h}" class="draggable-handle">${h.replace(/_/g, " ")}</th>`)
      .join("")}</tr>`;
  }

  static renderBody(bodyEl, orderedRows, visibleHeaders, getRowKey, isPinnedKey) {
    if (!bodyEl) return;
    bodyEl.innerHTML = orderedRows
      .map((row) => {
        const rowId = getRowKey(row);
        const isPinned = isPinnedKey(rowId);
        const cellsHtml = visibleHeaders
          .map((h, i) => {
            const isHandle = isPinned && i === 0;
            const handleAttrs = isHandle ? ` draggable="true" data-pin-handle class="pin-cell draggable-handle"` : "";
            return `<td${handleAttrs}><div class="cell-content">${row[h] ?? ""}</div></td>`;
          })
          .join("");

        return `<tr data-row-id="${rowId}" class="${isPinned ? "pinned" : ""}">${cellsHtml}</tr>`;
      })
      .join("");
  }

  static applyRowOffsets(headEl, bodyEl) {
    if (!headEl || !bodyEl) return;
    let currentTop = headEl.offsetHeight;

    bodyEl.querySelectorAll("tr.pinned").forEach((tr) => {
      for (const td of tr.children) {
        Object.assign(td.style, { position: "sticky", top: `${currentTop}px`, zIndex: "2" });
      }
      currentTop += tr.offsetHeight;
    });
  }

  static clearRowOffsets(tr) {
    for (const td of tr.children) {
      td.style.position = "";
      td.style.top = "";
      td.style.zIndex = "";
    }
  }

  static removeStaleDOMRows(bodyEl, validKeys) {
    if (!bodyEl) return;
    Array.from(bodyEl.querySelectorAll("tr")).forEach((tr) => {
      if (!validKeys.has(tr.dataset.rowId)) tr.remove();
    });
  }

  static syncDOMOrder(bodyEl, orderedRows, getRowKey) {
    if (!bodyEl) return;
    const fragment = document.createDocumentFragment();
    for (const row of orderedRows) {
      const tr = bodyEl.querySelector(`tr[data-row-id="${getRowKey(row)}"]`);
      if (tr) fragment.appendChild(tr);
    }
    bodyEl.appendChild(fragment);
  }

  static markTruncatedCells(bodyEl) {
    if (!bodyEl) return;
    requestAnimationFrame(() => {
      const cells = Array.from(bodyEl.querySelectorAll(".cell-content"));
      const isTruncated = cells.map((content) => content.scrollHeight > content.clientHeight);

      cells.forEach((content, i) => {
        content.closest("td")?.classList.toggle("expandable", isTruncated[i]);
      });
    });
  }
}

class TableEvents {
  constructor(tableInstance) {
    this.table = tableInstance;
    this.draggedHeader = null;
    this.draggedRowId = null;
  }

  init() {
    const { head, body, wrapper } = this.table.elements;

    if (head) {
      head.addEventListener("dragstart", (e) => this.handleHeaderDragStart(e));
      head.addEventListener("dragover", (e) => e.preventDefault());
      head.addEventListener("drop", (e) => this.handleHeaderDrop(e));
      if (this.table.isMobile) this.initMobileHeaderTouch();
    }

    if (wrapper && !this.table.isMobile) {
      wrapper.addEventListener(
        "scroll",
        () => {
          if (this.table.roamingBtn) this.table.roamingBtn.classList.remove("is-active");
          TableRenderer.applyRowOffsets(head, body);
        },
        { passive: true },
      );
    }

    if (body) {
      body.addEventListener("click", (e) => {
        const td = e.target.closest("td.expandable");
        td?.querySelector(".cell-content")?.classList.toggle("expanded");
      });

      body.addEventListener("dblclick", (e) => {
        const tr = e.target.closest("tr");
        if (tr) this.table.togglePinRow(tr);
      });

      body.addEventListener("dragstart", (e) => this.handleRowDragStart(e));
      body.addEventListener("dragover", (e) => {
        if (this.draggedRowId !== null) e.preventDefault();
      });
      body.addEventListener("drop", (e) => this.handleRowDrop(e));

      if (this.table.isMobile) {
        this.initMobileRowTouch();
      } else {
        this.initDesktopHoverAndCopy();
      }
    }
  }

  handleHeaderDragStart(e) {
    const th = e.target.closest("th");
    if (!th) return;
    this.draggedHeader = th.dataset.header;
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", this.draggedHeader);
  }

  handleHeaderDrop(e) {
    e.preventDefault();
    const th = e.target.closest("th");
    if (!th || !this.draggedHeader) return;

    this.table.reorderHeader(this.draggedHeader, th.dataset.header);
    this.draggedHeader = null;
  }

  handleRowDragStart(e) {
    const handle = e.target.closest("[data-pin-handle]");
    const tr = handle?.closest("tr.pinned");
    if (!tr) return e.preventDefault();

    this.draggedRowId = tr.dataset.rowId;
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", this.draggedRowId);
  }

  handleRowDrop(e) {
    const tr = e.target.closest("tr.pinned");
    if (!tr || this.draggedRowId === null) return;
    e.preventDefault();

    this.table.reorderPinnedRow(this.draggedRowId, tr.dataset.rowId);
    this.draggedRowId = null;
  }

  initDesktopHoverAndCopy() {
    const { head, body, wrapper } = this.table.elements;

    const showRoamingBtn = (tr) => {
      if (!this.table.roamingBtn || !wrapper || !tr.dataset.rowId) {
        if (this.table.roamingBtn) this.table.roamingBtn.classList.remove("is-active");
        return;
      }

      const frameRect = wrapper.parentElement.getBoundingClientRect();
      const trRect = tr.getBoundingClientRect();
      const wrapperRect = wrapper.getBoundingClientRect();
      const headHeight = head?.offsetHeight || 0;
      const isPinned = tr.classList.contains("pinned");

      let topLimit = wrapperRect.top + headHeight;
      if (!isPinned) {
        topLimit += this.table.getPinnedRowsHeight();
      }

      if (trRect.top < topLimit - 1 || trRect.bottom > wrapperRect.bottom + 1) {
        this.table.roamingBtn.classList.remove("is-active");
        return;
      }

      this.table.roamingBtn.style.top = `${trRect.top - frameRect.top}px`;
      this.table.roamingBtn.style.height = `${trRect.height}px`;

      const btn = this.table.roamingBtn.querySelector(".btn-pin-copy");
      if (btn) btn.dataset.rowId = tr.dataset.rowId;

      this.table.roamingBtn.classList.add("is-active");
    };

    body.addEventListener("mousemove", (e) => {
      const tr = e.target.closest("tr");
      if (!tr || !tr.dataset.rowId) {
        if (this.table.roamingBtn) this.table.roamingBtn.classList.remove("is-active");
        return;
      }
      showRoamingBtn(tr);
    });

    wrapper.parentElement?.addEventListener("mouseleave", () => {
      if (this.table.roamingBtn) this.table.roamingBtn.classList.remove("is-active");
    });

    if (this.table.copyContainer) {
      this.table.copyContainer.addEventListener("click", (e) => {
        const btn = e.target.closest(".btn-pin-copy");
        if (!btn) return;
        const targetRow = this.table.idToRowMap.get(btn.dataset.rowId);
        if (targetRow) this.table.onRowCopy?.(targetRow);
      });
    }
  }

  initMobileHeaderTouch() {
    const { head } = this.table.elements;
    let activeHeader = null,
      startX = 0,
      isDragging = false;

    head.addEventListener(
      "touchstart",
      (e) => {
        const th = e.target.closest("th");
        if (!th) return;
        activeHeader = th.dataset.header;
        isDragging = false;
        startX = e.touches[0].clientX;
      },
      { passive: true },
    );

    head.addEventListener(
      "touchmove",
      (e) => {
        if (!activeHeader) return;
        if (Math.abs(e.touches[0].clientX - startX) > 5) {
          isDragging = true;
          if (e.cancelable) e.preventDefault();
        }
      },
      { passive: false },
    );

    head.addEventListener("touchend", (e) => {
      if (activeHeader && isDragging) {
        const touch = e.changedTouches[0];
        const targetTh = document.elementFromPoint(touch.clientX, touch.clientY)?.closest("th");
        if (targetTh && targetTh.dataset.header !== activeHeader) {
          this.table.reorderHeader(activeHeader, targetTh.dataset.header);
        }
      }
      activeHeader = null;
      isDragging = false;
    });

    head.addEventListener("touchcancel", () => {
      activeHeader = null;
      isDragging = false;
    });
  }

  initMobileRowTouch() {
    let touchTimer = null,
      startX = 0,
      startY = 0,
      longPressTriggered = false;
    let lastTapTime = 0,
      lastTapRowId = null,
      activeDragRowId = null,
      isDraggingRow = false;

    const clearTimer = () => {
      if (touchTimer) {
        clearTimeout(touchTimer);
        touchTimer = null;
      }
    };

    this.table.elements.body.addEventListener(
      "touchstart",
      (e) => {
        const tr = e.target.closest("tr");
        if (!tr) return;

        if (e.target.closest("[data-pin-handle]") && tr.classList.contains("pinned")) {
          activeDragRowId = tr.dataset.rowId;
          isDraggingRow = false;
          startX = e.touches[0].clientX;
          startY = e.touches[0].clientY;
          return;
        }

        longPressTriggered = false;
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;

        touchTimer = setTimeout(() => {
          longPressTriggered = true;
          const targetRow = this.table.idToRowMap.get(tr.dataset.rowId);
          if (targetRow) {
            this.table.onRowCopy?.(targetRow);
            if (navigator.vibrate) navigator.vibrate(50);
          }
        }, 500);
      },
      { passive: true },
    );

    this.table.elements.body.addEventListener(
      "touchmove",
      (e) => {
        if (activeDragRowId !== null) {
          if (Math.abs(e.touches[0].clientY - startY) > 5) {
            isDraggingRow = true;
            if (e.cancelable) e.preventDefault();
          }
          return;
        }
        if (touchTimer && Math.hypot(e.touches[0].clientX - startX, e.touches[0].clientY - startY) > 10) clearTimer();
      },
      { passive: false },
    );

    this.table.elements.body.addEventListener("touchend", (e) => {
      if (activeDragRowId !== null) {
        if (isDraggingRow) {
          const touch = e.changedTouches[0];
          const targetTr = document.elementFromPoint(touch.clientX, touch.clientY)?.closest("tr.pinned");
          if (targetTr && targetTr.dataset.rowId !== activeDragRowId) {
            this.table.reorderPinnedRow(activeDragRowId, targetTr.dataset.rowId);
          }
        }
        activeDragRowId = null;
        isDraggingRow = false;
        return;
      }

      const tr = e.target.closest("tr");
      clearTimer();

      if (longPressTriggered) {
        if (e.cancelable) e.preventDefault();
        lastTapTime = 0;
        return;
      }

      if (!tr) return;
      const now = Date.now();
      const currentRowId = tr.dataset.rowId;

      if (now - lastTapTime < 300 && lastTapRowId === currentRowId) {
        if (e.cancelable) e.preventDefault();
        this.table.togglePinRow(tr);
        lastTapTime = 0;
        lastTapRowId = null;
      } else {
        lastTapTime = now;
        lastTapRowId = currentRowId;
      }
    });

    this.table.elements.body.addEventListener("touchcancel", () => {
      clearTimer();
      activeDragRowId = null;
      isDraggingRow = false;
    });
  }
}

export class ResultsTable {
  constructor(elements, { onHeaderReorder, onHeaderAutoFit, onRowCopy }) {
    this.elements = elements;
    this.onHeaderReorder = onHeaderReorder;
    this.onHeaderAutoFit = onHeaderAutoFit;
    this.onRowCopy = onRowCopy;

    this.pinnedRows = new Map();
    this.currentData = [];
    this.currentHeaders = [];

    this.idToRowMap = new Map();

    this.isMobile = window.matchMedia("(pointer: coarse)").matches || "ontouchstart" in window;

    if (!this.isMobile) this.initCopyContainer();

    this.eventHandler = new TableEvents(this);
    this.eventHandler.init();
  }

  getRowKey(row) {
    const key = `id:${row._id}`;
    this.idToRowMap.set(key, row);
    return key;
  }

  moveItem(array, fromIdx, toIdx) {
    if (fromIdx === -1 || toIdx === -1 || fromIdx === toIdx) return false;
    array.splice(toIdx, 0, array.splice(fromIdx, 1)[0]);
    return true;
  }

  reorderHeader(fromHeader, toHeader) {
    const headers = [...this.currentHeaders];
    if (this.moveItem(headers, headers.indexOf(fromHeader), headers.indexOf(toHeader))) {
      this.onHeaderReorder(headers);
    }
  }

  reorderPinnedRow(fromRowId, toRowId) {
    const pinnedArray = [...this.pinnedRows.values()];
    const fromIdx = pinnedArray.findIndex((r) => this.getRowKey(r) === fromRowId);
    const toIdx = pinnedArray.findIndex((r) => this.getRowKey(r) === toRowId);

    if (!this.moveItem(pinnedArray, fromIdx, toIdx)) return;

    this.pinnedRows = new Map(pinnedArray.map((r) => [this.getRowKey(r), r]));
    this.reorderDOMRows();
    TableRenderer.applyRowOffsets(this.elements.head, this.elements.body);
  }

  getPinnedRowsHeight() {
    const pinnedTrs = [...(this.elements.body?.querySelectorAll("tr.pinned") || [])];
    return pinnedTrs.reduce((sum, tr) => sum + tr.offsetHeight, 0);
  }

  getOrderedRows() {
    const pinnedKeys = new Set(this.pinnedRows.keys());
    const unpinned = this.currentData.filter((row) => !pinnedKeys.has(this.getRowKey(row)));
    return [...this.pinnedRows.values(), ...unpinned];
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

    if (!this.copyContainer.querySelector("#roaming-copy-btn")) {
      this.copyContainer.innerHTML = `
        <div class="pin-copy-item roaming-copy-btn" id="roaming-copy-btn">
          <button type="button" class="btn-pin-copy" aria-label="Copy row">
            <span class="icon icon-lg icon-copy" aria-hidden="true"></span>
          </button>
        </div>
      `;
    }
    this.roamingBtn = this.copyContainer.querySelector("#roaming-copy-btn");
  }

  togglePinRow(tr) {
    if (!tr) return;
    const key = tr.dataset.rowId;
    const targetRow = this.idToRowMap.get(key);
    if (!targetRow) return;

    const isPinned = this.pinnedRows.has(key);

    if (isPinned) {
      this.pinnedRows.delete(key);
      TableRenderer.clearRowOffsets(tr);
    } else {
      const headerHeight = this.elements.head?.offsetHeight || 0;
      const currentPinnedHeight = this.getPinnedRowsHeight();
      const wrapperHeight = this.elements.wrapper?.clientHeight || 0;

      if (headerHeight + currentPinnedHeight + tr.offsetHeight >= wrapperHeight) return;

      this.pinnedRows.set(key, targetRow);
    }

    tr.classList.toggle("pinned", !isPinned);

    const firstTd = tr.querySelector("td");
    if (firstTd) {
      firstTd.classList.toggle("pin-cell", !isPinned);
      firstTd.classList.toggle("draggable-handle", !isPinned);
      firstTd.toggleAttribute("data-pin-handle", !isPinned);
      if (!isPinned) firstTd.setAttribute("draggable", "true");
      else firstTd.removeAttribute("draggable");
    }

    this.reorderDOMRows();
    TableRenderer.applyRowOffsets(this.elements.head, this.elements.body);
  }

  reorderDOMRows() {
    const orderedRows = this.getOrderedRows();
    const validKeys = new Set(orderedRows.map((row) => this.getRowKey(row)));

    TableRenderer.removeStaleDOMRows(this.elements.body, validKeys);
    TableRenderer.syncDOMOrder(this.elements.body, orderedRows, (r) => this.getRowKey(r));
  }

  fitHeadersToWidth(headers, visibleHeaders) {
    const { wrapper } = this.elements;
    if (!wrapper || wrapper.clientWidth === 0) return;

    const visibleSet = new Set(visibleHeaders);
    const fullData = this.currentData;
    const calcData = fullData.slice(0, 5);

    this.render(calcData, headers, visibleSet);

    let changed = false;
    while (wrapper.clientWidth > 0 && wrapper.scrollWidth > wrapper.clientWidth && visibleSet.size > 1) {
      visibleSet.delete([...visibleSet].pop());
      this.render(calcData, headers, visibleSet);
      changed = true;
    }

    this.render(fullData, headers, visibleSet);
    if (changed) this.onHeaderAutoFit(visibleSet);
  }

  render(data = [], headers = [], visibleHeaders = new Set()) {
    this.currentData = data;
    this.currentHeaders = headers;

    const visible = TableRenderer.getVisibleHeaders(headers, visibleHeaders);
    TableRenderer.renderHeaders(this.elements.head, visible);

    if (data.length === 0) {
      const colSpan = Math.max(1, visible.length);
      this.elements.body.innerHTML = `<tr><td colspan="${colSpan}" class="no-results">No matching records found</td></tr>`;
      return;
    }

    const orderedRows = this.getOrderedRows();
    TableRenderer.renderBody(
      this.elements.body,
      orderedRows,
      visible,
      (r) => this.getRowKey(r),
      (key) => this.pinnedRows.has(key),
    );

    TableRenderer.applyRowOffsets(this.elements.head, this.elements.body);
    TableRenderer.markTruncatedCells(this.elements.body);
  }

  applyRowOffsets() {
    TableRenderer.applyRowOffsets(this.elements.head, this.elements.body);
  }

  markTruncatedCells() {
    TableRenderer.markTruncatedCells(this.elements.body);
  }
}
