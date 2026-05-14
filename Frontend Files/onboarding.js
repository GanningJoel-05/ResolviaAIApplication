// ======================================================
// onboarding.js — ResolviaAI Guided Tour
// Works for both USER (dashboard.html) and ADMIN (admin-dashboard.html)
// Triggers on first 2 logins per user. Pure CSS + JS, no external library.
// ======================================================

(function () {

    // ── CSS injected once ──────────────────────────────────────────────────
    const style = document.createElement('style');
    style.textContent = `
        /* Overlay */
        .ob-overlay {
            position: fixed; inset: 0; z-index: 99990;
            background: rgba(0,0,0,0.55);
            animation: obFadeIn 0.3s ease;
        }
        /* Spotlight hole — created by box-shadow */
        .ob-spotlight {
            position: fixed; z-index: 99991;
            border-radius: 8px;
            box-shadow: 0 0 0 9999px rgba(0,0,0,0.55);
            transition: all 0.35s cubic-bezier(0.4,0,0.2,1);
            pointer-events: none;
        }
        /* Tooltip bubble */
        .ob-tooltip {
            position: fixed; z-index: 99999;
            background: #fff;
            border-radius: 14px;
            padding: 1.25rem 1.5rem 1rem;
            max-width: 320px; min-width: 260px;
            box-shadow: 0 20px 60px rgba(0,0,0,0.25), 0 0 0 1px rgba(0,0,0,0.06);
            font-family: 'Inter', sans-serif;
            animation: obSlideIn 0.3s ease;
        }
        [data-theme="dark"] .ob-tooltip {
            background: #1e293b;
            box-shadow: 0 20px 60px rgba(0,0,0,0.5), 0 0 0 1px rgba(255,255,255,0.08);
        }
        .ob-tooltip-arrow {
            position: absolute; width: 12px; height: 12px;
            background: #fff; transform: rotate(45deg);
        }
        [data-theme="dark"] .ob-tooltip-arrow { background: #1e293b; }
        .ob-step-badge {
            display: inline-block;
            background: #ede9fe; color: #5b21b6;
            font-size: 0.7rem; font-weight: 700;
            padding: 2px 10px; border-radius: 99px;
            margin-bottom: 0.5rem;
            text-transform: uppercase; letter-spacing: 0.04em;
        }
        .ob-title {
            font-size: 1rem; font-weight: 700;
            color: #111827; margin: 0 0 0.4rem;
        }
        [data-theme="dark"] .ob-title { color: #f1f5f9; }
        .ob-body {
            font-size: 0.875rem; color: #6b7280;
            line-height: 1.55; margin: 0 0 1rem;
        }
        [data-theme="dark"] .ob-body { color: #94a3b8; }
        .ob-footer {
            display: flex; align-items: center;
            justify-content: space-between; gap: 0.5rem;
        }
        .ob-dots {
            display: flex; gap: 5px; align-items: center;
        }
        .ob-dot {
            width: 7px; height: 7px; border-radius: 50%;
            background: #e2e8f0; transition: background 0.2s;
        }
        [data-theme="dark"] .ob-dot { background: #334155; }
        .ob-dot.active { background: #4f46e5; }
        .ob-btn-skip {
            font-size: 0.8rem; color: #9ca3af; background: none;
            border: none; cursor: pointer; font-family: inherit;
            padding: 0.3rem 0.5rem; border-radius: 6px;
        }
        .ob-btn-skip:hover { color: #6b7280; }
        .ob-btn-next {
            background: linear-gradient(135deg, #4f46e5, #6366f1);
            color: #fff; border: none; border-radius: 8px;
            padding: 0.45rem 1.2rem; font-size: 0.85rem;
            font-weight: 600; cursor: pointer; font-family: inherit;
            transition: opacity 0.2s;
        }
        .ob-btn-next:hover { opacity: 0.88; }
        @keyframes obFadeIn  { from { opacity:0 } to { opacity:1 } }
        @keyframes obSlideIn { from { opacity:0; transform:translateY(10px) } to { opacity:1; transform:translateY(0) } }
    `;
    document.head.appendChild(style);

    // ── Step definitions per role ──────────────────────────────────────────
    const USER_STEPS = [
        {
            selector: '.user-greeting, .user-greeting h1',
            title: 'Welcome to ResolviaAI! 👋',
            body: "This is your personal IT support dashboard. Everything about your tickets lives here.",
            position: 'bottom'
        },
        {
            selector: '.summary-container',
            title: 'Your Ticket Summary',
            body: "See all your tickets at a glance — total, open, and resolved. Click any card to filter the list.",
            position: 'bottom'
        },
        {
            selector: '.create-ticket-action a, a[href="create-ticket.html"]',
            title: 'Create a Ticket',
            body: "Click here to submit a new IT support ticket. AI will analyze it and generate a solution instantly.",
            position: 'bottom'
        },
        {
            selector: '#ticketTable, .ticket-list-card',
            title: 'Your Ticket History',
            body: "All your tickets appear here with their status. Click any resolved ticket to view the AI-generated solution.",
            position: 'top'
        },
        {
            selector: '.theme-toggle-btn',
            title: 'Dark / Light Mode',
            body: "Toggle between dark and light mode anytime. Your preference is saved automatically.",
            position: 'bottom'
        },
        {
            selector: '.user-profile, .nav-right .user-profile',
            title: 'Your Profile',
            body: "Click here to view your profile, check your ticket stats, and upload a profile photo.",
            position: 'bottom'
        }
    ];

    const ADMIN_STEPS = [
        {
            selector: '.admin-greeting, .admin-greeting h1',
            title: 'Welcome, Admin! 👋',
            body: "This is your ResolviaAI admin dashboard. You manage tickets that AI escalates to you.",
            position: 'bottom'
        },
        {
            selector: '.summary-container, .summary-card:first-child',
            title: 'System Overview',
            body: "These cards show all tickets, open ones, AI-resolved, and this month's count. Click any card to filter.",
            position: 'bottom'
        },
        {
            selector: 'a[href="ticket-management.html"]',
            title: 'Ticket Management',
            body: "This is where you review and resolve tickets that AI couldn't handle confidently. The red badge shows pending tickets.",
            position: 'right'
        },
        {
            selector: 'a[href="ai-decision-panel.html"]',
            title: 'AI Decision Panel',
            body: "Monitor AI performance — confidence scores, resolution time comparisons, and top ticket categories.",
            position: 'right'
        },
        {
            selector: '.chart-card, canvas',
            title: 'Live Analytics',
            body: "Real-time charts showing ticket distribution and monthly trends. Data updates on every page load.",
            position: 'top'
        },
        {
            selector: '.theme-toggle-btn',
            title: 'Dark / Light Mode',
            body: "Toggle display theme anytime. ResolviaAI works great in both modes.",
            position: 'bottom'
        },
        {
            selector: 'a[href="admin-profile.html"]',
            title: 'Your Admin Profile',
            body: "View your admin stats, manage your profile photo, and see users you've blocked or actioned.",
            position: 'right'
        }
    ];

    // ── Public entry point ─────────────────────────────────────────────────
    window.startOnboarding = function (role) {
        const steps = role === 'ADMIN' ? ADMIN_STEPS : USER_STEPS;
        runTour(steps);
    };

    // ── Auto-trigger on load (USER pages only) ────────────────────────────
    // Admin pages trigger from loadAdminProfile() after async userId is set
    document.addEventListener('DOMContentLoaded', function () {
        const role = localStorage.getItem('role');
        if (role === 'ADMIN') return; // Admins handled by runAdminTour()

        const userId = localStorage.getItem('currentUserId');
        if (!userId) return;

        const key   = 'onboardingCount_' + userId;
        const count = parseInt(localStorage.getItem(key) || '0');

        if (count < 2) {
            localStorage.setItem(key, count + 1);
            setTimeout(function () {
                runTour(USER_STEPS);
            }, 800);
        }
    });

    // ── Admin tour — called from loadAdminProfile() after userId is stored ──
    window.runAdminTour = function () {
        runTour(ADMIN_STEPS);
    };

    // ── Core tour engine ───────────────────────────────────────────────────
    function runTour(steps) {
        let currentStep = 0;

        // Create overlay
        const overlay = document.createElement('div');
        overlay.className = 'ob-overlay';
        overlay.onclick = function (e) {
            if (e.target === overlay) advance();
        };
        document.body.appendChild(overlay);

        // Create spotlight
        const spotlight = document.createElement('div');
        spotlight.className = 'ob-spotlight';
        document.body.appendChild(spotlight);

        // Create tooltip
        const tooltip = document.createElement('div');
        tooltip.className = 'ob-tooltip';
        document.body.appendChild(tooltip);

        function getValidSteps() {
            return steps.filter(function (s) {
                return !!document.querySelector(s.selector);
            });
        }

        function showStep(idx) {
            const validSteps = getValidSteps();
            if (idx >= validSteps.length) { destroy(); return; }
            const step = validSteps[idx];
            const el   = document.querySelector(step.selector);
            if (!el) { showStep(idx + 1); return; }

            // Scroll element into view
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });

            setTimeout(function () {
                const rect = el.getBoundingClientRect();
                const pad  = 8;

                // Position spotlight
                spotlight.style.left   = (rect.left   - pad) + 'px';
                spotlight.style.top    = (rect.top    - pad) + 'px';
                spotlight.style.width  = (rect.width  + pad * 2) + 'px';
                spotlight.style.height = (rect.height + pad * 2) + 'px';

                // Build tooltip content
                const total = validSteps.length;
                tooltip.innerHTML =
                    '<div class="ob-step-badge">Step ' + (idx + 1) + ' of ' + total + '</div>' +
                    '<h3 class="ob-title">' + step.title + '</h3>' +
                    '<p class="ob-body">' + step.body + '</p>' +
                    '<div class="ob-footer">' +
                        '<div class="ob-dots">' +
                            validSteps.map(function (_, i) {
                                return '<div class="ob-dot' + (i === idx ? ' active' : '') + '"></div>';
                            }).join('') +
                        '</div>' +
                        '<div style="display:flex;gap:0.5rem;align-items:center;">' +
                            '<button class="ob-btn-skip" id="obSkip">Skip Tour</button>' +
                            '<button class="ob-btn-next" id="obNext">' +
                                (idx === total - 1 ? "Finish 🎉" : "Next →") +
                            '</button>' +
                        '</div>' +
                    '</div>';

                document.getElementById('obSkip').onclick = destroy;
                document.getElementById('obNext').onclick = advance;

                // Position tooltip
                positionTooltip(rect, step.position || 'bottom');

            }, 350);
        }

        function positionTooltip(rect, position) {
            const tw = tooltip.offsetWidth  || 300;
            const th = tooltip.offsetHeight || 160;
            const vw = window.innerWidth;
            const vh = window.innerHeight;
            const pad = 16;
            let top, left;

            if (position === 'bottom') {
                top  = rect.bottom + 16;
                left = rect.left + rect.width / 2 - tw / 2;
            } else if (position === 'top') {
                top  = rect.top - th - 16;
                left = rect.left + rect.width / 2 - tw / 2;
            } else if (position === 'right') {
                top  = rect.top + rect.height / 2 - th / 2;
                left = rect.right + 16;
            } else {
                top  = rect.top + rect.height / 2 - th / 2;
                left = rect.left - tw - 16;
            }

            // Clamp within viewport
            left = Math.max(pad, Math.min(left, vw - tw - pad));
            top  = Math.max(pad, Math.min(top,  vh - th - pad));

            tooltip.style.left = left + 'px';
            tooltip.style.top  = top  + 'px';
        }

        function advance() {
            currentStep++;
            const validSteps = getValidSteps();
            if (currentStep >= validSteps.length) { destroy(); return; }
            showStep(currentStep);
        }

        function destroy() {
            [overlay, spotlight, tooltip].forEach(function (el) {
                if (el && el.parentNode) el.parentNode.removeChild(el);
            });
        }

        showStep(0);
    }

})();