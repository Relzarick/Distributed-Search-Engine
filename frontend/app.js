import { SearchBar } from "./search.js";
import { ResultsGrid } from "./results.js";
import { ColumnFilter } from "./filter.js";
import { Pagination } from "./pagination.js";

export class SearchService {
  constructor(baseUrl = "/search") {
    this.baseUrl = baseUrl;
    this.activeController = null;
  }

  async search(query = "", page = 0, limit = 50) {
    if (this.activeController) this.activeController.abort();

    this.activeController = new AbortController();
    const url = `${this.baseUrl}?q=${encodeURIComponent(query)}&page=${page}&limit=${limit}`;

    try {
      const res = await fetch(url, { signal: this.activeController.signal });
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
  lastQuery: null,
  page: 0,
  pageSize: 50,
  totalPages: 5,
});

export class SearchApp {
  constructor(searchService = new SearchService()) {
    this.searchService = searchService;
    this.state = createInitialState();

    this.container = document.querySelector("[data-table-container]");
    this.tableFrame = document.querySelector("[data-table-frame]");
    this.paginationContainer = document.querySelector(".pagination-container");

    this.searchBar = new SearchBar(
      document.querySelector(".search-form"),
      document.querySelector('input[name="query"]'),
      (query) => this.handleSearch(query),
    );

    this.grid = new ResultsGrid(
      {
        wrapper: document.querySelector(".table-wrapper"),
        head: document.querySelector("[data-table-head]"),
        body: document.querySelector("[data-table-body]"),
      },
      {
        onHeaderReorder: (newHeaders) => this.handleHeaderReorder(newHeaders),
        onHeaderAutoFit: (visibleSet) => this.handleHeaderAutoFit(visibleSet),
      },
    );

    this.columnFilter = new ColumnFilter(
      {
        btn: document.querySelector(".btn-filter"),
        menu: document.querySelector(".filter-menu"),
        selectAll: document.querySelector(".filter-select-all input"),
        options: document.querySelector(".filter-options"),
        count: document.querySelector(".filter-count"),
        alignTo: document.querySelector(".table-frame"),
      },
      {
        onToggle: (header, isChecked) => this.handleHeaderToggle(header, isChecked),
        onToggleAll: (isChecked) => this.handleToggleAllHeaders(isChecked),
      },
    );

    this.pagination = new Pagination(
      this.paginationContainer,
      (uiPage) => this.handlePageChange(uiPage),
      this.state.totalPages,
    );
  }

  async handleSearch(query) {
    this.state.lastQuery = query;
    this.state.page = 0;
    await this.fetchData();
  }

  async handlePageChange(uiPage) {
    this.state.page = uiPage - 1;
    await this.fetchData();
  }

  handleHeaderToggle(header, isChecked) {
    const nextVisible = new Set(this.state.visibleHeaders);
    if (isChecked) nextVisible.add(header);
    else nextVisible.delete(header);

    this.state.visibleHeaders = nextVisible;
    this.finishRender();
  }

  handleToggleAllHeaders(isChecked) {
    this.state.visibleHeaders = isChecked ? new Set(this.state.headers) : new Set();
    this.finishRender();
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

      if (response.aborted) return;
      this.processSearchResults(response);
    } catch (error) {
      console.error("Search failed:", error);
    } finally {
      this.searchBar.setLoading(false);
    }
  }

  processSearchResults({ items, totalPages }) {
    this.state.data = items;
    if (totalPages !== null) this.state.totalPages = totalPages;

    if (!this.state.data.length) {
      if (this.container) this.container.hidden = true;
      if (this.paginationContainer) this.paginationContainer.hidden = true;
      return;
    }

    const incomingHeaders = Object.keys(this.state.data[0]).filter((k) => k !== "_id");

    if (!this.state.headers.length) {
      this.state.headers = incomingHeaders;
      this.state.visibleHeaders = new Set(incomingHeaders);
    } else {
      const preservedHeaders = this.state.headers.filter((h) => incomingHeaders.includes(h));
      const newHeaders = incomingHeaders.filter((h) => !this.state.headers.includes(h));

      this.state.headers = [...preservedHeaders, ...newHeaders];
      newHeaders.forEach((h) => this.state.visibleHeaders.add(h));
    }

    if (this.container) this.container.hidden = false;

    this.finishRender();
    this.grid.fitHeadersToWidth(this.state.headers, this.state.visibleHeaders);
  }

  finishRender() {
    const hasColumns = this.state.visibleHeaders.size > 0;

    if (this.tableFrame) this.tableFrame.hidden = !hasColumns;
    if (this.paginationContainer) this.paginationContainer.hidden = !hasColumns;

    this.columnFilter.render(this.state.headers, this.state.visibleHeaders);
    this.pagination.render(this.state.page + 1, this.state.totalPages);

    if (hasColumns) this.grid.render(this.state.data, this.state.headers, this.state.visibleHeaders);
  }
}

const startApp = () => {
  new SearchApp();
};

if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", startApp);
else startApp();
