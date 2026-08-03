const form = document.querySelector(".search-form");
const input = form.querySelector('input[name="query"]');
const output = document.querySelector(".output");

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const query = input.value.trim();

  if (!query) return;

  input.value = "";

  try {
    const res = await fetch(`/search?q=${encodeURIComponent(query)}`);

    if (!res.ok) throw new Error("Request failed");

    const data = await res.json();

    renderResults(data);
  } catch (error) {
    console.log("Search error:", error);
  }
});

function renderResults(data) {
  output.replaceChildren();

  const rows = Array.isArray(data) ? data.slice(0, 50) : [data]; // limit view to 50

  if (!rows.length) return;

  for (const row of rows) {
    const section = document.createElement("section");
    section.className = "result";

    for (const [key, value] of Object.entries(row)) {
      const field = document.createElement("div");
      field.className = "field";

      const heading = document.createElement("h3");
      heading.textContent = key;

      const content = document.createElement("p");
      content.textContent = value ?? "";

      field.append(heading, content);
      section.append(field);
    }

    output.append(section);
  }
}
