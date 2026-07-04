document.addEventListener("DOMContentLoaded", () => {
  document.documentElement.classList.add("banking-theme-ready");

  const username = document.getElementById("username");
  const password = document.getElementById("password");

  if (username && !username.getAttribute("placeholder")) {
    username.setAttribute("placeholder", "tu.usuario@nerva.dev");
  }

  if (password && !password.getAttribute("placeholder")) {
    password.setAttribute("placeholder", "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022");
  }

  const passwordToggle = document.getElementById("password-show-password");

  if (password && passwordToggle) {
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

      if (icon) {
        icon.classList.toggle("banking-icon--eye", !isHidden);
        icon.classList.toggle("banking-icon--eye-off", isHidden);
      }
    });
  }
});
