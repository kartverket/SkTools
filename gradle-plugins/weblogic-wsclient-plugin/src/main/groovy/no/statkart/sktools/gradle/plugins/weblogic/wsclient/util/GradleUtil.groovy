package no.statkart.sktools.gradle.plugins.weblogic.wsclient.util

import org.gradle.api.Project

class GradleUtil {

    static void makeIdeaShowBuildDirectory(Project project) {
        project.idea.module.iml {
            whenMerged {
                it.excludeFolders = it.excludeFolders.findAll {!it.url.contains("build")}
            }
        }
    }

}
