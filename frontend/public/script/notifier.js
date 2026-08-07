export const NotificationSource = Object.freeze({
  ROW_COPY: "ROW_COPY",
});

export class StatusNotifier {
  static MESSAGES = {
    [NotificationSource.ROW_COPY]: "Copied row to clipboard",
  };

  constructor(element, displayDurationMs = 3000) {
    this.element = element;
    this.displayDurationMs = displayDurationMs;
    this.timeoutId = null;
  }

  notify(sourceIdentifier) {
    if (!this.element) return;

    const message = StatusNotifier.MESSAGES[sourceIdentifier];
    if (!message) return;

    if (this.timeoutId) clearTimeout(this.timeoutId);

    this.element.textContent = message;
    this.element.classList.add("is-visible");

    this.timeoutId = setTimeout(() => {
      this.element.classList.remove("is-visible");
      this.timeoutId = null;
    }, this.displayDurationMs);
  }
}
