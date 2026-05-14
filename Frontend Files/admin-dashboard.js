// ======================================================
// admin-dashboard.js — ResolviaAI Admin Pages
// common.js must be loaded before this file
// ======================================================

requireAdmin();

let adminTickets  = [];
let sortState     = { col: null, dir: "asc" };
let pieChart      = null;
let barChart      = null;

// ===========================
// INIT ADMIN DASHBOARD
// ===========================
async function initAdminDashboard() {
    try {
        await loadAdminProfile();
        await loadTickets();
        loadCounters();
        updateNeedsReviewBadge();
        renderAdminTable(adminTickets);
        renderPieChart();
        renderBarChart();
    } catch (err) {
        console.error("Dashboard init error:", err);
        showToast("Failed to load dashboard data", "error");
    }
}

// ===========================
// LOAD ADMIN PROFILE
// ===========================
async function loadAdminProfile() {
    try {
        const res  = await fetch(`${API_BASE_URL}/user/profile`, { headers: getAuthHeaders() });
        const user = await res.json();
        const set  = (id, val) => { const el = document.getElementById(id); if(el) el.innerText = val; };
        set("adminNameText", "Admin: " + user.name);

        // FIX 5: Store userId so onboarding.js can read it for admin tour
        if (user.id) {
            localStorage.setItem("currentUserId", user.id);
            // Trigger onboarding AFTER userId is stored
            const key   = "onboardingCount_" + user.id;
            const count = parseInt(localStorage.getItem(key) || "0");
            if (count < 2) {
                localStorage.setItem(key, count + 1);
                setTimeout(function() {
                    if (typeof runAdminTour === "function") runAdminTour();
                }, 900);
            }
        }

        const av = document.getElementById("adminAvatar");
        if (av) {
            const storedPic = user.id ? localStorage.getItem("profilePic_admin_" + user.id) : null;
            if (storedPic) {
                av.innerHTML = `<img src="${storedPic}" style="width:100%;height:100%;object-fit:cover;border-radius:50%;" alt="avatar">`;
                av.style.padding = "0";
            } else {
                av.innerText = user.name.charAt(0).toUpperCase();
            }
        }
        const hour     = new Date().getHours();
        const greeting = hour < 12 ? "Good morning" : hour < 17 ? "Good afternoon" : "Good evening";
        set("greetingText",    greeting + ", " + user.name + "!");
        set("greetingSubText", "Here's what's happening with your tickets today.");
    } catch (e) {
        const el = document.getElementById("adminNameText");
        if (el) el.innerText = "Admin";
    }
}

// ===========================
// LOAD ALL TICKETS
// ===========================
async function loadTickets() {
    showSpinner("adminTicketTableBody", 9);
    const res = await fetch(`${API_BASE_URL}/admin/tickets`, { headers: getAuthHeaders() });
    if (!res.ok) { showToast("Failed to load tickets", "error"); adminTickets = []; return; }
    adminTickets = await res.json();
}

// ===========================
// NEEDS REVIEW BADGE
// ===========================
function updateNeedsReviewBadge() {
    const count = adminTickets.filter(t => t.status === "IN_PROGRESS").length;
    document.querySelectorAll(".needsReviewBadge").forEach(badge => {
        badge.textContent = count;
        badge.style.display = count > 0 ? "inline-flex" : "none";
    });
}

// ===========================
// SUMMARY COUNTERS
// ===========================
function loadCounters() {
    const total    = adminTickets.length;
    const open     = adminTickets.filter(t => t.status === "OPEN").length;
    const resolved = adminTickets.filter(t => t.status === "RESOLVED").length;
    const aiSolved = adminTickets.filter(t => t.aiResolutionType === "AUTO").length;
    const month    = new Date().getMonth();
    const monthly  = adminTickets.filter(t => new Date(t.createdAt).getMonth() === month).length;

    const set = (id, val) => { const el = document.getElementById(id); if(el) el.innerText = val; };
    set("count-total",    total);
    set("count-open",     open);
    set("count-resolved", resolved);
    set("count-ai",       aiSolved);
    set("count-monthly",  monthly);
}

// ===========================
// SEARCH FILTER
// ===========================
function searchTickets(query) {
    const q = (query || "").toLowerCase().trim();
    if (!q) { renderAdminTable(adminTickets); return; }
    const filtered = adminTickets.filter(t => {
        const user = t.user ? t.user.name.toLowerCase() : "";
        return (
            (t.ticketNumber || "").toLowerCase().includes(q) ||
            (t.title || "").toLowerCase().includes(q)        ||
            (t.category || "").toLowerCase().includes(q)     ||
            user.includes(q)
        );
    });
    renderAdminTable(filtered);
}

// ===========================
// SORT COLUMN
// ===========================
function sortBy(col) {
    if (sortState.col === col) sortState.dir = sortState.dir === "asc" ? "desc" : "asc";
    else { sortState.col = col; sortState.dir = "asc"; }
    document.querySelectorAll(".sort-icon").forEach(el => el.textContent = "↕");
    const icon = document.getElementById("sort-" + col);
    if (icon) icon.textContent = sortState.dir === "asc" ? "↑" : "↓";
    const sorted = [...adminTickets].sort((a, b) => {
        let va, vb;
        if      (col === "date")  { va = new Date(a.createdAt);      vb = new Date(b.createdAt); }
        else if (col === "conf")  { va = a.aiConfidence || 0;          vb = b.aiConfidence || 0; }
        else if (col === "title") { va = (a.title||"").toLowerCase();  vb = (b.title||"").toLowerCase(); }
        else { va = 0; vb = 0; }
        if (va < vb) return sortState.dir === "asc" ? -1 : 1;
        if (va > vb) return sortState.dir === "asc" ?  1 : -1;
        return 0;
    });
    renderAdminTable(sorted);
}

// ===========================
// TICKET TYPE HELPER
// Returns badge HTML for Technical / Non-Technical
// ===========================
function getTicketTypeBadge(ticket) {
    const isNonTechnical = ticket.aiConfidence === 0 &&
        (ticket.aiResolutionType === "NON_TECHNICAL" || ticket.aiResolutionType === "MANUAL");

    if (isNonTechnical) {
        return `<span style="
            background:#fee2e2; color:#991b1b; border:1px solid #fecaca;
            padding:3px 8px; border-radius:99px; font-size:0.72rem; font-weight:600;
            white-space:nowrap; display:inline-block;">
            ⚠ Non-Tech
        </span>`;
    }
    return `<span style="
        background:#dcfce7; color:#166534; border:1px solid #bbf7d0;
        padding:3px 8px; border-radius:99px; font-size:0.72rem; font-weight:600;
        white-space:nowrap; display:inline-block;">
        ✓ Technical
    </span>`;
}

// ===========================
// RENDER ADMIN TABLE
// 9 columns: ID | User | Issue | Category | Ticket Type | AI Conf | Priority | Status | Action
// ===========================
// ===========================
// ADMIN TABLE — with pagination
// ===========================
const ADMIN_TICKETS_PER_PAGE = 10;
var _adminCurrentPage    = 1;
var _adminPagedTickets   = [];

function renderAdminTable(tickets) {
    _adminPagedTickets = tickets;
    _adminCurrentPage  = 1;
    renderAdminTablePage();
}

function renderAdminTablePage() {
    const tbody = document.getElementById("adminTicketTableBody");
    if (!tbody) return;

    const total      = _adminPagedTickets.length;
    const totalPages = Math.max(1, Math.ceil(total / ADMIN_TICKETS_PER_PAGE));
    _adminCurrentPage = Math.min(_adminCurrentPage, totalPages);

    const start = (_adminCurrentPage - 1) * ADMIN_TICKETS_PER_PAGE;
    const end   = Math.min(start + ADMIN_TICKETS_PER_PAGE, total);
    const paged = _adminPagedTickets.slice(start, end);

    if (total === 0) {
        tbody.innerHTML = `<tr><td colspan="9" style="text-align:center;padding:2.5rem;color:#6b7280;">No tickets found.</td></tr>`;
        renderAdminPaginationControls(0, 0, 0);
        return;
    }

    tbody.innerHTML = "";
    paged.forEach(ticket => {

        let statusHTML = "";
        if (ticket.status === "RESOLVED" && ticket.aiResolutionType === "AUTO") {
            statusHTML = `<span class="badge-admin badge-aisolved" style="white-space:nowrap;">AI Solved</span>`;
        } else if (ticket.status === "RESOLVED" && ticket.aiResolutionType === "NON_TECHNICAL") {
            statusHTML = `<span class="badge-admin" style="background:#fee2e2;color:#991b1b;border:1px solid #fecaca;white-space:nowrap;">AI Rejected</span>`;
        } else if (ticket.status === "RESOLVED") {
            statusHTML = `<span class="badge-admin badge-mixed" style="white-space:nowrap;">Resolved</span>`;
        } else if (ticket.status === "IN_PROGRESS") {
            statusHTML = `<span class="badge-admin badge-human" style="white-space:nowrap;">Needs Review</span>`;
        } else {
            statusHTML = `<span class="badge-admin badge-open" style="white-space:nowrap;">Open</span>`;
        }

        let actionClass = "btn-action-view", actionText = "View";
        if (ticket.status === "IN_PROGRESS") { actionClass = "btn-action-assign"; actionText = "Resolve"; }

        const pMap = {
            HIGH:   {bg:"#fee2e2",color:"#991b1b",border:"#fecaca"},
            MEDIUM: {bg:"#fef3c7",color:"#92400e",border:"#fde68a"},
            LOW:    {bg:"#f0fdf4",color:"#166534",border:"#bbf7d0"}
        };
        const p = pMap[ticket.priority] || null;
        const priorityBadge = p
            ? `<span style="background:${p.bg};color:${p.color};border:1px solid ${p.border};
                padding:3px 10px;border-radius:99px;font-size:0.75rem;font-weight:600;white-space:nowrap;">
                ${ticket.priority.charAt(0) + ticket.priority.slice(1).toLowerCase()}</span>`
            : `<span style="color:#9ca3af;font-size:0.85rem;">—</span>`;

        const userId   = ticket.user ? ticket.user.id   : null;
        const userName = ticket.user ? ticket.user.name : "Unknown";
        const userCell = userId
            ? `<a href="profile.html?userId=${userId}"
                style="color:var(--primary-accent);font-weight:500;text-decoration:none;"
                title="View ${userName}'s profile"
                onmouseover="this.style.textDecoration='underline'"
                onmouseout="this.style.textDecoration='none'">${userName}</a>`
            : `<span>${userName}</span>`;

        const confText       = ticket.aiConfidence != null ? ticket.aiConfidence + "%" : "—";
        const ticketTypeBadge = getTicketTypeBadge(ticket);

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td>${ticket.ticketNumber || "#" + ticket.id}</td>
            <td>${userCell}</td>
            <td>${ticket.title}</td>
            <td>${ticket.category || "General"}</td>
            <td>${ticketTypeBadge}</td>
            <td>${confText}</td>
            <td>${priorityBadge}</td>
            <td>${statusHTML}</td>
            <td><button class="btn-ticket-action ${actionClass}" onclick="viewAdminTicket(${ticket.id})">${actionText}</button></td>
        `;
        tbody.appendChild(tr);
    });

    renderAdminPaginationControls(totalPages, start + 1, end);
}

function renderAdminPaginationControls(totalPages, start, end) {
    const existing = document.getElementById("adminTicketPagination");
    if (existing) existing.remove();
    if (totalPages <= 1) return;

    const total     = _adminPagedTickets.length;
    const container = document.createElement("div");
    container.id    = "adminTicketPagination";
    container.style.cssText =
        "display:flex;align-items:center;justify-content:space-between;" +
        "padding:0.85rem 1.25rem;border-top:1px solid var(--border-color);" +
        "flex-wrap:wrap;gap:0.5rem;";

    const info = document.createElement("span");
    info.style.cssText = "font-size:0.83rem;color:#6b7280;";
    info.textContent   = `Showing ${start}–${end} of ${total} tickets`;

    const btnGroup = document.createElement("div");
    btnGroup.style.cssText = "display:flex;align-items:center;gap:0.4rem;";

    const prev = document.createElement("button");
    prev.textContent   = "← Prev";
    prev.disabled      = _adminCurrentPage === 1;
    prev.style.cssText = adminPaginBtnStyle(_adminCurrentPage === 1);
    prev.onclick = () => { _adminCurrentPage--; renderAdminTablePage(); };

    const pageNums = document.createElement("div");
    pageNums.style.cssText = "display:flex;gap:0.3rem;";
    let startPage = Math.max(1, _adminCurrentPage - 2);
    let endPage   = Math.min(totalPages, startPage + 4);
    if (endPage - startPage < 4) startPage = Math.max(1, endPage - 4);
    for (let pg = startPage; pg <= endPage; pg++) {
        const btn = document.createElement("button");
        btn.textContent   = pg;
        btn.style.cssText = adminPaginBtnStyle(false, pg === _adminCurrentPage);
        const p = pg;
        btn.onclick = () => { _adminCurrentPage = p; renderAdminTablePage(); };
        pageNums.appendChild(btn);
    }

    const next = document.createElement("button");
    next.textContent   = "Next →";
    next.disabled      = _adminCurrentPage === totalPages;
    next.style.cssText = adminPaginBtnStyle(_adminCurrentPage === totalPages);
    next.onclick = () => { _adminCurrentPage++; renderAdminTablePage(); };

    btnGroup.appendChild(prev);
    btnGroup.appendChild(pageNums);
    btnGroup.appendChild(next);
    container.appendChild(info);
    container.appendChild(btnGroup);

    const card = document.querySelector(".table-card");
    if (card) card.appendChild(container);
}

function adminPaginBtnStyle(disabled, active) {
    const base = "padding:0.35rem 0.75rem;border-radius:6px;font-size:0.82rem;font-weight:500;" +
                 "cursor:pointer;font-family:inherit;border:1px solid var(--border-color);transition:all 0.15s;";
    if (disabled) return base + "opacity:0.4;cursor:not-allowed;background:transparent;color:#9ca3af;";
    if (active)   return base + "background:var(--primary-accent);color:#fff;border-color:var(--primary-accent);";
    return base + "background:transparent;color:var(--text-color);";
}

// ===========================
// FILTER ADMIN TICKETS
// ===========================
function filterAdminTickets(type, card) {
    document.querySelectorAll(".summary-card").forEach(c => c.classList.remove("active"));
    card.classList.add("active");
    let filtered  = adminTickets;
    const titleEl = document.getElementById("table-title");
    if      (type==="Open")     { filtered=adminTickets.filter(t=>t.status==="OPEN"); if(titleEl) titleEl.innerText="Open Tickets"; }
    else if (type==="Resolved") { filtered=adminTickets.filter(t=>t.status==="RESOLVED"); if(titleEl) titleEl.innerText="Resolved Tickets"; }
    else if (type==="AI_Solved"){ filtered=adminTickets.filter(t=>t.aiResolutionType==="AUTO"); if(titleEl) titleEl.innerText="AI Resolved Tickets"; }
    else if (type==="Monthly")  {
        const m=new Date().getMonth();
        filtered=adminTickets.filter(t=>new Date(t.createdAt).getMonth()===m);
        if(titleEl) titleEl.innerText="This Month's Tickets";
    } else { if(titleEl) titleEl.innerText="Recent Tickets"; }
    renderAdminTable(filtered);
}

// ===========================
// NAVIGATE TO TICKET DETAIL
// ===========================
function viewAdminTicket(id) {
    localStorage.setItem("currentAdminTicketId", id);
    window.location.href = "admin-ticket-detail.html";
}

// ===========================
// CHARTS
// ===========================
function renderPieChart() {
    const ctx = document.getElementById("distributionPieChart");
    if (!ctx) return;
    const open=adminTickets.filter(t=>t.status==="OPEN").length;
    const resolved=adminTickets.filter(t=>t.status==="RESOLVED").length;
    const inProgress=adminTickets.filter(t=>t.status==="IN_PROGRESS").length;
    if (pieChart) pieChart.destroy();
    pieChart = new Chart(ctx, {
        type:"pie", data:{labels:["Open","Resolved","Needs Review"],
            datasets:[{data:[open,resolved,inProgress],backgroundColor:["#3B82F6","#22C55E","#F59E0B"]}]},
        options:{responsive:true,maintainAspectRatio:false}
    });
}

function renderBarChart() {
    const ctx = document.getElementById("monthlyBarChart");
    if (!ctx) return;
    const monthNames=["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
    const created=new Array(12).fill(0), solved=new Array(12).fill(0);
    adminTickets.forEach(t => {
        const m=new Date(t.createdAt).getMonth(); created[m]++;
        if(t.status==="RESOLVED") solved[m]++;
    });
    if (barChart) barChart.destroy();
    barChart = new Chart(ctx, {
        type:"bar", data:{labels:monthNames, datasets:[
            {label:"Tickets Created",data:created,backgroundColor:"#ef4444"},
            {label:"Tickets Solved", data:solved, backgroundColor:"#22c55e"}
        ]}, options:{responsive:true,maintainAspectRatio:false}
    });
}

// ===========================
// AI DECISION PANEL
// ===========================
async function initAiDecisionPanel() {
    await loadAdminProfile();
    showSpinner("topCategoriesContainer");
    try {
        const [statsRes,ticketsRes]=await Promise.all([
            fetch(`${API_BASE_URL}/admin/stats`,   {headers:getAuthHeaders()}),
            fetch(`${API_BASE_URL}/admin/tickets`, {headers:getAuthHeaders()})
        ]);
        if(!statsRes.ok||!ticketsRes.ok){showToast("Failed to load AI stats","error");return;}
        const stats=await statsRes.json(), tickets=await ticketsRes.json();
        const total=stats.totalTickets||0, aiCount=stats.aiResolved||0;
        const score=total>0?Math.round((aiCount/total)*100):0;
        const set=(id,val)=>{const el=document.getElementById(id);if(el)el.innerText=val;};
        set("ai-score-value",score+"%"); set("ai-solved-count",aiCount);
        set("human-solved-count",total-aiCount); set("stat-total",total);
        set("stat-month",stats.thisMonth||0); set("stat-open",stats.openTickets||0);
        renderTopCategories(tickets);
    } catch(err){showToast("Error loading panel data","error");}
}

function renderTopCategories(tickets) {
    const catMap={};
    tickets.forEach(t=>{const c=t.category||"General";catMap[c]=(catMap[c]||0)+1;});
    const sorted=Object.entries(catMap).sort((a,b)=>b[1]-a[1]).slice(0,5);
    const colors=[
        {bg:"#fee2e2",color:"#dc2626"},{bg:"#fef3c7",color:"#d97706"},
        {bg:"#e0e7ff",color:"#4338ca"},{bg:"#dcfce7",color:"#16a34a"},
        {bg:"#f3e8ff",color:"#9333ea"}
    ];
    const container=document.getElementById("topCategoriesContainer");
    if(!container)return;
    if(sorted.length===0){container.innerHTML='<div style="color:#6b7280;font-size:0.9rem;">No data yet.</div>';return;}
    container.innerHTML=sorted.map(([cat,count],i)=>{
        const c=colors[i]||colors[0];
        return`<div style="display:flex;align-items:center;gap:1rem;">
            <div style="width:32px;height:32px;border-radius:6px;background:${c.bg};color:${c.color};
                display:flex;align-items:center;justify-content:center;font-weight:700;flex-shrink:0;">${i+1}</div>
            <div style="flex:1;font-weight:500;font-size:0.95rem;color:var(--text-color);">${cat}</div>
            <div style="font-weight:600;color:#6b7280;font-size:0.9rem;">${count}</div>
        </div>`;
    }).join("");
}