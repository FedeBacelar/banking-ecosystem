document.addEventListener("DOMContentLoaded", () => {
  document.documentElement.classList.add("banking-theme-ready");

  document.querySelectorAll("[data-banking-password-toggle]").forEach((passwordToggle) => {
    const controlledInputId = passwordToggle.getAttribute("aria-controls");
    const password = controlledInputId
      ? document.getElementById(controlledInputId)
      : null;

    if (!password) return;

    passwordToggle.setAttribute("aria-pressed", "false");

    passwordToggle.addEventListener("click", () => {
      const icon = passwordToggle.querySelector(".banking-icon");
      const isHidden = password.getAttribute("type") === "password";
      const nextType = isHidden ? "text" : "password";
      const nextLabel = isHidden
        ? passwordToggle.dataset.labelHide
        : passwordToggle.dataset.labelShow;

      password.setAttribute("type", nextType);

      if (nextLabel) {
        passwordToggle.setAttribute("aria-label", nextLabel);
      }

      passwordToggle.setAttribute("aria-pressed", String(isHidden));

      if (icon) {
        icon.classList.toggle("banking-icon--eye", !isHidden);
        icon.classList.toggle("banking-icon--eye-off", isHidden);
      }
    });
  });

  document.querySelectorAll("[data-banking-loading-form]").forEach((form) => {
    const submitButton = form.querySelector("[data-banking-submit]");
    if (!submitButton) return;

    let isSubmitting = false;

    form.addEventListener("submit", (event) => {
      if (event.submitter?.hasAttribute("data-banking-cancel")) return;

      if (isSubmitting) {
        event.preventDefault();
        return;
      }

      isSubmitting = true;
      form.setAttribute("aria-busy", "true");
      submitButton.setAttribute("aria-disabled", "true");
      submitButton.classList.add("banking-submit--loading");

      const label = submitButton.querySelector(".banking-submit__label");
      const progress = form.querySelector("[data-banking-progress]");
      const loadingLabel = submitButton.dataset.loadingLabel;
      const loadingAnnouncement = form.dataset.loadingAnnouncement;

      if (label && loadingLabel) label.textContent = loadingLabel;
      if (progress && loadingAnnouncement) progress.textContent = loadingAnnouncement;
    });
  });
});
