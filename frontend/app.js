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
      () => this.handleReorder(),
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
      () => this.handleFilterChange(),
    );

    this.pagination = new Pagination(
      this.state,
      document.querySelector(".pagination-container"),
      (page) => this.handlePageChange(page),
    );
  }

  async handleSearch(query) {
    if (query === this.state.lastQuery) return;

    this.state.lastQuery = query;
    this.state.page = 1;
    this.searchBar.clear();
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
    this.state.data = Array.isArray(data)
      ? data.slice(0, PAGE_SIZE)
      : data
        ? [data]
        : [];

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

  handleReorder() {
    this.columnFilter.build();
    this.finishRender();
  }

  handleFilterChange() {
    this.finishRender();
  }

  handlePageChange(page) {
    // Not yet wired
  }

  finishRender() {
    this.columnFilter.updateCount();
    this.grid.render();
  }
}

new SearchApp();
