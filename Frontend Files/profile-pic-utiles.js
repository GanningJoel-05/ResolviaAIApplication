// =======================================================
// profile-pic-utils.js — ResolviaAI Profile Picture
// Include this on: profile.html, admin-profile.html,
//                  dashboard.js (via common), admin-dashboard.js
// =======================================================

// ── Storage Key Helpers ──────────────────────────────
function picKey(email)     { return "profilePic_"     + email; }
function privacyKey(email) { return "profilePicPrivacy_" + email; }

// ── Save profile picture (base64) ───────────────────
function saveProfilePic(email, base64) {
    try { localStorage.setItem(picKey(email), base64); return true; }
    catch(e) { console.error("Could not save profile pic:", e); return false; }
}

// ── Get profile picture ──────────────────────────────
function getProfilePic(email) {
    return localStorage.getItem(picKey(email)) || null;
}

// ── Save privacy setting ─────────────────────────────
// value: "public" | "private"
function savePrivacy(email, value) {
    localStorage.setItem(privacyKey(email), value);
}

// ── Get privacy setting ──────────────────────────────
function getPrivacy(email) {
    return localStorage.getItem(privacyKey(email)) || "public";
}

// ── Apply pic to any avatar element ─────────────────
// If pic exists: show <img>, hide initials text
// avatarEl = the circle div, initial = fallback letter
function applyPicToAvatar(avatarEl, email, initial) {
    if (!avatarEl) return;
    const pic = getProfilePic(email);
    if (pic) {
        avatarEl.innerHTML = `<img src="${pic}" alt="Profile"
            style="width:100%;height:100%;object-fit:cover;border-radius:50%;">`;
        avatarEl.style.padding = "0";
    } else {
        avatarEl.innerHTML = initial || "?";
        avatarEl.style.padding = "";
    }
}

// ── File → Base64 converter ──────────────────────────
function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        // Limit to 2MB
        if (file.size > 2 * 1024 * 1024) {
            reject(new Error("Image must be under 2MB"));
            return;
        }
        if (!file.type.startsWith("image/")) {
            reject(new Error("Please select an image file"));
            return;
        }
        const reader = new FileReader();
        reader.onload  = e => resolve(e.target.result);
        reader.onerror = () => reject(new Error("Failed to read file"));
        reader.readAsDataURL(file);
    });
}