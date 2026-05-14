// ======================================================
// theme.js — ResolviaAI Dark / Light Mode System
// Include on EVERY page after common.js
// ======================================================

(function () {

    // ── Apply theme immediately on load (before paint) ────────────────────
    const saved = localStorage.getItem("resolviaTheme") || "dark";
    applyTheme(saved, false);

    // ── Wait for DOM then inject toggle button ─────────────────────────────
    document.addEventListener("DOMContentLoaded", () => {
        injectThemeToggle();
    });

    // ── Core: apply theme by setting data-theme on <html> ─────────────────
    function applyTheme(theme, animate) {
        document.documentElement.setAttribute("data-theme", theme);
        localStorage.setItem("resolviaTheme", theme);

        // Update all toggle buttons on page
        document.querySelectorAll(".theme-toggle-btn").forEach(btn => {
            btn.innerHTML   = theme === "dark" ? getSunIcon() : getMoonIcon();
            btn.title       = theme === "dark" ? "Switch to Light Mode" : "Switch to Dark Mode";
        });
    }

    // ── Toggle between dark and light ─────────────────────────────────────
    window.toggleTheme = function () {
        const current = localStorage.getItem("resolviaTheme") || "dark";
        applyTheme(current === "dark" ? "light" : "dark", true);
    };

    // ── Inject toggle button into existing nav ─────────────────────────────
    // Works for both admin nav (.nav-right) and user nav (.nav-right)
    function injectThemeToggle() {
        const navRight = document.querySelector(".nav-right");
        if (!navRight) return;

        // Don't inject twice
        if (document.querySelector(".theme-toggle-btn")) return;

        const btn = document.createElement("button");
        btn.className = "theme-toggle-btn";
        btn.onclick   = window.toggleTheme;

        const current = localStorage.getItem("resolviaTheme") || "dark";
        btn.innerHTML = current === "dark" ? getSunIcon() : getMoonIcon();
        btn.title     = current === "dark" ? "Switch to Light Mode" : "Switch to Dark Mode";

        // Insert before the first child of nav-right
        navRight.insertBefore(btn, navRight.firstChild);
    }

    function getSunIcon() {
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"/>
            <line x1="12" y1="1" x2="12" y2="3"/>
            <line x1="12" y1="21" x2="12" y2="23"/>
            <line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/>
            <line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/>
            <line x1="1" y1="12" x2="3" y2="12"/>
            <line x1="21" y1="12" x2="23" y2="12"/>
            <line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/>
            <line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
        </svg>`;
    }

    function getMoonIcon() {
        return `<svg width="18" height="18" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
        </svg>`;
    }

})();