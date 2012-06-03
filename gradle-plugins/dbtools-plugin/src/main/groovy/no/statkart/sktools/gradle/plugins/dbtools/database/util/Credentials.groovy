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

    String username
    String password



    Credentials(Project project, String context) {
        username = project.ext.properties['username']
        password = project.ext.properties['password']
        this.context = context
    }

    public boolean hasUsername() {
        return username != null
    }

    public String getUsername() {
        if (!hasUsername()) {
            username = System.console().readLine('<%s>Please enter username: ', context)
        }
        return username
    }

    public boolean hasPassword() {
        return password != null
    }

    public String getPassword() {
        if (!hasPassword()) {
            password = new String(System.console().readPassword('<%s>Please enter password for %s (empty defaults to %s): ', context, username, username))
            if (password.isEmpty()) {
                password = username
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
