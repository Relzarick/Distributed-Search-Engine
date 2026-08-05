class SearchBar {
  constructor(formEl, inputEl, onSubmit) {
    this.form = formEl;
    this.input = inputEl;
    this.onSubmit = onSubmit;
    this.form.addEventListener("submit", (e) => this.handleSubmit(e));
  }

  handleSubmit(e) {
    e.preventDefault();
    const query = this.input.value.trim();
    if (!query) return;
    this.onSubmit(query);
  }

  clear() {
    this.input.value = "";
  }

  setLoading(isLoading) {
    this.form.classList.toggle("loading", isLoading);
  }
}
