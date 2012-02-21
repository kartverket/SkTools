package no.statkart.sktools.gradle.plugins.dbtools.filterproperties

import org.gradle.api.Plugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.Project
import java.util.Map.Entry
import no.statkart.sktools.gradle.plugins.dbtools.filterproperties.util.GradleUtil

/**
 * todo: dokumentasjon
 *
 *
 * @since 1.0
 * @author Thor Åge Eldby
 */
class FilterPropertiesPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.getPlugins().apply(JavaPlugin.class);
        FilterPropertiesConvention convention = new FilterPropertiesConvention()
        project.getConvention().getPlugins().put('statKartFilterProperties', convention)
        project.afterEvaluate {
            if (it.state.failure == null) {
                createFilterTasks(convention, project)
            }
        }
    }

    private def createFilterTasks(FilterPropertiesConvention convention, Project project) {
        convention.resources.each { Entry<String, String> entry ->
            String scope = entry.key
            String path = entry.value
            def unfilteredPropertiesDir = project.file(path)
            if (unfilteredPropertiesDir.isDirectory()) {
                def filteredResourcesDir = project.file("build/generated/${scope}/resources")
                String taskName = "filterResources$scope"
                project.task (taskName) {
                    inputs.dir(unfilteredPropertiesDir)
                    inputs.properties(convention.properties)
                    outputs.dir(filteredResourcesDir)
                    doLast {
                        // Ant copy forstår ikke at properties har endret seg; derfor må vi tvinge refilterering
                        project.delete(filteredResourcesDir);
                        project.copy {
                            from(unfilteredPropertiesDir)
                            into(filteredResourcesDir)
                            filter(org.apache.tools.ant.filters.ReplaceTokens, tokens: convention.properties)
                        }
                        project.ant.touch() { fileset(dir: filteredResourcesDir)}
                    }
                }
                project.sourceSets."$scope".resources.srcDirs -= unfilteredPropertiesDir
                project.sourceSets."$scope".resources.srcDir filteredResourcesDir
                project.processResources.dependsOn taskName
                GradleUtil.makeIdeaShowBuildDirectory(project)
            }
        }
    }
}
