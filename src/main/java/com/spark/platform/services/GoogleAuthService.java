package com.spark.platform.services;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import com.spark.platform.models.User;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

/**
 * Service for handling Google OAuth 2.0 authentication.
 * Allows users to sign in with their Google accounts.
 */
public class GoogleAuthService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Arrays.asList(
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile"
    );

    private String clientId;
    private String clientSecret;
    private String applicationName;
    private String tokensDirectory;

    private boolean initialized = false;
    private final UserService userService = new UserService();

    public GoogleAuthService() {
        loadConfiguration();
    }

    /**
     * Loads OAuth configuration from email.properties file.
     */
    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("email.properties")) {
            if (input == null) {
                System.err.println("[GoogleAuth] email.properties not found. Google sign-in disabled.");
                return;
            }

            Properties props = new Properties();
            props.load(input);

            clientId = props.getProperty("gmail.client.id");
            clientSecret = props.getProperty("gmail.client.secret");
            applicationName = props.getProperty("gmail.application.name", "Spark Platform");
            tokensDirectory = props.getProperty("gmail.tokens.directory", "tokens");

            if (clientId == null || clientId.contains("YOUR_CLIENT_ID") ||
                clientSecret == null || clientSecret.contains("YOUR_CLIENT_SECRET")) {
                System.err.println("[GoogleAuth] Gmail OAuth not configured. Google sign-in disabled.");
                return;
            }

            initialized = true;
            System.out.println("[GoogleAuth] Configuration loaded successfully.");

        } catch (IOException e) {
            System.err.println("[GoogleAuth] Failed to load configuration: " + e.getMessage());
        }
    }

    /**
     * Checks if Google authentication is available.
     */
    public boolean isAvailable() {
        return initialized;
    }

    /**
     * Initiates Google OAuth flow and returns the authenticated user if found in database.
     * 
     * @param allowedRoles Array of allowed user types (e.g., "ADMINISTRATOR", "TEACHER")
     * @return User object if authentication successful and user exists with allowed role, null otherwise
     * @throws GoogleAuthException if authentication fails
     */
    public GoogleAuthResult authenticateWithGoogle(String... allowedRoles) throws GoogleAuthException {
        return authenticateWithGoogle(false, allowedRoles);
    }

    /**
     * Initiates Google OAuth flow with option to force account selection.
     * 
     * @param forceAccountSelection If true, clears stored credentials to force account picker
     * @param allowedRoles Array of allowed user types (e.g., "ADMINISTRATOR", "TEACHER")
     * @return GoogleAuthResult with authentication status and user info
     * @throws GoogleAuthException if authentication fails
     */
    public GoogleAuthResult authenticateWithGoogle(boolean forceAccountSelection, String... allowedRoles) throws GoogleAuthException {
        if (!initialized) {
            throw new GoogleAuthException("Google authentication not configured");
        }

        // Clear stored credentials if forced account selection requested
        if (forceAccountSelection) {
            clearStoredCredentials();
        }

        try {
            // Get credentials via OAuth flow
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential credential = getCredentials(httpTransport);

            // Get user info from Google
            Oauth2 oauth2 = new Oauth2.Builder(httpTransport, JSON_FACTORY, credential)
                    .setApplicationName(applicationName)
                    .build();

            Userinfo userinfo = oauth2.userinfo().get().execute();
            String email = userinfo.getEmail();
            String name = userinfo.getName();
            String picture = userinfo.getPicture();

            System.out.println("[GoogleAuth] Google user authenticated: " + email);

            // Check if user exists in our database
            User user = userService.findByEmail(email);

            if (user == null) {
                return new GoogleAuthResult(
                        GoogleAuthResult.Status.USER_NOT_FOUND,
                        null,
                        email,
                        name,
                        "No account found with email: " + email
                );
            }

            // Check if user has an allowed role
            boolean hasAllowedRole = false;
            if (allowedRoles != null && allowedRoles.length > 0) {
                for (String role : allowedRoles) {
                    if (role.equals(user.getUserType())) {
                        hasAllowedRole = true;
                        break;
                    }
                }
            } else {
                hasAllowedRole = true; // No role restriction
            }

            if (!hasAllowedRole) {
                return new GoogleAuthResult(
                        GoogleAuthResult.Status.UNAUTHORIZED_ROLE,
                        user,
                        email,
                        name,
                        "Your account type (" + user.getUserType() + ") is not authorized for Google sign-in"
                );
            }

            // Check if account is active
            if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                return new GoogleAuthResult(
                        GoogleAuthResult.Status.ACCOUNT_DISABLED,
                        user,
                        email,
                        name,
                        "Your account has been disabled"
                );
            }

            // Success!
            return new GoogleAuthResult(
                    GoogleAuthResult.Status.SUCCESS,
                    user,
                    email,
                    name,
                    null
            );

        } catch (Exception e) {
            System.err.println("[GoogleAuth] Authentication failed: " + e.getMessage());
            throw new GoogleAuthException("Google authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets OAuth credentials, opening browser for authorization if needed.
     */
    private Credential getCredentials(final NetHttpTransport httpTransport) throws IOException {
        // Build client secrets
        GoogleClientSecrets.Details details = new GoogleClientSecrets.Details()
                .setClientId(clientId)
                .setClientSecret(clientSecret);

        GoogleClientSecrets clientSecrets = new GoogleClientSecrets()
                .setInstalled(details);

        // Use different token store for login vs email to avoid conflicts
        File tokenStore = new File(tokensDirectory + "/login");
        tokenStore.mkdirs();

        // Build authorization flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(tokenStore))
                .setAccessType("offline")
                .build();

        // Start local server for OAuth callback
        LocalServerReceiver receiver = new LocalServerReceiver.Builder()
                .setPort(8889) // Different port from email service
                .build();

        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Clears stored credentials to force re-authentication.
     */
    public void clearStoredCredentials() {
        try {
            File tokenStore = new File(tokensDirectory + "/login");
            if (tokenStore.exists()) {
                for (File file : tokenStore.listFiles()) {
                    file.delete();
                }
                System.out.println("[GoogleAuth] Stored credentials cleared.");
            }
        } catch (Exception e) {
            System.err.println("[GoogleAuth] Failed to clear credentials: " + e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════
    // RESULT AND EXCEPTION CLASSES
    // ═══════════════════════════════════════════════════════

    /**
     * Result of a Google authentication attempt.
     */
    public static class GoogleAuthResult {
        public enum Status {
            SUCCESS,
            USER_NOT_FOUND,
            UNAUTHORIZED_ROLE,
            ACCOUNT_DISABLED
        }

        private final Status status;
        private final User user;
        private final String googleEmail;
        private final String googleName;
        private final String errorMessage;

        public GoogleAuthResult(Status status, User user, String googleEmail, String googleName, String errorMessage) {
            this.status = status;
            this.user = user;
            this.googleEmail = googleEmail;
            this.googleName = googleName;
            this.errorMessage = errorMessage;
        }

        public Status getStatus() { return status; }
        public User getUser() { return user; }
        public String getGoogleEmail() { return googleEmail; }
        public String getGoogleName() { return googleName; }
        public String getErrorMessage() { return errorMessage; }
        public boolean isSuccess() { return status == Status.SUCCESS; }
    }

    /**
     * Exception thrown when Google authentication fails.
     */
    public static class GoogleAuthException extends Exception {
        public GoogleAuthException(String message) {
            super(message);
        }

        public GoogleAuthException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
