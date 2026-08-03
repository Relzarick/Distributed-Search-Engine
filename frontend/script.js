const form = document.querySelector(".search-form");
const input = form.querySelector('input[name="query"]');

form.addEventListener("submit", async (event) => {
  event.preventDefault();

  const query = input.value.trim();

  if (!query) return;

  try {
    const res = await fetch(
      `https://service.relzarick.com/search?q=${encodeURIComponent(query)}`,
    );

    if (!res.ok) throw new Error("Request failed");

    const data = await res.json();

    console.log(data); // remove
  } catch (error) {
    console.log;
  }
});
