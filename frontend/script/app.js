import { SearchBar } from "./search.js";
import { ResultsTable } from "./results.js";
import { ColumnFilter, ValueFilter } from "./filter.js";
import { Pagination } from "./pagination.js";
import { StatusNotifier, NotificationSource } from "./notifier.js";

export class SearchService {
  constructor(baseUrl = "/search") {
    this.baseUrl = baseUrl;
    this.activeController = null;
  }

  async search(query = "", page = 0, limit = 50) {
    this.activeController?.abort();
    this.activeController = new AbortController();

    const params = new URLSearchParams({ q: query, page, limit });

    try {
      const res = await fetch(`${this.baseUrl}?${params}`, { signal: this.activeController.signal });
      if (!res.ok) throw new Error(`Search request failed with status: ${res.status}`);

      const json = await res.json();
      this.activeController = null;

      if (Array.isArray(json)) return { items: json, totalPages: null };

      return {
        items: json.items || json.data || [],
        totalPages: typeof json.totalPages === "number" ? json.totalPages : null,
      };
    } catch (err) {
      if (err.name === "AbortError") return { aborted: true, items: [], totalPages: null };
      this.activeController = null;
      throw err;
    }
  }
}

const createInitialState = () => ({
  data: [],
  headers: [],
  visibleHeaders: new Set(),
  activeFilterHeaders: new Set(),
  lastQuery: null,
  page: 0,
  pageSize: 50,
  totalPages: 5,
});

const $ = (selector) => document.querySelector(selector);

export class SearchApp {
  constructor(searchService = new SearchService()) {
    this.searchService = searchService;
    this.state = createInitialState();

    this.container = $("[data-table-container]");
    this.tableFrame = $("[data-table-frame]");
    this.paginationContainer = $(".pagination-container");
    this.fullscreenBtn = $(".btn-fullscreen");
    this.headerNotice = $(".header-notice");

    this.notifier = new StatusNotifier($("[data-toolbar-status]"));

    this.searchBar = new SearchBar($(".search-form"), $('input[name="query"]'), (query) => this.handleSearch(query));

    this.grid = new ResultsTable(
      {
        wrapper: $(".table-wrapper"),
        head: $("[data-table-head]"),
        body: $("[data-table-body]"),
      },
      {
        onHeaderReorder: (headers) => this.handleHeaderReorder(headers),
        onHeaderAutoFit: (visibleSet) => this.handleHeaderAutoFit(visibleSet),
        onRowCopy: (row) => {
          navigator.clipboard.writeText(JSON.stringify(row, null, 2));
          this.notifier.notify(NotificationSource.ROW_COPY);
        },
      },
    );

    this.columnFilter = new ColumnFilter(
      {
        btn: $(".btn-filter"),
        menu: $(".filter-menu"),
        selectAll: $(".filter-select-all input"),
        options: $(".filter-options"),
        count: $(".filter-count"),
        alignTo: $(".table-frame"),
      },
      {
        onToggle: (header, checked) => this.handleHeaderToggle(header, checked),
        onToggleAll: (checked) => this.handleToggleAllHeaders(checked),
      },
    );

    this.valueFilter = new ValueFilter({
      btn: $('[popovertarget="value-menu"]'),
      menu: $(".value-menu"),
      alignTo: $(".table-frame"),
    });

    this.valueFilter.elements.menu?.addEventListener("value-filter-change", (e) => {
      this.handleValueFilterChange(e.detail.header, e.detail.filter);
    });

    this.pagination = new Pagination(
      this.paginationContainer,
      (page) => this.handlePageChange(page),
      this.state.totalPages,
    );

    if (this.fullscreenBtn) this.fullscreenBtn.addEventListener("click", () => this.toggleFullscreen());
  }

  toggleFullscreen() {
    if (!this.container) return;
    const isFullscreen = this.container.classList.toggle("is-fullscreen");
    if (this.fullscreenBtn) this.fullscreenBtn.setAttribute("aria-pressed", String(isFullscreen));
    document.body.classList.toggle("no-scroll", isFullscreen);

    requestAnimationFrame(() => {
      this.grid.applyRowOffsets();
      this.grid.markTruncatedCells();
    });
  }

  async handleSearch(query) {
    if (this.headerNotice) this.headerNotice.classList.add("is-hidden");
    this.state.lastQuery = query;
    this.state.page = 0;
    await this.fetchData();
  }

  async handlePageChange(uiPage) {
    this.state.page = uiPage - 1;
    await this.fetchData();
  }

  handleHeaderToggle(header, isChecked) {
    isChecked ? this.state.visibleHeaders.add(header) : this.state.visibleHeaders.delete(header);
    this.finishRender();
  }

  handleToggleAllHeaders(isChecked) {
    this.state.visibleHeaders = isChecked ? new Set(this.state.headers) : new Set();
    this.finishRender();
  }

  handleValueFilterChange(header, filter) {
    filter ? this.state.activeFilterHeaders.add(header) : this.state.activeFilterHeaders.delete(header);
    this.renderGrid();
  }

  handleHeaderReorder(newHeaders) {
    this.state.headers = newHeaders;
    this.finishRender();
  }

  handleHeaderAutoFit(updatedVisibleSet) {
    this.state.visibleHeaders = updatedVisibleSet;
    this.columnFilter.render(this.state.headers, this.state.visibleHeaders);
  }

  async fetchData() {
    if (this.state.lastQuery === null) return;

    this.searchBar.setLoading(true);

    try {
      const response = await this.searchService.search(this.state.lastQuery, this.state.page, this.state.pageSize);
      if (!response.aborted) this.processSearchResults(response);
    } catch (error) {
      console.error("Search failed:", error);
    } finally {
      this.searchBar.setLoading(false);
    }
  }

  detectSchema(headers, sampleRow) {
    if (!sampleRow) return new Map();
    const schema = new Map();

    headers.forEach((header) => {
      const val = sampleRow[header];
      let type = "string";

      if (typeof val === "number") type = "number";
      else if (val instanceof Date) type = "date";
      else if (
        typeof val === "string" &&
        isNaN(Number(val)) &&
        !isNaN(Date.parse(val)) &&
        (val.includes("-") || val.includes("/"))
      )
        type = "date";

      schema.set(header, type);
    });

    return schema;
  }

  processSearchResults({ items, totalPages }) {
    this.state.data = items;
    if (totalPages !== null) this.state.totalPages = totalPages;

    const hasData = Boolean(this.state.data.length);
    if (this.container) this.container.hidden = !hasData;
    if (this.paginationContainer) this.paginationContainer.hidden = !hasData;
    if (!hasData) return;

    const incomingHeaders = Object.keys(items[0]).filter((k) => k !== "_id");

    if (!this.state.headers.length) {
      this.state.headers = incomingHeaders;
      this.state.visibleHeaders = new Set(incomingHeaders);
    } else {
      const preserved = this.state.headers.filter((h) => incomingHeaders.includes(h));
      const newHeaders = incomingHeaders.filter((h) => !this.state.headers.includes(h));

      this.state.headers = [...preserved, ...newHeaders];
      newHeaders.forEach((h) => this.state.visibleHeaders.add(h));
    }

    this.valueFilter.schema = this.detectSchema(this.state.headers, items[0]);

    this.finishRender();
    this.grid.fitHeadersToWidth(this.state.headers, this.state.visibleHeaders);
  }

  renderGrid() {
    const hasColumns = this.state.visibleHeaders.size > 0;
    if (!hasColumns) return;

    const filteredData = this.valueFilter.filterData(this.state.data);
    this.grid.render(filteredData, this.state.headers, this.state.visibleHeaders);
  }

  finishRender() {
    const hasColumns = this.state.visibleHeaders.size > 0;

    if (this.tableFrame) this.tableFrame.hidden = !hasColumns;
    if (this.paginationContainer) this.paginationContainer.hidden = !hasColumns;

    this.columnFilter.render(this.state.headers, this.state.visibleHeaders);
    this.valueFilter.render(this.state.headers);
    this.pagination.render(this.state.page + 1, this.state.totalPages);

    if (hasColumns) this.renderGrid();
  }
}

const startApp = () => new SearchApp();

if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", startApp);
else startApp();
