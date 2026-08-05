const PAGE_SIZE = 50;

class SearchApp {
  constructor() {
    this.state = {
      data: [],
      headers: [],
      visibleHeaders: new Set(),
      lastQuery: null,
      page: 1,
    };

    this.container = document.getElementById("table-container");
    this.tableFrame = document.querySelector(".table-frame");
    this.paginationContainer = document.querySelector(".pagination-container");

    this.searchBar = new SearchBar(
      document.querySelector(".search-form"),
      document.querySelector('input[name="query"]'),
      (query) => this.handleSearch(query),
    );

    this.grid = new ResultsGrid(
      this.state,
      {
        wrapper: document.querySelector(".table-wrapper"),
        head: document.getElementById("table-head"),
        body: document.getElementById("table-body"),
      },
      () => {
        this.columnFilter.build();
        this.finishRender();
      },
    );

    this.columnFilter = new ColumnFilter(
      this.state,
      {
        btn: document.getElementById("filter-btn"),
        menu: document.getElementById("filter-menu"),
        selectAll: document.querySelector(".filter-select-all input"),
        options: document.getElementById("filter-options"),
        count: document.getElementById("filter-count"),
      },
      () => this.finishRender(),
    );

    this.pagination = new Pagination(
      this.state,
      this.paginationContainer,
      (page) => (this.state.page = page),
    );
  }

  async handleSearch(query) {
    if (query === this.state.lastQuery) return;

    this.state.lastQuery = query;
    this.state.page = 1;
    this.searchBar.setLoading(true);

    try {
      const res = await fetch(`/search?q=${encodeURIComponent(query)}`);
      if (!res.ok) throw new Error("Request failed");
      this.renderResults(await res.json());
    } catch (error) {
      console.error("Search error:", error);
    } finally {
      this.searchBar.setLoading(false);
    }
  }

  renderResults(data) {
    const list = Array.isArray(data) ? data : data ? [data] : [];
    this.state.data = list.slice(0, PAGE_SIZE);

    if (!this.state.data.length) {
      this.container.style.display = "none";
      return;
    }

    this.state.headers = Object.keys(this.state.data[0]).filter(
      (key) => key !== "_id",
    );
    this.state.visibleHeaders = new Set(this.state.headers);
    this.container.style.display = "flex";

    this.grid.fitHeadersToWidth();
    this.columnFilter.build();
    this.finishRender();
  }

  finishRender() {
    const hasColumns = this.state.visibleHeaders.size > 0;

    this.tableFrame.style.display = hasColumns ? "block" : "none";
    this.paginationContainer.style.display = hasColumns ? "flex" : "none";

    this.columnFilter.updateCount();
    if (hasColumns) this.grid.render();
  }
}

new SearchApp();
