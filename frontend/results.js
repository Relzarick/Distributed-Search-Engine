export class ResultsTable {
  constructor(elements, { onHeaderReorder, onHeaderAutoFit, onRowCopy }) {
    this.elements = elements;
    this.onHeaderReorder = onHeaderReorder;
    this.onHeaderAutoFit = onHeaderAutoFit;
    this.onRowCopy = onRowCopy;

    this.pinnedRows = new Map();
    this.draggedHeader = null;
    this.draggedRowId = null;
    this.currentData = [];
    this.currentHeaders = [];
    this.currentVisibleHeaders = new Set();

    this.nextFallbackId = 0;
    this.fallbackIdMap = new WeakMap();
    this.idToRowMap = new Map();

    this.isMobile = window.matchMedia("(pointer: coarse)").matches || "ontouchstart" in window;

    if (!this.isMobile) this.initCopyContainer();
    this.initEventListeners();
  }

  getRowKey(row) {
    let key;
    if (row?._id !== undefined && row._id !== null) {
      key = `id:${row._id}`;
    } else {
      if (!this.fallbackIdMap.has(row)) this.fallbackIdMap.set(row, `tmp:${++this.nextFallbackId}`);
      key = this.fallbackIdMap.get(row);
    }
    this.idToRowMap.set(key, row);
    return key;
  }

  moveItem(array, fromIdx, toIdx) {
    if (fromIdx === -1 || toIdx === -1 || fromIdx === toIdx) return false;
    array.splice(toIdx, 0, array.splice(fromIdx, 1)[0]);
    return true;
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

  initEventListeners() {
    const { head, body, wrapper } = this.elements;

    if (head) {
      head.addEventListener("dragstart", (e) => this.handleHeaderDragStart(e));
      head.addEventListener("dragover", (e) => e.preventDefault());
      head.addEventListener("drop", (e) => this.handleHeaderDrop(e));
      if (this.isMobile) this.initMobileHeaderTouchEvents();
    }

    if (wrapper && !this.isMobile) {
      wrapper.addEventListener(
        "scroll",
        () => {
          if (this.roamingBtn) this.roamingBtn.classList.remove("is-active");
          this.applyRowOffsets();
        },
        { passive: true },
      );
    }

    if (body) {
      body.addEventListener("click", (e) => {
        const td = e.target.closest("td.expandable");
        td?.querySelector(".cell-content")?.classList.toggle("expanded");
      });

      body.addEventListener("dblclick", (e) => this.handleRowDblClick(e));
      body.addEventListener("dragstart", (e) => this.handleRowDragStart(e));
      body.addEventListener("dragover", (e) => {
        if (this.draggedRowId !== null) e.preventDefault();
      });
      body.addEventListener("drop", (e) => this.handleRowDrop(e));

      if (this.isMobile) {
        this.initMobileTouchEvents();
      } else {
        const showRoamingBtn = (tr) => {
          if (!this.roamingBtn || !wrapper) return;

          const frameRect = wrapper.parentElement.getBoundingClientRect();
          const trRect = tr.getBoundingClientRect();
          const wrapperRect = wrapper.getBoundingClientRect();

          const headHeight = head?.offsetHeight || 0;
          const isPinned = tr.classList.contains("pinned");

          let topLimit = wrapperRect.top + headHeight;
          if (!isPinned) {
            const pinnedTotalHeight = Array.from(body.querySelectorAll("tr.pinned")).reduce(
              (acc, r) => acc + r.offsetHeight,
              0,
            );
            topLimit += pinnedTotalHeight;
          }

          if (trRect.top < topLimit - 1 || trRect.bottom > wrapperRect.bottom + 1) {
            this.roamingBtn.classList.remove("is-active");
            return;
          }

          this.roamingBtn.style.top = `${trRect.top - frameRect.top}px`;
          this.roamingBtn.style.height = `${trRect.height}px`;

          const btn = this.roamingBtn.querySelector(".btn-pin-copy");
          if (btn) btn.dataset.rowId = tr.dataset.rowId;

          this.roamingBtn.classList.add("is-active");
        };

        body.addEventListener("mousemove", (e) => {
          const tr = e.target.closest("tr");
          if (tr) showRoamingBtn(tr);
        });

        wrapper.parentElement?.addEventListener("mouseleave", () => {
          if (this.roamingBtn) this.roamingBtn.classList.remove("is-active");
        });

        if (this.copyContainer) {
          this.copyContainer.addEventListener("click", (e) => {
            const btn = e.target.closest(".btn-pin-copy");
            if (!btn) return;
            const targetRow = this.idToRowMap.get(btn.dataset.rowId);
            if (targetRow) this.onRowCopy?.(targetRow);
          });
        }
      }
    }
  }

  initMobileHeaderTouchEvents() {
    const { head } = this.elements;
    if (!head) return;

    let activeHeader = null;
    let startX = 0,
      startY = 0;
    let isDraggingHeader = false;

    head.addEventListener(
      "touchstart",
      (e) => {
        const th = e.target.closest("th");
        if (!th) return;

        activeHeader = th.dataset.header;
        isDraggingHeader = false;
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
      },
      { passive: true },
    );

    head.addEventListener(
      "touchmove",
      (e) => {
        if (!activeHeader) return;
        const moveX = Math.abs(e.touches[0].clientX - startX);
        if (moveX > 5) {
          isDraggingHeader = true;
          if (e.cancelable) e.preventDefault();
        }
      },
      { passive: false },
    );

    head.addEventListener("touchend", (e) => {
      if (!activeHeader) return;

      if (isDraggingHeader) {
        const touch = e.changedTouches[0];
        const targetEl = document.elementFromPoint(touch.clientX, touch.clientY);
        const targetTh = targetEl?.closest("th");

        if (targetTh && targetTh.dataset.header !== activeHeader) {
          const headers = [...this.currentHeaders];
          if (this.moveItem(headers, headers.indexOf(activeHeader), headers.indexOf(targetTh.dataset.header))) {
            this.onHeaderReorder(headers);
          }
        }
      }

      activeHeader = null;
      isDraggingHeader = false;
    });

    head.addEventListener("touchcancel", () => {
      activeHeader = null;
      isDraggingHeader = false;
    });
  }

  initMobileTouchEvents() {
    let touchTimer = null;
    let startX = 0,
      startY = 0;
    let longPressTriggered = false;
    let lastTapTime = 0,
      lastTapRowId = null;
    let activeDragRowId = null;
    let isDraggingRow = false;

    const clearTimer = () => {
      if (!touchTimer) return;
      clearTimeout(touchTimer);
      touchTimer = null;
    };

    this.elements.body.addEventListener(
      "touchstart",
      (e) => {
        const tr = e.target.closest("tr");
        if (!tr) return;

        const pinHandle = e.target.closest("[data-pin-handle]");
        if (pinHandle && tr.classList.contains("pinned")) {
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
          const targetRow = this.idToRowMap.get(tr.dataset.rowId);
          if (targetRow) {
            this.onRowCopy?.(targetRow);
            if (navigator.vibrate) navigator.vibrate(50);
          }
        }, 500);
      },
      { passive: true },
    );

    this.elements.body.addEventListener(
      "touchmove",
      (e) => {
        if (activeDragRowId !== null) {
          const moveY = Math.abs(e.touches[0].clientY - startY);
          if (moveY > 5) {
            isDraggingRow = true;
            if (e.cancelable) e.preventDefault();
          }
          return;
        }

        if (!touchTimer) return;
        if (Math.hypot(e.touches[0].clientX - startX, e.touches[0].clientY - startY) > 10) clearTimer();
      },
      { passive: false },
    );

    this.elements.body.addEventListener("touchend", (e) => {
      if (activeDragRowId !== null) {
        if (isDraggingRow) {
          const touch = e.changedTouches[0];
          const targetEl = document.elementFromPoint(touch.clientX, touch.clientY);
          const targetTr = targetEl?.closest("tr.pinned");

          if (targetTr && targetTr.dataset.rowId !== activeDragRowId) {
            const pinnedArray = [...this.pinnedRows.values()];
            const fromIdx = pinnedArray.findIndex((row) => this.getRowKey(row) === activeDragRowId);
            const toIdx = pinnedArray.findIndex((row) => this.getRowKey(row) === targetTr.dataset.rowId);

            if (this.moveItem(pinnedArray, fromIdx, toIdx)) {
              this.pinnedRows = new Map(pinnedArray.map((row) => [this.getRowKey(row), row]));
              this.reorderDOMRows();
              this.applyRowOffsets();
            }
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
        this.togglePinRow(tr);
        lastTapTime = 0;
        lastTapRowId = null;
      } else {
        lastTapTime = now;
        lastTapRowId = currentRowId;
      }
    });

    this.elements.body.addEventListener("touchcancel", () => {
      clearTimer();
      activeDragRowId = null;
      isDraggingRow = false;
    });
  }

  togglePinRow(tr) {
    if (!tr) return;
    const key = tr.dataset.rowId;
    const targetRow = this.idToRowMap.get(key);
    if (!targetRow) return;

    const isPinned = this.pinnedRows.has(key);

    if (isPinned) {
      this.pinnedRows.delete(key);
      for (const td of tr.children) {
        td.style.position = "";
        td.style.top = "";
        td.style.zIndex = "";
      }
    } else {
      const headerHeight = this.elements.head?.offsetHeight || 0;
      const pinnedTrs = [...(this.elements.body?.querySelectorAll("tr.pinned") || [])];
      const currentPinnedHeight = pinnedTrs.reduce((sum, pTr) => sum + pTr.offsetHeight, 0);
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
    this.applyRowOffsets();
  }

  handleRowDblClick(e) {
    this.togglePinRow(e.target.closest("tr"));
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

    const headers = [...this.currentHeaders];
    const moved = this.moveItem(headers, headers.indexOf(this.draggedHeader), headers.indexOf(th.dataset.header));
    this.draggedHeader = null;

    if (moved) this.onHeaderReorder(headers);
  }

  handleRowDragStart(e) {
    const handle = e.target.closest("[data-pin-handle]");
    const tr = handle?.closest("tr.pinned");
    if (!tr) return e.preventDefault();

    this.draggedRowId = tr.dataset.rowId;
    e.dataTransfer.effectAllowed = "move";
  }

  handleRowDrop(e) {
    const tr = e.target.closest("tr.pinned");
    if (!tr || this.draggedRowId === null) return;
    e.preventDefault();

    const pinnedArray = [...this.pinnedRows.values()];
    const fromIdx = pinnedArray.findIndex((row) => this.getRowKey(row) === this.draggedRowId);
    const toIdx = pinnedArray.findIndex((row) => this.getRowKey(row) === tr.dataset.rowId);
    this.draggedRowId = null;

    if (!this.moveItem(pinnedArray, fromIdx, toIdx)) return;

    this.pinnedRows = new Map(pinnedArray.map((row) => [this.getRowKey(row), row]));
    this.reorderDOMRows();
    this.applyRowOffsets();
  }

  reorderDOMRows() {
    if (!this.elements.body) return;

    const orderedRows = this.getOrderedRows();
    const validKeys = new Set(orderedRows.map((row) => this.getRowKey(row)));

    Array.from(this.elements.body.querySelectorAll("tr")).forEach((tr) => {
      if (!validKeys.has(tr.dataset.rowId)) {
        tr.remove();
      }
    });

    const fragment = document.createDocumentFragment();
    for (const row of orderedRows) {
      const tr = this.elements.body.querySelector(`tr[data-row-id="${this.getRowKey(row)}"]`);
      if (tr) fragment.appendChild(tr);
    }

    this.elements.body.appendChild(fragment);
  }

  getVisibleHeaders(headers, visibleHeaders) {
    return headers.filter((h) => visibleHeaders.has(h));
  }

  renderHeadersOnly(visible) {
    if (!this.elements.head) return;
    this.elements.head.innerHTML = `<tr>${visible.map((h) => `<th draggable="true" data-header="${h}" class="draggable-handle">${h.replace(/_/g, " ")}</th>`).join("")}</tr>`;
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

  getRenderedRows() {
    return this.getOrderedRows().map((row) => ({ row, isPinned: this.pinnedRows.has(this.getRowKey(row)) }));
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
          const rowId = this.getRowKey(row);
          const cellsHtml = visible
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

    this.applyRowOffsets();
    this.markTruncatedCells();
  }

  applyRowOffsets() {
    const { head, body } = this.elements;
    if (!head || !body) return;

    let currentTop = head.offsetHeight;

    body.querySelectorAll("tr.pinned").forEach((tr) => {
      for (const td of tr.children)
        Object.assign(td.style, { position: "sticky", top: `${currentTop}px`, zIndex: "2" });
      currentTop += tr.offsetHeight;
    });
  }

  markTruncatedCells() {
    if (!this.elements.body) return;
    requestAnimationFrame(() => {
      const cells = Array.from(this.elements.body.querySelectorAll(".cell-content"));
      const isTruncated = cells.map((content) => content.scrollHeight > content.clientHeight);

      cells.forEach((content, i) => {
        content.closest("td")?.classList.toggle("expandable", isTruncated[i]);
      });
    });
  }
}
