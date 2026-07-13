document.addEventListener("DOMContentLoaded", () => {
  document.documentElement.classList.add("banking-theme-ready");

  const password = document.getElementById("password");
  const passwordToggle = document.getElementById("password-show-password");

  if (password && passwordToggle) {
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
  }

  const loginForm = document.getElementById("kc-form-login");
  const loginButton = document.getElementById("kc-login");

  if (loginForm && loginButton) {
    let isSubmitting = false;

    loginForm.addEventListener("submit", (event) => {
      if (isSubmitting) {
        event.preventDefault();
        return;
      }

      isSubmitting = true;
      loginForm.setAttribute("aria-busy", "true");
      loginButton.setAttribute("aria-disabled", "true");
      loginButton.classList.add("banking-submit--loading");

      const label = loginButton.querySelector(".banking-submit__label");
      const progress = document.getElementById("login-progress");
      const loadingLabel = loginButton.dataset.loadingLabel;
      const loadingAnnouncement = loginForm.dataset.loadingAnnouncement;

      if (label && loadingLabel) label.textContent = loadingLabel;
      if (progress && loadingAnnouncement) progress.textContent = loadingAnnouncement;
    });
  }
});
