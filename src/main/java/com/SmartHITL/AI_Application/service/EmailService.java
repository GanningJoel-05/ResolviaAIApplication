package com.SmartHITL.AI_Application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // ── Email Verification ────────────────────────────────────────────────

    public void sendVerificationEmail(String toEmail, String recipientName, String token) {
        // baseUrl is already "http://localhost:8080/api" — so just append the path directly
        String verifyLink = baseUrl + "/auth/verify-email?token=" + token;

        String html = buildEmailHtml(
                "Verify Your ResolviaAI Account",
                "Hi " + recipientName + ",",
                "Thank you for registering with <strong>ResolviaAI</strong>. " +
                        "Please click the button below to verify your email address and activate your account.",
                "Verify Email Address",
                verifyLink,
                "This link will expire in <strong>24 hours</strong>. " +
                        "If you did not create an account, you can safely ignore this email.",
                "#4f46e5"
        );

        sendHtmlEmail(toEmail, "Verify Your ResolviaAI Account", html);
    }

    // ── Password Reset ────────────────────────────────────────────────────

    public void sendPasswordResetEmail(String toEmail, String recipientName, String token) {
        String resetLink = frontendUrl + "/reset-password.html?token=" + token;

        String html = buildEmailHtml(
                "Reset Your ResolviaAI Password",
                "Hi " + recipientName + ",",
                "We received a request to reset your <strong>ResolviaAI</strong> password. " +
                        "Click the button below to set a new password.",
                "Reset Password",
                resetLink,
                "This link will expire in <strong>15 minutes</strong>. " +
                        "If you did not request a password reset, please ignore this email — " +
                        "your password will remain unchanged.",
                "#ef4444"
        );

        sendHtmlEmail(toEmail, "Reset Your ResolviaAI Password", html);
    }

    // ── Ticket Resolved Notification ─────────────────────────────────────

    public void sendTicketResolvedEmail(String toEmail, String recipientName,
                                        String ticketNumber, String ticketTitle,
                                        String resolvedBy, String solutionPreview) {

        // Truncate long solutions for the email preview
        String preview = solutionPreview != null && solutionPreview.length() > 300
                ? solutionPreview.substring(0, 297) + "..."
                : (solutionPreview != null ? solutionPreview : "Please log in to view the full solution.");

        String viewLink = frontendUrl + "/ticket-solution.html";

        String bodyText = "Your support ticket has been resolved. Here's a quick summary:";

        String extraContent =
                "<div style='background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;" +
                        "padding:1.1rem 1.25rem;margin:16px 0;'>" +
                        "<div style='font-size:0.8rem;font-weight:700;text-transform:uppercase;" +
                        "letter-spacing:0.06em;color:#94a3b8;margin-bottom:0.6rem;'>Ticket</div>" +
                        "<div style='font-size:1rem;font-weight:700;color:#0f172a;margin-bottom:0.25rem;'>" +
                        escapeHtml(ticketTitle) +
                        "</div>" +
                        "<div style='font-size:0.82rem;color:#64748b;margin-bottom:0.85rem;'>" +
                        ticketNumber + " &nbsp;·&nbsp; Resolved by: <strong>" + escapeHtml(resolvedBy) + "</strong>" +
                        "</div>" +
                        "<div style='font-size:0.85rem;font-weight:600;color:#374151;margin-bottom:0.4rem;'>" +
                        "Solution Preview:" +
                        "</div>" +
                        "<div style='font-size:0.88rem;color:#475569;line-height:1.6;white-space:pre-line;'>" +
                        escapeHtml(preview) +
                        "</div>" +
                        "</div>";

        String html = buildEmailHtmlWithExtra(
                "Your Ticket Has Been Resolved ✅",
                "Hi " + recipientName + ",",
                bodyText,
                extraContent,
                "View Full Solution",
                viewLink,
                "Log in to ResolviaAI to view the complete step-by-step solution and rate your experience.",
                "#10b981"
        );

        sendHtmlEmail(toEmail, "✅ Ticket Resolved: " + ticketTitle, html);
    }

    // ── Private Helpers ───────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            throw new RuntimeException("Failed to send email. Please try again later.");
        }
    }

    private String buildEmailHtml(String title, String greeting, String bodyText,
                                  String buttonLabel, String buttonUrl,
                                  String footerNote, String buttonColor) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <title>%s</title>
            </head>
            <body style="margin:0;padding:0;background:#f8fafc;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="560" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:16px;
                                  box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">

                      <!-- Header -->
                      <tr>
                        <td style="background:linear-gradient(135deg,#4f46e5,#6366f1);
                                   padding:32px 40px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:800;
                                     letter-spacing:-0.5px;">
                            Resolvia<span style="color:#c4b5fd;">AI</span>
                          </h1>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px 40px 32px;">
                          <p style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0f172a;">
                            %s
                          </p>
                          <p style="margin:16px 0 24px;font-size:15px;color:#64748b;line-height:1.7;">
                            %s
                          </p>

                          <!-- CTA Button -->
                          <table cellpadding="0" cellspacing="0" style="margin:28px 0;">
                            <tr>
                              <td style="border-radius:10px;background:%s;">
                                <a href="%s"
                                   style="display:inline-block;padding:14px 32px;
                                          color:#ffffff;font-size:15px;font-weight:700;
                                          text-decoration:none;border-radius:10px;
                                          letter-spacing:0.2px;">
                                  %s
                                </a>
                              </td>
                            </tr>
                          </table>

                          <!-- Fallback link -->
                          <p style="margin:0 0 8px;font-size:13px;color:#94a3b8;">
                            If the button doesn't work, copy and paste this link:
                          </p>
                          <p style="margin:0 0 24px;font-size:12px;color:#4f46e5;word-break:break-all;">
                            <a href="%s" style="color:#4f46e5;">%s</a>
                          </p>

                          <!-- Footer note -->
                          <div style="background:#f8fafc;border-radius:8px;padding:16px 18px;
                                      border-left:3px solid #e2e8f0;">
                            <p style="margin:0;font-size:13px;color:#64748b;line-height:1.6;">
                              %s
                            </p>
                          </div>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f8fafc;padding:20px 40px;text-align:center;
                                   border-top:1px solid #e2e8f0;">
                          <p style="margin:0;font-size:12px;color:#94a3b8;">
                            © 2026 ResolviaAI · AI-Powered IT Support
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(title, greeting, bodyText, buttonColor, buttonUrl,
                buttonLabel, buttonUrl, buttonUrl, footerNote);
    }
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String buildEmailHtmlWithExtra(String title, String greeting, String bodyText,
                                           String extraHtml, String buttonLabel,
                                           String buttonUrl, String footerNote,
                                           String buttonColor) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8"><title>%s</title></head>
            <body style="margin:0;padding:0;background:#f8fafc;font-family:'Segoe UI',Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:40px 0;">
                <tr><td align="center">
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="background:#ffffff;border-radius:16px;box-shadow:0 4px 24px rgba(0,0,0,0.08);overflow:hidden;">
                    <tr>
                      <td style="background:linear-gradient(135deg,#4f46e5,#6366f1);padding:32px 40px;text-align:center;">
                        <h1 style="margin:0;color:#ffffff;font-size:24px;font-weight:800;">Resolvia<span style="color:#c4b5fd;">AI</span></h1>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:36px 40px 28px;">
                        <p style="margin:0 0 8px;font-size:22px;font-weight:700;color:#0f172a;">%s</p>
                        <p style="margin:12px 0 8px;font-size:15px;color:#64748b;line-height:1.7;">%s</p>
                        %s
                        <table cellpadding="0" cellspacing="0" style="margin:20px 0;">
                          <tr>
                            <td style="border-radius:10px;background:%s;">
                              <a href="%s" style="display:inline-block;padding:14px 32px;color:#ffffff;font-size:15px;font-weight:700;text-decoration:none;border-radius:10px;">%s</a>
                            </td>
                          </tr>
                        </table>
                        <div style="background:#f8fafc;border-radius:8px;padding:14px 18px;border-left:3px solid #e2e8f0;">
                          <p style="margin:0;font-size:13px;color:#64748b;line-height:1.6;">%s</p>
                        </div>
                      </td>
                    </tr>
                    <tr>
                      <td style="background:#f8fafc;padding:18px 40px;text-align:center;border-top:1px solid #e2e8f0;">
                        <p style="margin:0;font-size:12px;color:#94a3b8;">© 2026 ResolviaAI · AI-Powered IT Support</p>
                      </td>
                    </tr>
                  </table>
                </td></tr>
              </table>
            </body></html>
            """.formatted(title, greeting, bodyText, extraHtml, buttonColor, buttonUrl, buttonLabel, footerNote);
    }

}