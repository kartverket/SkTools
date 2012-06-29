package no.statkart.sktools.gradle.plugins.dbtools.database.util

import org.gradle.api.Project

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

    Closure defaultUsername
    Closure defaultPassword

    Credentials(String context, final Map<java.lang.String, ?> properties) {
        defaultUsername = {
            properties['username']
        }
        defaultPassword = {
            properties['password']
        }
        this.context = context
    }

    public boolean hasUsername() {
        if (username == null) {
            username = defaultUsername.call()
        }
        return username != null
    }

    public String getUsername() {
        if (!hasUsername()) {
            username = System.console()?.readLine('<%s>Please enter username: ', context)
        }
        return username
    }

    public boolean hasPassword() {
        if (password == null) {
            password = defaultPassword.call()
        }
        return password != null
    }

    public String getPassword() {
        if (!hasPassword()) {
            char[] pwd = System.console()?.readPassword('<%s>Please enter password for %s (empty defaults to %s): ', context, username, username)
            if (pwd != null) {
                password = new String(pwd)
                if (password.isEmpty()) {
                    password = username
                }
            }
        }
        return password
    }

    /**
     * @since 1.2
     */
    public void clear() {
        username = null
        password = null
    }

    /**
     * @since 1.2
     */
    public boolean isEmpty() {
        return !(hasUsername() || hasPassword())
    }


}
