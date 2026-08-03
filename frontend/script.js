const form = document.querySelector(".search-form");
const input = form.querySelector('input[name="query"]');

form.addEventListener("submit", async (e) => {
  e.preventDefault();

  const query = input.value.trim();

  if (!query) return;

  try {
    const res = await fetch(`/search?q=${encodeURIComponent(query)}`);

    if (!res.ok) throw new Error("Request failed");

    const data = await res.json();

    console.log(data); // remove
  } catch (error) {
    console.log("Search error:", error);
  }
});
