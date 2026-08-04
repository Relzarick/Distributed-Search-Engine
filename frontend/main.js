const elements = {
  form: document.querySelector(".search-form"),
  input: document.querySelector('input[name="query"]'),
  container: document.getElementById("table-container"),
  wrapper: document.querySelector(".table-wrapper"),
  head: document.getElementById("table-head"),
  body: document.getElementById("table-body"),
  filterBtn: document.getElementById("filter-btn"),
  filterMenu: document.getElementById("filter-menu"),
  filterCount: document.getElementById("filter-count"),
};

const state = {
  data: [],
  headers: [],
  visibleHeaders: new Set(),
};

elements.form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const query = elements.input.value.trim();
  if (!query) return;

  elements.input.value = "";

  try {
    const res = await fetch(`/search?q=${encodeURIComponent(query)}`);

    if (!res.ok) throw new Error("Request failed");

    renderResults(await res.json());
  } catch (error) {
    console.error("Search error:", error);
  }
});

function renderResults(data) {
  state.data = Array.isArray(data) ? data.slice(0, 50) : data ? [data] : [];

  if (!state.data.length) {
    elements.container.style.display = "none";
    return;
  }

  state.headers = Object.keys(state.data[0]);
  state.visibleHeaders = new Set(state.headers);
  elements.container.style.display = "flex";

  renderHeadersOnly();

  while (
    elements.wrapper.scrollWidth > elements.wrapper.clientWidth &&
    state.visibleHeaders.size > 1
  ) {
    const lastVisible = [...state.visibleHeaders].pop();
    state.visibleHeaders.delete(lastVisible);
    renderHeadersOnly();
  }

  buildDropdown();
  renderTable();
}

function renderHeadersOnly() {
  const tr = document.createElement("tr");

  for (const header of state.headers) {
    if (!state.visibleHeaders.has(header)) continue;

    const th = document.createElement("th");
    th.textContent = header.replace(/_/g, " ");
    tr.append(th);
  }

  elements.head.replaceChildren(tr);
}

function buildDropdown() {
  elements.filterMenu.replaceChildren();

  state.headers.forEach((header) => {
    const label = document.createElement("label");
    label.className = "filter-label";

    const checkbox = document.createElement("input");
    checkbox.type = "checkbox";
    checkbox.checked = state.visibleHeaders.has(header);

    checkbox.addEventListener("change", (e) => {
      e.target.checked
        ? state.visibleHeaders.add(header)
        : state.visibleHeaders.delete(header);
      renderTable();
    });

    label.append(checkbox, header.replace(/_/g, " "));
    elements.filterMenu.append(label);
  });
}

function renderTable() {
  elements.filterCount.textContent = `${state.visibleHeaders.size} of ${state.headers.length} columns`;
  renderHeadersOnly();

  const fragment = document.createDocumentFragment();

  for (const row of state.data) {
    const tr = document.createElement("tr");

    for (const header of state.headers) {
      if (!state.visibleHeaders.has(header)) continue;
      const td = document.createElement("td");
      td.textContent = row[header] ?? "";
      tr.append(td);
    }
    fragment.append(tr);
  }

  elements.body.replaceChildren(fragment);
}

elements.filterBtn.addEventListener("click", () => {
  const isExpanded = elements.filterMenu.classList.toggle("show");
  elements.filterBtn.setAttribute("aria-expanded", isExpanded);
});

document.addEventListener("click", (e) => {
  if (
    !elements.filterBtn.contains(e.target) &&
    !elements.filterMenu.contains(e.target)
  ) {
    elements.filterMenu.classList.remove("show");
    elements.filterBtn.setAttribute("aria-expanded", "false");
  }
});
