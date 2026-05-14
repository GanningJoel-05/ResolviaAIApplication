const API_BASE_URL = "http://localhost:8080/api";

function getAuthHeaders() {
    return {
        "Content-Type": "application/json",
        "Authorization": "Bearer " + localStorage.getItem("token")
    };
}

function handleLogout() {
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("currentUserEmail");
    localStorage.removeItem("currentUserName");
    localStorage.removeItem("currentViewTicketId");
    localStorage.removeItem("currentAdminTicketId");
    window.location.href = "login.html";
}

function formatDate(dateString) {
    if (!dateString) return "--/--/----";
    const date  = new Date(dateString);
    const day   = String(date.getDate()).padStart(2, "0");
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const year  = date.getFullYear();
    return `${day}/${month}/${year}`;
}

function showToast(message, type = "info") {
    const existing = document.getElementById("resolviaToast");
    if (existing) existing.remove();

    const colors = {
        success: { bg: "#dcfce7", color: "#166534", border: "#bbf7d0" },
        error:   { bg: "#fee2e2", color: "#991b1b", border: "#fecaca" },
        info:    { bg: "#dbeafe", color: "#1e40af", border: "#bfdbfe" },
        warning: { bg: "#fef3c7", color: "#92400e", border: "#fde68a" }
    };
    const c = colors[type] || colors.info;

    const toast = document.createElement("div");
    toast.id = "resolviaToast";
    toast.style.cssText = `
        position:fixed; top:20px; right:20px; z-index:9999;
        padding:14px 20px; border-radius:10px;
        font-size:0.9rem; font-weight:500; font-family:inherit;
        background:${c.bg}; color:${c.color}; border:1px solid ${c.border};
        box-shadow:0 4px 12px rgba(0,0,0,0.1); max-width:360px;
        animation:toastIn 0.3s ease;
    `;
    toast.textContent = message;

    if (!document.getElementById("toastStyle")) {
        const style = document.createElement("style");
        style.id = "toastStyle";
        style.textContent = `
            @keyframes toastIn { from{opacity:0;transform:translateY(-10px)} to{opacity:1;transform:translateY(0)} }
        `;
        document.head.appendChild(style);
    }
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3500);
}

function showSpinner(containerId, colSpan = 7) {
    const el = document.getElementById(containerId);
    if (!el) return;
    const isTable = el.tagName === "TBODY";
    if (isTable) {
        el.innerHTML = `
            <tr><td colspan="${colSpan}" style="text-align:center;padding:2.5rem;">
                <div style="display:inline-flex;flex-direction:column;align-items:center;gap:0.75rem;">
                    <div class="resolvia-spinner"></div>
                    <span style="color:#6b7280;font-size:0.9rem;">Loading...</span>
                </div>
            </td></tr>`;
    } else {
        el.innerHTML = `
            <div style="display:flex;flex-direction:column;align-items:center;gap:0.75rem;padding:2.5rem;">
                <div class="resolvia-spinner"></div>
                <span style="color:#6b7280;font-size:0.9rem;">Loading...</span>
            </div>`;
    }
    if (!document.getElementById("spinnerStyle")) {
        const style = document.createElement("style");
        style.id = "spinnerStyle";
        style.textContent = `
            .resolvia-spinner {
                width:32px; height:32px; border-radius:50%;
                border:3px solid #e5e7eb;
                border-top-color:#4f46e5;
                animation:spin 0.7s linear infinite;
            }
            @keyframes spin { to { transform:rotate(360deg); } }
        `;
        document.head.appendChild(style);
    }
}

function hideSpinner(containerId) {
    const el = document.getElementById(containerId);
    if (el) el.innerHTML = "";
}

function showConfirm(message, onConfirm, confirmText = "Confirm", confirmColor = "#4f46e5") {
    const existing = document.getElementById("resolviaModal");
    if (existing) existing.remove();

    const overlay = document.createElement("div");
    overlay.id = "resolviaModal";
    overlay.style.cssText = `
        position:fixed; inset:0; z-index:10000;
        background:rgba(0,0,0,0.4);
        display:flex; align-items:center; justify-content:center;
        animation:fadeInOverlay 0.2s ease;
    `;

    overlay.innerHTML = `
        <div style="
            background:#fff; border-radius:14px; padding:2rem;
            max-width:400px; width:90%; box-shadow:0 20px 40px rgba(0,0,0,0.15);
            animation:slideInModal 0.2s ease; font-family:inherit;
        ">
            <h3 style="font-size:1.1rem;font-weight:600;color:#111827;margin:0 0 0.75rem;">Confirm Action</h3>
            <p style="color:#6b7280;font-size:0.95rem;line-height:1.5;margin:0 0 1.5rem;">${message}</p>
            <div style="display:flex;gap:0.75rem;justify-content:flex-end;">
                <button id="modalCancel" style="
                    padding:0.5rem 1.2rem; border-radius:8px;
                    border:1px solid #e5e7eb; background:#f9fafb;
                    font-size:0.9rem; font-weight:500; cursor:pointer;
                    color:#374151; font-family:inherit;
                ">Cancel</button>
                <button id="modalConfirm" style="
                    padding:0.5rem 1.2rem; border-radius:8px;
                    border:none; background:${confirmColor};
                    color:#fff; font-size:0.9rem; font-weight:500;
                    cursor:pointer; font-family:inherit;
                ">${confirmText}</button>
            </div>
        </div>
    `;

    if (!document.getElementById("modalStyle")) {
        const style = document.createElement("style");
        style.id = "modalStyle";
        style.textContent = `
            @keyframes fadeInOverlay { from{opacity:0} to{opacity:1} }
            @keyframes slideInModal  { from{opacity:0;transform:scale(0.95)} to{opacity:1;transform:scale(1)} }
        `;
        document.head.appendChild(style);
    }

    document.body.appendChild(overlay);

    document.getElementById("modalCancel").onclick  = () => overlay.remove();
    document.getElementById("modalConfirm").onclick = () => { overlay.remove(); onConfirm(); };
    overlay.onclick = (e) => { if (e.target === overlay) overlay.remove(); };
}

function copyToClipboard(text, btnEl) {
    navigator.clipboard.writeText(text).then(() => {
        const original = btnEl.textContent;
        btnEl.textContent = "Copied!";
        btnEl.style.background = "#dcfce7";
        btnEl.style.color = "#166534";
        setTimeout(() => {
            btnEl.textContent = original;
            btnEl.style.background = "";
            btnEl.style.color = "";
        }, 2000);
    }).catch(() => showToast("Could not copy to clipboard", "error"));
}

function getPriorityBadge(priority) {
    const map = {
        "HIGH":   { bg:"#fee2e2", color:"#991b1b", border:"#fecaca", label:"High"   },
        "MEDIUM": { bg:"#fef3c7", color:"#92400e", border:"#fde68a", label:"Medium" },
        "LOW":    { bg:"#f0fdf4", color:"#166534", border:"#bbf7d0", label:"Low"    }
    };
    const p = map[priority] || map["LOW"];
    return `<span style="
        background:${p.bg}; color:${p.color}; border:1px solid ${p.border};
        padding:2px 10px; border-radius:99px; font-size:0.75rem; font-weight:600;
    ">${p.label}</span>`;
}

function requireAuth() {
    if (!localStorage.getItem("token")) {
        window.location.href = "login.html";
        return false;
    }
    return true;
}

function requireAdmin() {
    if (!localStorage.getItem("token")) {
        window.location.href = "login.html";
        return false;
    }
    if (localStorage.getItem("role") !== "ADMIN") {
        window.location.href = "dashboard.html";
        return false;
    }
    return true;
}