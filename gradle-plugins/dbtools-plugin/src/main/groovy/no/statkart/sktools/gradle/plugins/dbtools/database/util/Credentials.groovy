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

    private List<Provider<String>> defaultUsername = new ArrayList<>()
    private List<Provider<String>> defaultPassword = new ArrayList<>()

    Credentials(String context, Provider<String> defaultUsername, Provider<String> defaultPassword) {
        this.context = context
        addFallbackUsername(defaultUsername)
        addFallbackPassword(defaultPassword)
    }

    public String getUsername() {
        if (username != null) return username;
        for (Provider<String> fallbackProvider : defaultUsername) {
            def fallback = fallbackProvider.getOrNull()
            if (fallback != null) return fallback
        }
        return null
    }

    public String getPassword() {
        if (password != null) return password
        for (Provider<String> fallbackProvider : defaultPassword) {
            def fallback = fallbackProvider.getOrNull()
            if (fallback != null) return fallback
        }
        return null
    }

    public void addFallbackUsername(Provider fallback) {
        defaultUsername.add(fallback)
    }

    public void addFallbackPassword(Provider fallback) {
        defaultPassword.add(fallback)
    }
}
