export class SearchBar {
  constructor(formEl, inputEl, onSubmit) {
    this.form = formEl;
    this.input = inputEl;
    this.onSubmit = onSubmit;

    if (this.form) this.form.addEventListener("submit", (e) => this.handleSubmit(e));
  }

  handleSubmit(e) {
    e.preventDefault();
    this.onSubmit(this.input?.value.trim() || "");
  }

  clear() {
    if (this.input) this.input.value = "";
  }

  setLoading(isLoading) {
    if (this.form) this.form.classList.toggle("loading", isLoading);
    if (this.input) this.input.disabled = isLoading;
  }
}
