package no.statkart.sktools.gradle.plugins.dbtools.database.util


import org.gradle.api.provider.Provider

/**
 * Setter username og password dersom ikke allerede angitt som parametere.
 *
 * @author Leif Lislegård
 * @since 1.0
 */
class Credentials {
    private final String context

    String username = null
    String password = null

    private Provider<String> defaultUsername
    private Provider<String> defaultPassword

    Credentials(String context, Provider<String> defaultUsername, Provider<String> defaultPassword) {
        this.context = context
        this.defaultUsername = defaultUsername
        this.defaultPassword = defaultPassword
    }

    public String getUsername() {
        return username ?: defaultUsername.getOrNull()
    }

    public String getPassword() {
        return password != null ? password : defaultPassword.getOrNull()
    }

    public void addFallbackUsername(Provider fallback) {
        defaultUsername = defaultUsername.orElse(fallback)
    }

    public void addFallbackPassword(Provider fallback) {
        defaultPassword = defaultPassword.orElse(fallback)
    }
}
