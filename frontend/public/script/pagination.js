export class Pagination {
  constructor(container, onPageChange, totalPages) {
    this.container = container;
    this.onPageChange = onPageChange;
    this.currentPage = 1;
    this.totalPages = totalPages;

    if (this.container) this.container.addEventListener("click", (e) => this.handleClick(e));
  }

  handleClick(e) {
    const btn = e.target.closest(".pagination-btn");
    if (!btn || btn.disabled) return;

    const { action, page } = btn.dataset;
    let targetPage = this.currentPage;

    if (action === "first") targetPage = 1;
    else if (action === "prev") targetPage = Math.max(1, this.currentPage - 1);
    else if (action === "next") targetPage = Math.min(this.totalPages, this.currentPage + 1);
    else if (action === "last") targetPage = this.totalPages;
    else if (page) targetPage = parseInt(page, 10);

    if (targetPage !== this.currentPage) this.onPageChange(targetPage);
  }

  getPageNumbers() {
    const maxVisible = 5;
    let start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
    let end = start + maxVisible - 1;

    if (end > this.totalPages) {
      end = this.totalPages;
      start = Math.max(1, end - maxVisible + 1);
    }

    return Array.from({ length: Math.max(0, end - start + 1) }, (_, i) => start + i);
  }

  render(currentPage, totalPages) {
    if (!this.container) return;

    this.currentPage = Math.max(1, currentPage);
    this.totalPages = Math.max(1, totalPages);
    if (this.currentPage > this.totalPages) this.currentPage = this.totalPages;

    const activeElement = document.activeElement;
    const activeAction = activeElement?.dataset?.action;
    const activePage = activeElement?.dataset?.page;

    const pageButtons = this.getPageNumbers()
      .map(
        (page) => `
      <button
        type="button"
        class="pagination-btn ${page === this.currentPage ? "active" : ""}"
        data-page="${page}"
        aria-current="${page === this.currentPage ? "page" : "false"}"
        aria-label="Page ${page}">
        ${page}
      </button>`,
      )
      .join("");

    this.container.innerHTML = `
      <div class="pagination-group pagination-controls">
        <button type="button" class="pagination-btn pagination-btn-nav" data-action="first" aria-label="First Page" ${this.currentPage === 1 ? "disabled" : ""}>
          <span class="icon icon-sm icon-chevrons-left" aria-hidden="true"></span>
        </button>
        <button type="button" class="pagination-btn pagination-btn-nav" data-action="prev" aria-label="Previous Page" ${this.currentPage === 1 ? "disabled" : ""}>
          <span class="icon icon-sm icon-chevron-left" aria-hidden="true"></span>
        </button>
      </div>

      <div class="pagination-group pagination-pages">${pageButtons}</div>

      <div class="pagination-group pagination-controls">
        <button type="button" class="pagination-btn pagination-btn-nav" data-action="next" aria-label="Next Page" ${this.currentPage === this.totalPages ? "disabled" : ""}>
          <span class="icon icon-sm icon-chevron-right" aria-hidden="true"></span>
        </button>
        <button type="button" class="pagination-btn pagination-btn-nav" data-action="last" aria-label="Last Page" ${this.currentPage === this.totalPages ? "disabled" : ""}>
          <span class="icon icon-sm icon-chevrons-right" aria-hidden="true"></span>
        </button>
      </div>`;

    if (activeAction) this.container.querySelector(`[data-action="${activeAction}"]`)?.focus();
    else if (activePage) this.container.querySelector(`[data-page="${activePage}"]`)?.focus();
  }
}
