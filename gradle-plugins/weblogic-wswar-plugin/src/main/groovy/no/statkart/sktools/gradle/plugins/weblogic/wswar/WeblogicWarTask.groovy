package no.statkart.sktools.gradle.plugins.weblogic.wswar

import org.gradle.api.tasks.bundling.Jar

/**
 * Task for generering av arkivfil
 *
 * @since 1.2
 * @author Leif Lislegård
 */
class WeblogicWarTask extends Jar {

    WeblogicWarTask() {
        extension = 'war'

        //tar bort evt duplikater etter LIFO prinsippet
        def paths = [:]
        eachFile {
//            println "visiting ${it.path}"
            if (paths[it.path]) {
                it.exclude()
//                println "duplicate  found !!!!!!!!!!!!! "
            }
            paths.put(it.path, it)
        }


    }


}
