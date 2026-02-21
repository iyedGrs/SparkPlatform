package com.spark.platform.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;
import com.spark.platform.models.User;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.*;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.commons.codec.binary.Base64;

/**
 * Email service using Gmail OAuth 2.0 for sending account notifications.
 * Handles welcome emails, password resets, and other notifications.
 */
public class EmailService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_SEND);

    private String clientId;
    private String clientSecret;
    private String applicationName;
    private String senderEmail;
    private String senderName;
    private String tokensDirectory;

    private Gmail gmailService;
    private boolean initialized = false;

    public EmailService() {
        loadConfiguration();
    }

    /**
     * Loads email configuration from email.properties file.
     */
    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (input == null) {
                System.err.println("[EmailService] email.properties not found. Email sending disabled.");
                return;
            }

            Properties props = new Properties();
            props.load(input);

            clientId = props.getProperty("gmail.client.id");
            clientSecret = props.getProperty("gmail.client.secret");
            applicationName = props.getProperty("gmail.application.name", "Spark Platform");
            senderEmail = props.getProperty("gmail.sender.email");
            senderName = props.getProperty("gmail.sender.name", "Spark Platform");
            tokensDirectory = props.getProperty("gmail.tokens.directory", "tokens");

            if (clientId == null || clientId.contains("YOUR_CLIENT_ID") ||
                clientSecret == null || clientSecret.contains("YOUR_CLIENT_SECRET")) {
                System.err.println("[EmailService] Gmail OAuth not configured. Email sending disabled.");
                return;
            }

            initialized = true;
            System.out.println("[EmailService] Configuration loaded successfully.");

        } catch (IOException e) {
            System.err.println("[EmailService] Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Gets Gmail API credentials using OAuth 2.0.
     */
    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        // Build client secrets from properties
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                .setClientId(clientId)
                .setClientSecret(clientSecret);

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(details);

        // Build authorization flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(tokensDirectory)))
                .setAccessType("offline")
                .build();

        // Authorize - this will open browser for first-time authorization
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8888)
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Gets or initializes the Gmail service.
     */
    private Gmail getGmailService() throws GeneralSecurityException, IOException {
        if (gmailService == null) {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            gmailService = new Gmail.Builder(httpTransport, JSON_FACTORY, getCredentials(httpTransport))
                    .setApplicationName(applicationName)
                    .build();
        }
        return gmailService;
    }

    /**
     * Checks if email service is properly configured and available.
     */
    public boolean isAvailable() {
        return initialized;
    }

    /**
     * Sends a welcome email to a newly created user with their credentials.
     */
    public boolean sendWelcomeEmail(User user, String password) {
        if (!initialized) {
            System.err.println("[EmailService] Not configured - skipping welcome email.");
            return false;
        }

        String subject = "Welcome to Spark Platform - Your Account Has Been Created";
        String htmlContent = buildWelcomeEmailHtml(user, password);

        return sendEmail(user.getEmail(), subject, htmlContent);
    }

    /**
     * Sends a password reset email to a user.
     */
    public boolean sendPasswordResetEmail(User user, String newPassword) {
        if (!initialized) {
            System.err.println("[EmailService] Not configured - skipping password reset email.");
            return false;
        }

        String subject = "Spark Platform - Your Password Has Been Reset";
        String htmlContent = buildPasswordResetEmailHtml(user, newPassword);

        return sendEmail(user.getEmail(), subject, htmlContent);
    }

    /**
     * Sends an account disabled notification email to a user.
     */
    public boolean sendAccountDisabledEmail(User user) {
        if (!initialized) {
            System.err.println("[EmailService] Not configured - skipping account disabled email.");
            return false;
        }

        String subject = "Spark Platform - Your Account Has Been Temporarily Disabled";
        String htmlContent = buildAccountDisabledEmailHtml(user);

        return sendEmail(user.getEmail(), subject, htmlContent);
    }

    /**
     * Core method to send an HTML email via Gmail API.
     */
    private boolean sendEmail(String recipientEmail, String subject, String htmlContent) {
        try {
            Gmail service = getGmailService();

            MimeMessage mimeMessage = createHtmlEmail(recipientEmail, subject, htmlContent);
            Message message = createMessageWithEmail(mimeMessage);

            service.users().messages().send("me", message).execute();

            System.out.println("[EmailService] Email sent successfully to: " + recipientEmail);
            return true;

        } catch (Exception e) {
            System.err.println("[EmailService] Failed to send email to " + recipientEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a MimeMessage for an HTML email.
     */
    private MimeMessage createHtmlEmail(String to, String subject, String htmlBody) throws MessagingException ,UnsupportedEncodingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(senderEmail, senderName, "UTF-8"));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject, "UTF-8");
        email.setContent(htmlBody, "text/html; charset=utf-8");

        return email;
    }

    /**
     * Converts a MimeMessage to a Gmail API Message.
     */
    private Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    // ═══════════════════════════════════════════════════════
    // HTML EMAIL TEMPLATES
    // ═══════════════════════════════════════════════════════

    /**
     * Builds the HTML content for a welcome email.
     */
    private String buildWelcomeEmailHtml(User user, String password) {
        String creationDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"));
        String roleDisplay = formatRole(user.getUserType());

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Welcome to Spark</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #F1F5F9;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #FFFFFF;">
                    <!-- Header with gradient -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #667EEA 0%%, #764BA2 50%%, #F093FB 100%%); padding: 40px 30px; text-align: center;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td style="text-align: center;">
                                        <div style="background-color: #FFFFFF; width: 60px; height: 60px; border-radius: 16px; display: inline-block; line-height: 60px;">
                                            <span style="font-size: 28px; font-weight: 700; color: #667EEA;">S</span>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding-top: 20px;">
                                        <h1 style="color: #FFFFFF; font-size: 28px; font-weight: 700; margin: 0;">Welcome to Spark!</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding-top: 8px;">
                                        <p style="color: rgba(255,255,255,0.9); font-size: 16px; margin: 0;">Your account has been created successfully</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Body -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <!-- Greeting -->
                            <p style="color: #1E293B; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                                Hello <strong>%s</strong>,
                            </p>
                            <p style="color: #475569; font-size: 15px; line-height: 1.6; margin: 0 0 24px 0;">
                                We're excited to have you on board! An administrator has created a <strong>%s</strong> account for you on Spark Platform. Below are your login credentials – please keep them safe.
                            </p>
                            
                            <!-- Credentials Card -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background: linear-gradient(135deg, #F8FAFC 0%%, #F1F5F9 100%%); border-radius: 12px; border: 1px solid #E2E8F0; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 24px;">
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td style="padding-bottom: 16px; border-bottom: 1px solid #E2E8F0;">
                                                    <p style="color: #64748B; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 4px 0;">Email Address</p>
                                                    <p style="color: #1E293B; font-size: 16px; font-weight: 500; margin: 0;">%s</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 16px 0; border-bottom: 1px solid #E2E8F0;">
                                                    <p style="color: #64748B; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 4px 0;">Temporary Password</p>
                                                    <p style="color: #667EEA; font-size: 18px; font-weight: 600; font-family: 'Courier New', monospace; background-color: #EEF2FF; padding: 8px 12px; border-radius: 6px; display: inline-block; margin: 4px 0 0 0;">%s</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding-top: 16px;">
                                                    <p style="color: #64748B; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 4px 0;">Account Type</p>
                                                    <span style="display: inline-block; background-color: %s; color: #FFFFFF; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600;">%s</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Account Details -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #F8FAFC; border-radius: 8px; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 16px 20px;">
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td width="50%%" style="padding: 8px 0;">
                                                    <p style="color: #64748B; font-size: 13px; margin: 0;">Account Created</p>
                                                    <p style="color: #1E293B; font-size: 14px; font-weight: 500; margin: 4px 0 0 0;">%s</p>
                                                </td>
                                                <td width="50%%" style="padding: 8px 0;">
                                                    <p style="color: #64748B; font-size: 13px; margin: 0;">Status</p>
                                                    <p style="color: #10B981; font-size: 14px; font-weight: 500; margin: 4px 0 0 0;">● Active</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Security Notice -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #FFFBEB; border: 1px solid #FDE68A; border-radius: 8px; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 16px 20px;">
                                        <table role="presentation" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td style="vertical-align: top; padding-right: 12px;">
                                                    <span style="font-size: 20px;">⚠️</span>
                                                </td>
                                                <td>
                                                    <p style="color: #92400E; font-size: 14px; font-weight: 600; margin: 0 0 4px 0;">Important Security Notice</p>
                                                    <p style="color: #A16207; font-size: 13px; line-height: 1.5; margin: 0;">We recommend changing your password after your first login. Keep your credentials confidential and never share them with others.</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- CTA Button -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="margin: 32px 0;">
                                <tr>
                                    <td style="text-align: center;">
                                        <p style="color: #475569; font-size: 14px; margin: 0 0 16px 0;">Ready to get started?</p>
                                        <a href="#" style="display: inline-block; background: linear-gradient(135deg, #667EEA 0%%, #764BA2 100%%); color: #FFFFFF; text-decoration: none; padding: 14px 32px; border-radius: 8px; font-size: 15px; font-weight: 600;">Open Spark Platform</a>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #F8FAFC; padding: 30px; border-top: 1px solid #E2E8F0;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td style="text-align: center;">
                                        <p style="color: #94A3B8; font-size: 13px; margin: 0 0 8px 0;">
                                            <strong style="color: #64748B;">Spark Platform</strong> • Smart Academic Management
                                        </p>
                                        <p style="color: #94A3B8; font-size: 12px; margin: 0;">
                                            This is an automated message. Please do not reply to this email.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                user.getName(),
                roleDisplay,
                user.getEmail(),
                password,
                getRoleBadgeColor(user.getUserType()),
                roleDisplay,
                creationDate
            );
    }

    /**
     * Builds the HTML content for a password reset email.
     */
    private String buildPasswordResetEmailHtml(User user, String newPassword) {
        String resetDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"));

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Reset - Spark Platform</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #F1F5F9;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #FFFFFF;">
                    <!-- Header with amber gradient -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #F59E0B 0%%, #D97706 50%%, #B45309 100%%); padding: 40px 30px; text-align: center;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td style="text-align: center;">
                                        <div style="background-color: #FFFFFF; width: 60px; height: 60px; border-radius: 50%%; display: inline-block; line-height: 60px;">
                                            <span style="font-size: 28px;">🔑</span>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding-top: 20px;">
                                        <h1 style="color: #FFFFFF; font-size: 28px; font-weight: 700; margin: 0;">Password Reset</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding-top: 8px;">
                                        <p style="color: rgba(255,255,255,0.9); font-size: 16px; margin: 0;">Your account password has been reset</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Body -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <!-- Greeting -->
                            <p style="color: #1E293B; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                                Hello <strong>%s</strong>,
                            </p>
                            <p style="color: #475569; font-size: 15px; line-height: 1.6; margin: 0 0 24px 0;">
                                An administrator has reset your password on Spark Platform. Your new temporary password is shown below. Please log in and change it as soon as possible.
                            </p>
                            
                            <!-- New Password Card -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background: linear-gradient(135deg, #FFFBEB 0%%, #FEF3C7 100%%); border-radius: 12px; border: 1px solid #FDE68A; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 24px; text-align: center;">
                                        <p style="color: #92400E; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 12px 0;">Your New Password</p>
                                        <p style="color: #B45309; font-size: 24px; font-weight: 600; font-family: 'Courier New', monospace; background-color: #FFFFFF; padding: 12px 24px; border-radius: 8px; display: inline-block; margin: 0; border: 2px dashed #FCD34D;">%s</p>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Reset Details -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #F8FAFC; border-radius: 8px; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 16px 20px;">
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td width="50%%" style="padding: 8px 0;">
                                                    <p style="color: #64748B; font-size: 13px; margin: 0;">Reset Date</p>
                                                    <p style="color: #1E293B; font-size: 14px; font-weight: 500; margin: 4px 0 0 0;">%s</p>
                                                </td>
                                                <td width="50%%" style="padding: 8px 0;">
                                                    <p style="color: #64748B; font-size: 13px; margin: 0;">Account</p>
                                                    <p style="color: #1E293B; font-size: 14px; font-weight: 500; margin: 4px 0 0 0;">%s</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Security Warning -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #FEF2F2; border: 1px solid #FECACA; border-radius: 8px; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 16px 20px;">
                                        <table role="presentation" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td style="vertical-align: top; padding-right: 12px;">
                                                    <span style="font-size: 20px;">🔒</span>
                                                </td>
                                                <td>
                                                    <p style="color: #991B1B; font-size: 14px; font-weight: 600; margin: 0 0 4px 0;">Security Reminder</p>
                                                    <p style="color: #B91C1C; font-size: 13px; line-height: 1.5; margin: 0;">If you did not request this password reset, please contact your administrator immediately. Never share your password with anyone.</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #F8FAFC; padding: 30px; border-top: 1px solid #E2E8F0;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td style="text-align: center;">
                                        <p style="color: #94A3B8; font-size: 13px; margin: 0 0 8px 0;">
                                            <strong style="color: #64748B;">Spark Platform</strong> • Smart Academic Management
                                        </p>
                                        <p style="color: #94A3B8; font-size: 12px; margin: 0;">
                                            This is an automated message. Please do not reply to this email.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                user.getName(),
                newPassword,
                resetDate,
                user.getEmail()
            );
    }

    /**
     * Builds the HTML content for an account disabled notification email.
     */
    private String buildAccountDisabledEmailHtml(User user) {
        String disabledDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a"));
        String roleDisplay = formatRole(user.getUserType());

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Account Disabled - Spark Platform</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: #F1F5F9;">
                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width: 600px; margin: 0 auto; background-color: #FFFFFF;">
                    <!-- Header with red/gray gradient -->
                    <tr>
                        <td style="background: linear-gradient(135deg, #6B7280 0%%, #4B5563 50%%, #374151 100%%); padding: 40px 30px; text-align: center;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td style="text-align: center;">
                                        <div style="background-color: #FFFFFF; width: 60px; height: 60px; border-radius: 50%%; display: inline-block; line-height: 60px;">
                                            <span style="font-size: 28px;">⚠️</span>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding-top: 20px;">
                                        <h1 style="color: #FFFFFF; font-size: 28px; font-weight: 700; margin: 0;">Account Temporarily Disabled</h1>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding-top: 8px;">
                                        <p style="color: rgba(255,255,255,0.9); font-size: 16px; margin: 0;">Important notice regarding your Spark account</p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                    
                    <!-- Body -->
                    <tr>
                        <td style="padding: 40px 30px;">
                            <!-- Greeting -->
                            <p style="color: #1E293B; font-size: 16px; line-height: 1.6; margin: 0 0 24px 0;">
                                Dear <strong>%s</strong>,
                            </p>
                            <p style="color: #475569; font-size: 15px; line-height: 1.6; margin: 0 0 24px 0;">
                                We regret to inform you that your account on <strong>Spark Platform</strong> has been <strong>temporarily disabled</strong> due to administrative policies.
                            </p>
                            
                            <!-- Account Info Card -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background: linear-gradient(135deg, #F8FAFC 0%%, #F1F5F9 100%%); border-radius: 12px; border: 1px solid #E2E8F0; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 24px;">
                                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td style="padding-bottom: 16px; border-bottom: 1px solid #E2E8F0;">
                                                    <p style="color: #64748B; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 4px 0;">Account Email</p>
                                                    <p style="color: #1E293B; font-size: 16px; font-weight: 500; margin: 0;">%s</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 16px 0; border-bottom: 1px solid #E2E8F0;">
                                                    <p style="color: #64748B; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 4px 0;">Account Type</p>
                                                    <span style="display: inline-block; background-color: %s; color: #FFFFFF; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600;">%s</span>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 16px 0; border-bottom: 1px solid #E2E8F0;">
                                                    <p style="color: #64748B; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 4px 0;">Disabled On</p>
                                                    <p style="color: #1E293B; font-size: 14px; font-weight: 500; margin: 0;">%s</p>
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding-top: 16px;">
                                                    <p style="color: #64748B; font-size: 12px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; margin: 0 0 4px 0;">Status</p>
                                                    <span style="display: inline-block; background-color: #EF4444; color: #FFFFFF; padding: 4px 12px; border-radius: 20px; font-size: 13px; font-weight: 600;">● Temporarily Disabled</span>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- What this means -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background-color: #FEF2F2; border: 1px solid #FECACA; border-radius: 8px; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 20px;">
                                        <p style="color: #991B1B; font-size: 14px; font-weight: 600; margin: 0 0 12px 0;">What does this mean?</p>
                                        <ul style="color: #B91C1C; font-size: 13px; line-height: 1.8; margin: 0; padding-left: 20px;">
                                            <li>You will no longer be able to log in to the platform</li>
                                            <li>Your data and progress have been archived</li>
                                            <li>This action was taken following administrative review</li>
                                        </ul>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Contact Info -->
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background: linear-gradient(135deg, #EEF2FF 0%%, #E0E7FF 100%%); border-radius: 12px; border: 1px solid #C7D2FE; margin: 24px 0;">
                                <tr>
                                    <td style="padding: 24px;">
                                        <table role="presentation" cellspacing="0" cellpadding="0">
                                            <tr>
                                                <td style="vertical-align: top; padding-right: 16px;">
                                                    <div style="background-color: #667EEA; width: 44px; height: 44px; border-radius: 50%%; display: inline-block; text-align: center; line-height: 44px;">
                                                        <span style="font-size: 20px;">📞</span>
                                                    </div>
                                                </td>
                                                <td>
                                                    <p style="color: #3730A3; font-size: 15px; font-weight: 600; margin: 0 0 8px 0;">Need Help? Contact ESPRIT Administration</p>
                                                    <p style="color: #4338CA; font-size: 14px; line-height: 1.6; margin: 0 0 12px 0;">
                                                        If you believe this action was taken in error or would like to discuss your account status, please contact the ESPRIT Administration Department.
                                                    </p>
                                                    <table role="presentation" cellspacing="0" cellpadding="0">
                                                        <tr>
                                                            <td style="padding: 4px 0;">
                                                                <span style="color: #6366F1; font-size: 13px;">📧 Email: </span>
                                                                <a href="mailto:admin@esprit.tn" style="color: #4F46E5; font-weight: 500; text-decoration: none;">admin@esprit.tn</a>
                                                            </td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding: 4px 0;">
                                                                <span style="color: #6366F1; font-size: 13px;">📍 Location: </span>
                                                                <span style="color: #4338CA; font-size: 13px;">ESPRIT Campus, Administration Building</span>
                                                            </td>
                                                        </tr>
                                                        <tr>
                                                            <td style="padding: 4px 0;">
                                                                <span style="color: #6366F1; font-size: 13px;">🕐 Hours: </span>
                                                                <span style="color: #4338CA; font-size: 13px;">Monday - Friday, 8:00 AM - 5:00 PM</span>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                            
                            <!-- Additional note -->
                            <p style="color: #64748B; font-size: 14px; line-height: 1.6; margin: 24px 0 0 0; text-align: center;">
                                Please have your account email address ready when contacting us for faster assistance.
                            </p>
                        </td>
                    </tr>
                    
                    <!-- Footer -->
                    <tr>
                        <td style="background-color: #F8FAFC; padding: 30px; border-top: 1px solid #E2E8F0;">
                            <table role="presentation" width="100%%" cellspacing="0" cellpadding="0">
                                <tr>
                                    <td style="text-align: center;">
                                        <p style="color: #94A3B8; font-size: 13px; margin: 0 0 8px 0;">
                                            <strong style="color: #64748B;">Spark Platform</strong> • Smart Academic Management
                                        </p>
                                        <p style="color: #94A3B8; font-size: 12px; margin: 0;">
                                            This is an automated message. Please do not reply to this email.
                                        </p>
                                        <p style="color: #94A3B8; font-size: 11px; margin: 12px 0 0 0;">
                                            © 2026 ESPRIT - École Supérieure Privée d'Ingénierie et de Technologies
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(
                user.getName(),
                user.getEmail(),
                getRoleBadgeColor(user.getUserType()),
                roleDisplay,
                disabledDate
            );
    }

    // ═══════════════════════════════════════════════════════
    // UTILITY METHODS
    // ═══════════════════════════════════════════════════════

    private String formatRole(String userType) {
        return switch (userType) {
            case "ADMINISTRATOR" -> "Administrator";
            case "TEACHER" -> "Teacher";
            case "STUDENT" -> "Student";
            default -> userType;
        };
    }

    private String getRoleBadgeColor(String userType) {
        return switch (userType) {
            case "ADMINISTRATOR" -> "#EF4444";
            case "TEACHER" -> "#3B82F6";
            case "STUDENT" -> "#10B981";
            default -> "#6B7280";
        };
    }
}
