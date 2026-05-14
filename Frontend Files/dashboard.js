// ======================================================
// dashboard.js — ResolviaAI User Dashboard
// Only loaded on USER pages
// ======================================================

requireAuth();

// ===========================
// INJECT USER SIDEBAR
// ===========================
function injectSidebar(activePage) {
    const existing = document.getElementById("userSidebar");
    if (existing) return;

    const isActive = (page) => activePage === page ? "active" : "";

    const sidebar = document.createElement("aside");
    sidebar.id = "userSidebar";
    sidebar.className = "user-sidebar";
    sidebar.innerHTML = `
        <a href="dashboard.html" class="user-sidebar-header">
            Resolvia<span>AI</span>
        </a>
        <ul class="user-sidebar-menu">
            <li>
                <a href="dashboard.html" class="${isActive('dashboard')}">
                    <svg class="menu-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                        fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <rect x="3" y="3" width="7" height="7"></rect><rect x="14" y="3" width="7" height="7"></rect>
                        <rect x="14" y="14" width="7" height="7"></rect><rect x="3" y="14" width="7" height="7"></rect>
                    </svg>
                    Dashboard
                </a>
            </li>
            <li>
                <a href="create-ticket.html" class="${isActive('create')}">
                    <svg class="menu-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                        fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="10"></circle>
                        <line x1="12" y1="8" x2="12" y2="16"></line>
                        <line x1="8" y1="12" x2="16" y2="12"></line>
                    </svg>
                    Create Ticket
                </a>
            </li>
            <li class="sidebar-divider"></li>
            <li>
                <a href="profile.html" class="${isActive('profile')}">
                    <svg class="menu-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                        fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                        <circle cx="12" cy="7" r="4"></circle>
                    </svg>
                    My Profile
                </a>
            </li>
            <li>
                <a href="settings.html" class="${isActive('settings')}">
                    <svg class="menu-icon" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"
                        fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <circle cx="12" cy="12" r="3"></circle>
                        <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                    </svg>
                    Settings
                </a>
            </li>
        </ul>
    `;

    document.body.insertBefore(sidebar, document.body.firstChild);
}

// ===========================
// LOAD USER PROFILE
// ===========================
async function loadUserProfile() {
    try {
        const res  = await fetch(`${API_BASE_URL}/user/profile`, { headers: getAuthHeaders() });
        if (!res.ok) { handleLogout(); return; }
        const user = await res.json();

        const set = (id, val) => { const el = document.getElementById(id); if(el) el.innerText = val; };
        set("welcomeUserText", "Welcome, " + user.name);
        set("userNameText",    user.name);
        set("userEmailText",   user.email);

        if (user.id) localStorage.setItem("currentUserId", user.id);
        const av = document.getElementById("userAvatar");
        if (av) {
            const storedPic = user.id ? localStorage.getItem("profilePic_" + user.id) : null;
            if (storedPic) {
                av.innerHTML = `<img src="${storedPic}" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" alt="avatar">`;
                av.style.padding = "0";
            } else {
                av.innerText = user.name.charAt(0).toUpperCase();
            }
        }

        const hour     = new Date().getHours();
        const greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        set("greetingText", greeting + ", " + user.name + "!");

        // ── BLOCK / DELETE OVERLAY CHECK ─────────────────────────────────
        // If account is restricted, show blurred overlay instead of normal dashboard
        const status = user.accountStatus;
        if (status && status !== "ACTIVE") {
            showAccountBlockOverlay(user);
        }

    } catch (e) { console.error("Profile error:", e); }
}

// ════════════════════════════════════════════════════════════
// ACCOUNT BLOCK OVERLAY — shown on dashboard for blocked users
// ════════════════════════════════════════════════════════════
let _blockOverlayAdminId = null;

function showAccountBlockOverlay(user) {
    const overlay = document.getElementById("accountBlockOverlay");
    if (!overlay) return;

    const status = user.accountStatus;

    // Style header by status
    const header   = document.getElementById("blockOverlayHeader");
    const icon     = document.getElementById("blockOverlayIcon");
    const title    = document.getElementById("blockOverlayTitle");
    const subtitle = document.getElementById("blockOverlaySubtitle");

    if (status === "TEMPORARY") {
        if (header)   header.style.background   = "linear-gradient(135deg,#d97706,#b45309)";
        if (icon)     icon.textContent           = "⏱";
        if (title)    title.textContent          = "Account Temporarily Blocked";
        if (subtitle) subtitle.textContent       = "Your access has been temporarily restricted";
    } else if (status === "PERMANENT") {
        if (header)   header.style.background   = "linear-gradient(135deg,#dc2626,#b91c1c)";
        if (icon)     icon.textContent           = "🔒";
        if (title)    title.textContent          = "Account Permanently Blocked";
        if (subtitle) subtitle.textContent       = "Your account has been permanently restricted";
    } else if (status === "DELETED") {
        if (header)   header.style.background   = "linear-gradient(135deg,#374151,#1f2937)";
        if (icon)     icon.textContent           = "🗑️";
        if (title)    title.textContent          = "Account Deleted";
        if (subtitle) subtitle.textContent       = "This account has been deactivated by an administrator";
    }

    // Fill reason
    const reasonEl = document.getElementById("blockOverlayReason");
    if (reasonEl) reasonEl.textContent = user.blockReason || "No specific reason provided.";

    // Fill admin name
    const adminNameEl = document.getElementById("blockOverlayAdminName");
    if (adminNameEl) adminNameEl.textContent = user.blockedByAdminName
        ? "Admin " + user.blockedByAdminName
        : "Administrator";

    // Store admin ID for messaging
    _blockOverlayAdminId = user.blockedByAdminId || null;

    // Show expiry for temporary blocks
    if (status === "TEMPORARY" && user.blockExpiresAt) {
        const expiryBox = document.getElementById("blockOverlayExpiryBox");
        const expiryEl  = document.getElementById("blockOverlayExpiry");
        if (expiryBox) expiryBox.style.display = "block";
        if (expiryEl)  expiryEl.textContent = new Date(user.blockExpiresAt)
            .toLocaleString("en-IN", { day:"2-digit", month:"short", year:"numeric",
                hour:"2-digit", minute:"2-digit" });
    }

    // Message area is shown for ALL statuses (TEMPORARY, PERMANENT, DELETED)
    // so the user can always appeal to the admin who actioned them
    const msgArea = document.getElementById("blockMessageArea");
    if (msgArea) msgArea.style.display = "block";

    // Check for existing admin reply
    pollForAdminReply(user.id, _blockOverlayAdminId);

    // Blur dashboard + show overlay
    document.body.classList.add("account-blocked");
    overlay.style.display = "flex";
}

async function sendBlockAppeal() {
    if (!_blockOverlayAdminId) {
        showToast("Cannot identify the admin. Please contact support.", "error");
        return;
    }
    const inputEl = document.getElementById("blockMessageInput");
    const msg     = inputEl ? inputEl.value.trim() : "";
    if (!msg) {
        showToast("Please write a message before sending.", "warning");
        if (inputEl) inputEl.focus();
        return;
    }

    try {
        const res = await fetch(`${API_BASE_URL}/messages/send`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ toAdminId: _blockOverlayAdminId, content: msg })
        });
        if (!res.ok) {
            const err = await res.json();
            showToast(err.message || "Failed to send message", "error");
            return;
        }
        // Show sent confirmation, hide form
        const form   = document.getElementById("blockMessageForm");
        const banner = document.getElementById("blockMessageSentBanner");
        if (form)   form.style.display   = "none";
        if (banner) banner.style.display = "block";
        if (inputEl) inputEl.value = "";
    } catch (e) {
        showToast("Server error: " + e.message, "error");
    }
}

async function pollForAdminReply(userId, adminId) {
    if (!userId || !adminId) return;
    try {
        const res = await fetch(`${API_BASE_URL}/messages/my`, { headers: getAuthHeaders() });
        if (!res.ok) return;
        const messages = await res.json();

        // Find most recent replied message from this admin
        const replied = messages.find(m => m.replied && m.toAdminId === adminId);
        if (replied) {
            const replyBox  = document.getElementById("blockAdminReplyBox");
            const replyText = document.getElementById("blockAdminReplyText");
            if (replyBox)  replyBox.style.display  = "block";
            if (replyText) replyText.textContent = replied.adminReply;
            // Also hide "send" form since there's already a reply
            const form = document.getElementById("blockMessageForm");
            if (form) form.style.display = "none";
        }

        // Check if user already sent a message (show sent banner)
        const hasSent = messages.some(m => m.toAdminId === adminId && !m.replied);
        if (hasSent && !replied) {
            const form   = document.getElementById("blockMessageForm");
            const banner = document.getElementById("blockMessageSentBanner");
            if (form)   form.style.display   = "none";
            if (banner) banner.style.display = "block";
        }
    } catch (e) { /* silent fail */ }

    // Poll every 30 seconds for admin reply
    setTimeout(() => pollForAdminReply(userId, adminId), 30000);
}

// ================================================================
// 😂 MOD 5 ENHANCED — NON-TECHNICAL TICKET PUNISHMENT SYSTEM
//
// Rules:
//   - 3rd NT ticket → yellow warning toast + yellow dashboard banner
//   - 4th NT ticket → orange warning toast + orange dashboard banner
//   - 5th NT ticket → 🚫 60-min block + red dashboard banner with countdown
//   - After block expires & user submits another NT ticket →
//     IMMEDIATE new 60-min block (no free passes after cooldown!)
//   - Counter is per-day (resets at midnight)
//   - Dashboard shows persistent banner when count >= 3
//   - create-ticket.html shows live mm:ss countdown during block
//
// Keys:
//   ntCount_<userId>_<YYYY-MM-DD>  → daily NT ticket count
//   ntBlockUntil_<userId>          → ISO timestamp of block expiry
// ================================================================

const NT_DAILY_LIMIT   = 5;   // triggers first block
const NT_WARN_AT       = 3;   // warning starts
const NT_BLOCK_MINUTES = 60;  // block duration in minutes

function getNtKey(userId) {
    const today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD
    return `ntCount_${userId}_${today}`;
}
function getBlockKey(userId) { return `ntBlockUntil_${userId}`; }

// Get today's NT count (0 if none)
function getNtCount(userId) {
    return parseInt(localStorage.getItem(getNtKey(userId)) || "0");
}

// Returns { blocked, minutesLeft, secondsLeft, remaining }
function checkPunishmentBlock(userId) {
    const blockUntil = localStorage.getItem(getBlockKey(userId));
    if (!blockUntil) return { blocked: false, minutesLeft: 0, secondsLeft: 0, remaining: 0 };
    const remaining = new Date(blockUntil) - new Date();
    if (remaining > 0) {
        return {
            blocked:     true,
            minutesLeft: Math.ceil(remaining / 60000),
            secondsLeft: Math.ceil(remaining / 1000),
            remaining
        };
    }
    // Block expired — clean it up
    localStorage.removeItem(getBlockKey(userId));
    return { blocked: false, minutesLeft: 0, secondsLeft: 0, remaining: 0 };
}

// Apply a fresh 60-min block
function applyBlock(userId) {
    const blockUntil = new Date(Date.now() + NT_BLOCK_MINUTES * 60 * 1000);
    localStorage.setItem(getBlockKey(userId), blockUntil.toISOString());
}

// Called every time a non-technical ticket is detected in the response
function recordNonTechnicalTicket(userId) {
    const key   = getNtKey(userId);
    const count = getNtCount(userId) + 1;
    localStorage.setItem(key, count);

    if (count >= NT_DAILY_LIMIT) {
        // BLOCK — first time at limit, or every NT ticket after limit
        applyBlock(userId);
        showToast(
            `🚫 Non-technical ticket #${count} today! ` +
            `Blocked for ${NT_BLOCK_MINUTES} minutes. Learn what IT support means! 😤`,
            "error"
        );
        // Refresh dashboard banner immediately
        renderDashboardPunishmentBanner(userId);
        return true;
    }

    if (count === 4) {
        showToast(
            `🔴 Final warning! ${count}/${NT_DAILY_LIMIT} non-technical tickets today. ` +
            `One more and you'll be blocked for ${NT_BLOCK_MINUTES} minutes! 😬`,
            "warning"
        );
    } else if (count >= NT_WARN_AT) {
        showToast(
            `⚠️ Warning: ${count}/${NT_DAILY_LIMIT} non-technical tickets today. ` +
            `Reach ${NT_DAILY_LIMIT} and you'll be blocked for ${NT_BLOCK_MINUTES} minutes! 😬`,
            "warning"
        );
    }

    // Refresh dashboard banner
    renderDashboardPunishmentBanner(userId);
    return false;
}

// ================================================================
// DASHBOARD WARNING BANNER
// Shows persistently on dashboard.html when NT count >= 3 today.
// Three visual states:
//   count == 3 → yellow  "Caution"
//   count == 4 → orange  "Final Warning"
//   count >= 5 → red     "Blocked" with live countdown
// ================================================================
let _bannerInterval = null;

function renderDashboardPunishmentBanner(userId) {
    const banner    = document.getElementById("ntWarningBanner");
    const title     = document.getElementById("ntBannerTitle");
    const message   = document.getElementById("ntBannerMessage");
    const icon      = document.getElementById("ntBannerIcon");
    const countdown = document.getElementById("ntBannerCountdown");
    if (!banner) return;

    const count = getNtCount(userId);
    const { blocked, remaining } = checkPunishmentBlock(userId);

    // ── MOD 2 FIX: Hide completely when not relevant ──────────────────────
    // Case 1: Below warning threshold AND not blocked → hide
    // Case 2: Block has expired (blocked=false) AND count >= limit → block lifted state
    if (count < NT_WARN_AT && !blocked) {
        banner.style.display = "none";
        if (_bannerInterval) { clearInterval(_bannerInterval); _bannerInterval = null; }
        return;
    }

    // ── MOD 2 FIX: Block-lifted state ────────────────────────────────────
    // When count >= limit but block has expired → show green "block lifted" message briefly
    if (!blocked && count >= NT_DAILY_LIMIT) {
        banner.style.cssText = `display:flex; margin-bottom:1.5rem; border-radius:10px;
            padding:1rem 1.25rem; border-left:4px solid #16a34a;
            background:#dcfce7; align-items:flex-start; gap:0.85rem;`;
        if (icon)    icon.textContent      = "✅";
        if (title)   { title.textContent   = "Block Lifted";
                       title.style.color   = "#166534"; }
        if (message) { message.textContent = `Your ticket creation block has ended. `+
                       `You've had ${count} non-technical tickets today — please submit only genuine IT issues.`;
                       message.style.color = "#166534"; }
        if (countdown) countdown.style.display = "none";
        if (_bannerInterval) { clearInterval(_bannerInterval); _bannerInterval = null; }
        // Auto-hide this "block lifted" message after 8 seconds
        setTimeout(() => {
            banner.style.display = "none";
        }, 8000);
        return;
    }

    // Choose state
    let bg, border, titleColor, msgColor, iconEl, titleText, msgText;

    if (blocked) {
        // RED — actively blocked
        bg         = "#fee2e2"; border = "#ef4444"; titleColor = "#991b1b"; msgColor = "#b91c1c";
        iconEl     = "🚫";
        titleText  = "Ticket Creation Blocked!";
        msgText    = `You've raised ${count} non-technical tickets today and are temporarily blocked. `+
                     `Only IT/technical issues are allowed. Block lifts automatically when the timer ends.`;
        if (countdown) countdown.style.display = "block";
    } else if (count === 4) {
        // ORANGE — final warning
        bg         = "#fff7ed"; border = "#f97316"; titleColor = "#c2410c"; msgColor = "#ea580c";
        iconEl     = "🔴";
        titleText  = "Final Warning!";
        msgText    = `You've raised ${count}/${NT_DAILY_LIMIT} non-technical tickets today. `+
                     `One more non-technical submission will block you for ${NT_BLOCK_MINUTES} minutes!`;
        if (countdown) countdown.style.display = "none";
    } else {
        // YELLOW — caution (count == 3)
        bg         = "#fefce8"; border = "#eab308"; titleColor = "#854d0e"; msgColor = "#a16207";
        iconEl     = "⚠️";
        titleText  = "Non-Technical Ticket Warning";
        msgText    = `You've raised ${count}/${NT_DAILY_LIMIT} non-technical tickets today. `+
                     `Reach ${NT_DAILY_LIMIT} and ticket creation will be blocked for ${NT_BLOCK_MINUTES} minutes.`;
        if (countdown) countdown.style.display = "none";
    }

    // Apply styles
    banner.style.cssText = `display:flex; margin-bottom:1.5rem; border-radius:10px;
        padding:1rem 1.25rem; border-left:4px solid ${border};
        background:${bg}; align-items:flex-start; gap:0.85rem;`;
    if (icon)      icon.textContent      = iconEl;
    if (title)     { title.textContent   = titleText; title.style.color = titleColor; }
    if (message)   { message.textContent = msgText;   message.style.color = msgColor; }

    // Live countdown for blocked state
    if (_bannerInterval) { clearInterval(_bannerInterval); _bannerInterval = null; }

    if (blocked && countdown) {
        function updateCountdown() {
            const { blocked: stillBlocked, remaining: rem } = checkPunishmentBlock(userId);
            if (!stillBlocked) {
                // ── MOD 2 FIX: Block just expired ────────────────────────
                countdown.style.display = "none";
                clearInterval(_bannerInterval);
                _bannerInterval = null;
                // Show "block lifted" state instead of re-triggering red banner
                renderDashboardPunishmentBanner(userId);
                return;
            }
            const secs = Math.ceil(rem / 1000);
            const m    = Math.floor(secs / 60);
            const s    = secs % 60;
            countdown.textContent  = `${m}m ${String(s).padStart(2,"0")}s`;
            countdown.style.color  = titleColor;
        }
        updateCountdown();
        _bannerInterval = setInterval(updateCountdown, 1000);
    }
}

// ================================================================
// MOD 3 — AI OVERRIDES CATEGORY
// The user's selected category is sent to backend but AI
// re-classifies and the backend stores the AI-determined category.
// On the frontend, we just send what the user picked — the backend
// AI classifier will override it with the correct one automatically.
// (No frontend change needed for the classification itself — the
//  backend AiClassifierService already returns the right category
//  and TicketService sets ticket.setCategory from the AI result.)
// ================================================================

// ================================================================
// MOD 4 — FASTER SUBMISSION
// Skip confirmation dialog to eliminate one round-trip delay.
// Submit directly on form submit.
// ================================================================
async function handleTicketSubmit(event) {
    event.preventDefault();
    const title       = document.getElementById("ticketTitle").value.trim();
    const description = document.getElementById("ticketDescription").value.trim();
    const category    = document.getElementById("ticketCategory").value;

    if (!title || !description || !category) {
        showToast("Please fill in all fields", "warning");
        return false;
    }

    // Check punishment block
    const userId = localStorage.getItem("currentUserId");
    if (userId) {
        const { blocked, minutesLeft } = checkPunishmentBlock(userId);
        if (blocked) {
            showToast(
                `🚫 You are blocked for ${minutesLeft} more minute${minutesLeft !== 1 ? "s" : ""}. ` +
                `Too many non-technical tickets! 😅`,
                "error"
            );
            return false;
        }
    }

    // MOD 4: Direct submit — no confirm dialog, saves one full round-trip
    await submitTicketAPI(title, description, category);
    return false;
}

async function submitTicketAPI(title, description, category) {
    const btn = document.querySelector("#ticketForm button[type='submit']");
    if (btn) { btn.disabled = true; btn.textContent = "Submitting..."; }

    try {
        const res = await fetch(`${API_BASE_URL}/tickets`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ title, description, category })
        });

        if (!res.ok) {
            const data = await res.json();
            showToast(data.message || "Failed to create ticket", "error");
            if (btn) { btn.disabled = false; btn.textContent = "Submit Ticket"; }
            return;
        }

        const ticketData = await res.json();

        // ================================================================
        // MOD 5 + MOD 6: Non-technical detection
        // aiConfidence === 0 means AI flagged it as non-technical.
        // The backend (MOD 6) already auto-escalates it (RESOLVED with
        // a "non-technical" solution) — no admin involvement.
        // Here on frontend we just record punishment count.
        // ================================================================
        if (ticketData && ticketData.aiConfidence === 0) {
            const uid = localStorage.getItem("currentUserId");
            if (uid) recordNonTechnicalTicket(uid);
        }

        window.location.href = "ticket-success.html";

    } catch (e) {
        showToast("Server error. Please try again.", "error");
        if (btn) { btn.disabled = false; btn.textContent = "Submit Ticket"; }
    }
}

// ===========================
// LOAD USER TICKETS
// ===========================
async function loadUserTickets() {
    showSpinner("ticketTableBody", 6);
    try {
        const res = await fetch(`${API_BASE_URL}/tickets/my`, { headers: getAuthHeaders() });
        if (!res.ok) return;
        const tickets       = await res.json();
        window._userTickets = tickets;
        renderCounts(tickets);
        renderTable(tickets);

        const open = tickets.filter(t => t.status === "OPEN" || t.status === "IN_PROGRESS").length;
        const el   = document.getElementById("greetingSubText");
        if (el) {
            el.innerText = open > 0
                ? `You have ${open} open ticket${open > 1 ? "s" : ""} awaiting resolution.`
                : "All your tickets are resolved. Great!";
        }
    } catch (e) { console.error("Tickets error:", e); }
}

// ===========================
// FILTER TICKETS
// ===========================
function filterTickets(type, card) {
    document.querySelectorAll(".summary-card").forEach(c => c.classList.remove("active"));
    if (card) card.classList.add("active");
    const tickets = window._userTickets || [];
    let filtered  = tickets;
    const titleEl = document.getElementById("table-title");
    if (type === "Inprocess") {
        filtered = tickets.filter(t => t.status === "OPEN" || t.status === "IN_PROGRESS");
        if (titleEl) titleEl.innerText = "Open Tickets";
    } else if (type === "Approve") {
        filtered = tickets.filter(t => t.status === "RESOLVED");
        if (titleEl) titleEl.innerText = "Resolved Tickets";
    } else {
        if (titleEl) titleEl.innerText = "All Tickets";
    }
    renderTable(filtered);
}

// ===========================
// COUNTS
// ===========================
function renderCounts(tickets) {
    const set = (id, val) => { const el = document.getElementById(id); if(el) el.innerText = val; };
    set("count-all",      tickets.length);
    set("count-open",     tickets.filter(t => t.status === "OPEN" || t.status === "IN_PROGRESS").length);
    set("count-resolved", tickets.filter(t => t.status === "RESOLVED").length);
}

// ===========================
// RENDER TABLE — 6 columns
// ===========================
function renderTable(tickets) {
    const tbody = document.getElementById("ticketTableBody");
    if (!tbody) return;
    if (tickets.length === 0) {
        tbody.innerHTML = `<tr><td colspan="7" style="text-align:center;padding:2.5rem;color:var(--text-muted);">No tickets found.</td></tr>`;
        return;
    }
    tbody.innerHTML = "";
    tickets.forEach(ticket => {
        const tr = document.createElement("tr");
        let statusClass = "badge-inprocess", statusText = "In Progress";
        if      (ticket.status === "RESOLVED")    { statusClass = "badge-approve";   statusText = "Resolved";    }
        else if (ticket.status === "OPEN")        { statusClass = "badge-inprocess"; statusText = "Open";        }
        else if (ticket.status === "IN_PROGRESS") { statusClass = "badge-inprocess"; statusText = "In Progress"; }

        const pMap = { HIGH:{bg:"#fee2e2",color:"#991b1b",border:"#fecaca"}, MEDIUM:{bg:"#fef3c7",color:"#92400e",border:"#fde68a"}, LOW:{bg:"#f0fdf4",color:"#166534",border:"#bbf7d0"} };
        const p = pMap[ticket.priority] || null;
        const priorityBadge = p
            ? `<span style="background:${p.bg};color:${p.color};border:1px solid ${p.border};padding:3px 10px;border-radius:99px;font-size:0.75rem;font-weight:600;">${ticket.priority.charAt(0)+ticket.priority.slice(1).toLowerCase()}</span>`
            : `<span style="color:#9ca3af;">—</span>`;

        let resolvedByCell = `<span style="color:#9ca3af;font-size:0.85rem;">—</span>`;
        if (ticket.status === "RESOLVED") {
            if (ticket.aiResolutionType === "AUTO") {
                resolvedByCell = `<span style="background:#ede9fe;color:#5b21b6;padding:3px 10px;border-radius:99px;font-size:0.75rem;font-weight:600;border:1px solid #ddd6fe;white-space:nowrap;"> AI System</span>`;
            } else if (ticket.aiResolutionType === "AI_HUMAN") {
                resolvedByCell = `<span style="background:#fef3c7;color:#92400e;padding:3px 10px;border-radius:99px;font-size:0.75rem;font-weight:600;border:1px solid #fde68a;white-space:nowrap;"> AI + Admin</span>`;
            } else if (ticket.aiResolutionType === "NON_TECHNICAL") {
                // Non-technical tickets auto-escalated by AI
                resolvedByCell = `<span style="background:#fee2e2;color:#991b1b;padding:3px 10px;border-radius:99px;font-size:0.75rem;font-weight:600;border:1px solid #fecaca;white-space:nowrap;"> AI (Non-Tech)</span>`;
            } else if (ticket.aiResolutionType === "MANUAL" && ticket.aiConfidence === null) {
                // MOD 2b: API was unavailable — resolved by admin manually
                resolvedByCell = `<span style="background:#fff7ed;color:#c2410c;padding:3px 10px;border-radius:99px;font-size:0.75rem;font-weight:600;border:1px solid #fed7aa;white-space:nowrap;"> Admin (AI N/A)</span>`;
            } else {
                resolvedByCell = `<span style="background:#dbeafe;color:#1e40af;padding:3px 10px;border-radius:99px;font-size:0.75rem;font-weight:600;border:1px solid #bfdbfe;white-space:nowrap;"> Admin</span>`;
            }
        }

        tr.innerHTML = `
            <td>${ticket.ticketNumber || "#"+ticket.id}</td>
            <td>${ticket.title}</td>
            <td>${formatDate(ticket.createdAt)}</td>
            <td>${ticket.category || "General"}</td>
            <td>${priorityBadge}</td>
            <td>${resolvedByCell}</td>
            <td><span class="badge ${statusClass} clickable-badge" onclick="viewTicket(${ticket.id},'${ticket.status}')">${statusText}</span></td>
        `;
        tbody.appendChild(tr);
    });
}

// ===========================
// SMART ROUTING
// ===========================
function viewTicket(id, status) {
    localStorage.setItem("currentViewTicketId", id);
    window.location.href = status === "RESOLVED" ? "ticket-solution.html" : "ticket-info.html";
}

// ===========================
// STATUS TIMELINE
// ===========================
function renderStatusTimeline(ticket) {
    const container = document.getElementById("statusTimeline");
    if (!container) return;
    const isNonTechnical = ticket.aiConfidence === 0 && ticket.aiResolutionType === "NON_TECHNICAL";
    const isResolved     = ticket.status === "RESOLVED";
    const isInProgress   = ticket.status === "IN_PROGRESS";
    const isOpen         = ticket.status === "OPEN";
    const steps = [
        { label:"Submitted",    done:true },
        { label:"AI Analyzing", done:!isOpen },
        { label:"Admin Review", done:isInProgress || isResolved },
        { label:"Resolved",     done:isResolved }
    ];
    container.innerHTML = `
        <div style="display:flex;align-items:center;margin:1.5rem 0;">
            ${steps.map((step,i) => `
                <div style="display:flex;align-items:center;flex:1;">
                    <div style="display:flex;flex-direction:column;align-items:center;min-width:80px;">
                        <div style="width:32px;height:32px;border-radius:50%;
                            background:${step.done?"#4f46e5":"#e5e7eb"};
                            color:${step.done?"#fff":"#9ca3af"};
                            display:flex;align-items:center;justify-content:center;
                            font-weight:700;font-size:0.8rem;
                            border:2px solid ${step.done?"#4f46e5":"#e5e7eb"};">
                            ${step.done?"✓":(i+1)}
                        </div>
                        <div style="font-size:0.72rem;font-weight:${step.done?"600":"400"};
                            color:${step.done?"#4f46e5":"#9ca3af"};margin-top:6px;
                            text-align:center;white-space:nowrap;">${step.label}</div>
                    </div>
                    ${i<steps.length-1?`<div style="flex:1;height:2px;background:${steps[i+1].done?"#4f46e5":"#e5e7eb"};margin-bottom:18px;"></div>`:""}
                </div>
            `).join("")}
        </div>
        ${isNonTechnical?`
            <div style="background:#fef3c7;border:1px solid #fde68a;border-left:4px solid #f59e0b;
                border-radius:8px;padding:1rem 1.25rem;margin-top:0.5rem;">
                <div style="font-weight:600;color:#92400e;margin-bottom:0.25rem;">⚠ Non-Technical Issue Detected</div>
                <div style="color:#78350f;font-size:0.9rem;line-height:1.5;">
                    This ticket was identified as a non-technical issue and was automatically
                    escalated by AI. Please submit only IT-related problems.
                </div>
            </div>`:""}
    `;
}

// ===========================
// LOAD TICKET DETAILS
// ===========================
async function loadTicketDetails() {
    const id = localStorage.getItem("currentViewTicketId");
    if (!id) return;
    try {
        const res = await fetch(`${API_BASE_URL}/tickets/${id}`, { headers: getAuthHeaders() });
        if (!res.ok) return;
        const ticket = await res.json();
        const set = (elId,val) => { const el=document.getElementById(elId); if(el) el.innerText=val||"—"; };
        set("view-ticket-id",       ticket.ticketNumber||"#"+ticket.id);
        set("view-ticket-title",    ticket.title);
        set("view-ticket-desc",     ticket.description);
        set("view-ticket-category", ticket.category||"General");
        set("view-ticket-date",     formatDate(ticket.createdAt));
        renderStatusTimeline(ticket);
        const sol = document.getElementById("view-ticket-solution");
        if (sol) sol.innerText = ticket.solution || "AI is analyzing your issue. Please wait...";
        const dot  = document.getElementById("indicatorDot");
        const text = document.getElementById("indicator-type-text");
        if (text) {
            if      (ticket.aiResolutionType==="AUTO")          { if(dot) dot.className="indicator-color bg-green";  text.innerText="Solved automatically by AI"; }
            else if (ticket.aiResolutionType==="AI_HUMAN")      { if(dot) dot.className="indicator-color bg-yellow"; text.innerText="AI suggestion reviewed by support team"; }
            else if (ticket.aiResolutionType==="NON_TECHNICAL") { if(dot) dot.className="indicator-color bg-red";    text.innerText="Non-technical issue — auto-escalated by AI"; }
            else                                                 { if(dot) dot.className="indicator-color bg-red";    text.innerText="Manually resolved by support team"; }
        }
        if (ticket.status==="RESOLVED" && window.location.pathname.includes("ticket-info.html")) {
            window.location.href = "ticket-solution.html";
        }
    } catch (e) { console.error("Ticket detail error:", e); }
}

// ===========================
// PAGE INIT
// ===========================
document.addEventListener("DOMContentLoaded", () => {
    const path = window.location.pathname;

    if (path.includes("dashboard.html")) {
        injectSidebar("dashboard");
        loadUserProfile().then(() => {
            // Render punishment banner after profile loads (userId is set by loadUserProfile)
            const userId = localStorage.getItem("currentUserId");
            if (userId) renderDashboardPunishmentBanner(userId);
        });
        loadUserTickets();
    } else if (path.includes("create-ticket.html")) {
        injectSidebar("create");
        loadUserProfile();
    } else if (path.includes("ticket-info.html")) {
        injectSidebar("dashboard");
        loadUserProfile();
        loadTicketDetails();
    } else if (path.includes("ticket-solution.html")) {
        injectSidebar("dashboard");
        loadUserProfile();
    } else if (path.includes("profile.html") && localStorage.getItem("role") !== "ADMIN") {
        injectSidebar("profile");
    } else if (path.includes("settings.html")) {
        injectSidebar("settings");
        loadUserProfile();
    }
});

// Auto-refresh on ticket-info
setInterval(() => {
    if (window.location.pathname.includes("ticket-info.html")) loadTicketDetails();
}, 5000);