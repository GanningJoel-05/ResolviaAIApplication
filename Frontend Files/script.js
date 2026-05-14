// ======================================================
// script.js — ResolviaAI Auth Pages
// Login, Register, Forgot Password, Reset Password
// ======================================================

// ======================================================
// PASSWORD STRENGTH CHECK
// ======================================================

function checkPasswordStrength(password) {
    var bar = document.getElementById("strengthBar");
    if (!bar) return;

    var strength = 0;
    if (password.length >= 8) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/[0-9]/.test(password)) strength++;
    if (/[^A-Za-z0-9]/.test(password)) strength++;

    bar.className = "password-strength-bar";
    if (strength <= 1) bar.classList.add("pass-weak");
    else if (strength === 2 || strength === 3) bar.classList.add("pass-medium");
    else bar.classList.add("pass-strong");
}

// ======================================================
// LOGIN
// ======================================================

async function handleLogin(event) {
    event.preventDefault();

    var email    = document.getElementById("email").value;
    var password = document.getElementById("password").value;

    try {
        var response = await fetch(API_BASE_URL + "/auth/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email, password: password })
        });

        var data = await response.json();

        if (!response.ok) {
            var msg = data.message || "Invalid credentials";

            // Email not yet verified
            if (msg.startsWith("EMAIL_NOT_VERIFIED")) {
                showEmailNotVerifiedBanner(email);
                return;
            }

            showToast(msg, "error");
            return;
        }

        // Store auth info
        localStorage.setItem("token", data.token);
        localStorage.setItem("role",  data.role);

        // Redirect by role
        if (data.role === "ADMIN") {
            window.location.href = "admin-dashboard.html";
        } else {
            window.location.href = "dashboard.html";
        }

    } catch (error) {
        console.error(error);
        showToast("Server connection error. Is the backend running?", "error");
    }
}

function showEmailNotVerifiedBanner(email) {
    var existing = document.getElementById("verifyBanner");
    if (existing) existing.remove();

    var banner = document.createElement("div");
    banner.id = "verifyBanner";
    banner.style.cssText =
        "background:#fffbeb;border:1px solid #fde68a;" +
        "border-left:4px solid #f59e0b;border-radius:10px;" +
        "padding:1rem 1.25rem;margin-top:1rem;" +
        "font-size:0.88rem;color:#92400e;line-height:1.6;";
    banner.innerHTML =
        "<strong>📧 Email not verified</strong><br>" +
        "Please check your inbox and click the verification link before logging in.<br><br>" +
        "<span style='font-size:0.82rem;'>Didn't receive it? " +
        "<a href='#' id='resendVerifyBtn' style='color:#4f46e5;font-weight:700;text-decoration:none;'>" +
        "Resend verification email</a></span>";

    var form = document.getElementById("loginForm");
    if (form) form.appendChild(banner);

    document.getElementById("resendVerifyBtn").onclick = async function(e) {
        e.preventDefault();
        try {
            var res = await fetch(API_BASE_URL + "/auth/resend-verification", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ email: email })
            });
            var d = await res.json();
            showToast(d.message || "Verification email sent!", "success");
        } catch(err) {
            showToast("Server error. Please try again.", "error");
        }
    };
}

// ======================================================
// REGISTER
// ======================================================

var ADMIN_PASSKEY = "RESOLVIA@ADMIN2026";

async function handleRegister(event) {
    event.preventDefault();

    var name     = document.getElementById("fullName").value.trim();
    var email    = document.getElementById("email").value.trim();
    var password = document.getElementById("regPassword").value;
    var confirm  = document.getElementById("regConfirmPassword").value;
    var role     = document.getElementById("role").value;

    if (!role) {
        showToast("Please select a role.", "warning");
        return false;
    }

    // Domain whitelist
    var emailLower    = email.toLowerCase();
    var emailDomain   = emailLower.split("@")[1] || "";
    var allowedDomains = ["gmail.com", "psnacet.edu.in"];

    if (!allowedDomains.includes(emailDomain)) {
        showToast("Only @gmail.com (User) or @psnacet.edu.in (Admin) email addresses are allowed.", "error");
        return false;
    }
    if (role === "user" && emailDomain !== "gmail.com") {
        showToast("User accounts must use a @gmail.com email address.", "error");
        return false;
    }
    if (role === "admin" && emailDomain !== "psnacet.edu.in") {
        showToast("Admin accounts must use a @psnacet.edu.in email address.", "error");
        return false;
    }

    // Password strength
    var passwordRegex = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,}$/;
    if (!passwordRegex.test(password)) {
        showToast("Password must be at least 8 characters with letters, numbers & symbols.", "warning");
        return false;
    }

    // Password match
    if (password !== confirm) {
        document.getElementById("regMatchError").style.display = "block";
        return false;
    }

    // Admin passkey check
    if (role.toUpperCase() === "ADMIN") {
        if (!email.endsWith("@psnacet.edu.in")) {
            showToast("Only @psnacet.edu.in email IDs can register as ADMIN.", "error");
            return false;
        }
        var enteredPasskey = document.getElementById("adminPasskey").value;
        if (!enteredPasskey) {
            showToast("Admin passkey is required to register as Admin.", "warning");
            return false;
        }
        if (enteredPasskey !== ADMIN_PASSKEY) {
            showToast("Invalid admin passkey. Please contact your system administrator.", "error");
            return false;
        }
    }

    // Get username value
    var usernameField = document.getElementById("username");
    var username = usernameField ? usernameField.value.toLowerCase().trim() : "";

    try {
        var response = await fetch(API_BASE_URL + "/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                name:     name,
                username: username,
                email:    email,
                password: password,
                role:     role.toUpperCase()
            })
        });

        var data = await response.json();

        if (!response.ok) {
            var errMsg = data.message || "Registration failed";
            if (errMsg.toLowerCase().includes("already registered") ||
                errMsg.toLowerCase().includes("email")) {
                showToast("This email is already registered. Please use a different email or login instead.", "error");
            } else if (errMsg.toLowerCase().includes("username")) {
                showToast(errMsg, "error");
                if (usernameField) usernameField.focus();
            } else {
                showToast(errMsg, "error");
            }
            return false;
        }

        // Success — show "check your email" state
        showRegistrationSuccess(email);

    } catch (error) {
        showToast("Server error. Please try again.", "error");
    }

    return false;
}

function showRegistrationSuccess(email) {
    var container = document.querySelector(".auth-form-container");
    if (!container) return;

    container.innerHTML =
        "<a href='index.html' class='logo' style='display:block;margin-bottom:2rem;'>" +
            "Resolvia<span>AI</span>" +
        "</a>" +
        "<div style='text-align:center;'>" +
            "<div style='font-size:3.5rem;margin-bottom:1.25rem;'>📧</div>" +
            "<h2 style='font-size:1.75rem;font-weight:800;color:var(--text-main);" +
                "letter-spacing:-0.03em;margin-bottom:0.5rem;'>Check Your Email!</h2>" +
            "<p style='color:var(--text-muted);font-size:0.95rem;line-height:1.65;" +
                "margin-bottom:1.75rem;max-width:340px;margin-left:auto;margin-right:auto;'>" +
                "We've sent a verification link to<br>" +
                "<strong style='color:var(--text-main);'>" + email + "</strong><br><br>" +
                "Click the link in that email to verify your account and complete registration. " +
                "The link expires in <strong>24 hours</strong>." +
            "</p>" +
            "<div style='background:#f0fdf4;border:1px solid #bbf7d0;border-radius:10px;" +
                "padding:0.9rem 1.1rem;margin-bottom:1.5rem;font-size:0.85rem;" +
                "color:#166534;line-height:1.55;'>" +
                "✅ Check your spam/junk folder if you don't see the email within a few minutes." +
            "</div>" +
            "<p style='font-size:0.85rem;color:var(--text-muted);'>" +
                "Didn't get it? " +
                "<a href='#' id='resendFromRegister' style='color:var(--primary);" +
                    "font-weight:700;text-decoration:none;'>Resend email</a>" +
            "</p>" +
            "<p style='margin-top:1.5rem;'>" +
                "<a href='login.html' style='font-size:0.88rem;color:var(--text-muted);" +
                    "text-decoration:none;'>← Back to Login</a>" +
            "</p>" +
        "</div>";

    var resendBtn = document.getElementById("resendFromRegister");
    if (resendBtn) {
        resendBtn.onclick = async function(e) {
            e.preventDefault();
            try {
                var res = await fetch(API_BASE_URL + "/auth/resend-verification", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email: email })
                });
                var d = await res.json();
                showToast(d.message || "Verification email sent!", "success");
            } catch(err) {
                showToast("Server error. Please try again.", "error");
            }
        };
    }
}

// ======================================================
// FORGOT PASSWORD
// ======================================================

async function handleForgotPassword(event) {
    event.preventDefault();

    var email = document.getElementById("email").value.trim();
    if (!email) {
        showToast("Please enter your email address.", "warning");
        return;
    }

    var btn = event.target.querySelector("button[type=submit]");
    if (btn) { btn.disabled = true; btn.textContent = "Sending..."; }

    try {
        var response = await fetch(API_BASE_URL + "/auth/forgot-password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: email })
        });

        var data = await response.json();

        if (response.status === 404) {
            showToast("No account found with this email address. Please check and try again.", "error");
            if (btn) { btn.disabled = false; btn.textContent = "Send Reset Link"; }
            return;
        }

        if (response.status === 403) {
            var msg403 = data.message || "";
            if (msg403.startsWith("EMAIL_NOT_VERIFIED")) {
                showForgotPasswordUnverifiedBanner(email);
                if (btn) { btn.disabled = false; btn.textContent = "Send Reset Link"; }
                return;
            }
        }

        if (!response.ok) {
            showToast(data.message || "Something went wrong. Please try again.", "error");
            if (btn) { btn.disabled = false; btn.textContent = "Send Reset Link"; }
            return;
        }

        // Show "check your email" success state
        showForgotPasswordSuccess(email);

    } catch (error) {
        showToast("Server connection error. Is the backend running?", "error");
        if (btn) { btn.disabled = false; btn.textContent = "Send Reset Link"; }
    }
}

function showForgotPasswordUnverifiedBanner(email) {
    var existing = document.getElementById("forgotUnverifiedBanner");
    if (existing) existing.remove();

    var banner = document.createElement("div");
    banner.id = "forgotUnverifiedBanner";
    banner.style.cssText =
        "background:#fffbeb;border:1px solid #fde68a;" +
        "border-left:4px solid #f59e0b;border-radius:10px;" +
        "padding:1rem 1.25rem;margin-top:1rem;" +
        "font-size:0.88rem;color:#92400e;line-height:1.6;";
    banner.innerHTML =
        "<strong>📧 Email not verified</strong><br>" +
        "Your account email has not been verified yet. " +
        "You must verify your email before resetting your password.<br><br>" +
        "<span style='font-size:0.82rem;'>" +
        "<a href='#' id='resendFromForgot' style='color:#4f46e5;font-weight:700;text-decoration:none;'>" +
        "Resend verification email</a></span>";

    var form = document.getElementById("forgotForm");
    if (form) form.appendChild(banner);

    var resendBtn = document.getElementById("resendFromForgot");
    if (resendBtn) {
        resendBtn.onclick = async function(e) {
            e.preventDefault();
            try {
                var res = await fetch(API_BASE_URL + "/auth/resend-verification", {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email: email })
                });
                var d = await res.json();
                showToast(d.message || "Verification email sent!", "success");
            } catch(err) {
                showToast("Server error. Please try again.", "error");
            }
        };
    }
}

function showForgotPasswordSuccess(email) {
    var container = document.querySelector(".auth-form-container");
    if (!container) return;

    container.innerHTML =
        "<a href='index.html' class='logo' style='display:block;margin-bottom:2rem;'>" +
            "Resolvia<span>AI</span>" +
        "</a>" +
        "<div style='text-align:center;'>" +
            "<div style='font-size:3.5rem;margin-bottom:1.25rem;'>📬</div>" +
            "<h2 style='font-size:1.75rem;font-weight:800;color:var(--text-main);" +
                "letter-spacing:-0.03em;margin-bottom:0.5rem;'>Check Your Email!</h2>" +
            "<p style='color:var(--text-muted);font-size:0.95rem;line-height:1.65;" +
                "margin-bottom:1.75rem;max-width:340px;margin-left:auto;margin-right:auto;'>" +
                "We've sent a password reset link to<br>" +
                "<strong style='color:var(--text-main);'>" + email + "</strong><br><br>" +
                "Click the link in that email to set a new password. " +
                "The link expires in <strong>15 minutes</strong>." +
            "</p>" +
            "<div style='background:#eff6ff;border:1px solid #bfdbfe;border-radius:10px;" +
                "padding:0.9rem 1.1rem;margin-bottom:1.5rem;font-size:0.85rem;" +
                "color:#1e40af;line-height:1.55;'>" +
                "💡 Check your spam/junk folder if you don't see the email within a few minutes." +
            "</div>" +
            "<p style='margin-top:1.5rem;'>" +
                "<a href='login.html' style='font-size:0.88rem;color:var(--text-muted);" +
                    "text-decoration:none;'>← Back to Login</a>" +
            "</p>" +
        "</div>";
}

// ======================================================
// RESET PASSWORD
// Reads ?token= from URL (sent via email link)
// ======================================================

async function handleResetPassword(event) {
    event.preventDefault();

    var password = document.getElementById("newPassword").value;
    var confirm  = document.getElementById("confirmPassword").value;
    var params   = new URLSearchParams(window.location.search);
    var token    = params.get("token");

    if (!token) {
        showToast("Invalid reset link. Please use the link from your email.", "error");
        return false;
    }

    if (password !== confirm) {
        document.getElementById("matchError").style.display = "block";
        return false;
    }

    try {
        var response = await fetch(API_BASE_URL + "/auth/reset-password", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ token: token, newPassword: password })
        });

        var data = await response.json();

        if (!response.ok) {
            showToast(data.message || "Password reset failed. Please request a new link.", "error");
            return false;
        }

        showToast("Password updated successfully! Redirecting to login...", "success");
        setTimeout(function() { window.location.href = "login.html"; }, 1800);

    } catch (error) {
        showToast("Server error. Please try again.", "error");
    }

    return false;
}