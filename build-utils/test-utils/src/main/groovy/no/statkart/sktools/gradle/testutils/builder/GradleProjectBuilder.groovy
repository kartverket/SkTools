package no.statkart.sktools.gradle.testutils.builder

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.plugins.JavaPlugin
import org.gradle.plugins.ide.idea.IdeaPlugin
import no.statkart.sktools.gradle.testutils.ProjectHelper
import org.gradle.api.plugins.WarPlugin

/**
 * Verktøy for bygging av gradle test prosjekter.
 *
 * Subklasser kan legge til plugin spesifik konfigurasjon.
 *
 * @author Leif Lislegård
 */
class GradleProjectBuilder<T extends GradleProjectBuilder> {

    protected ProjectHelper projectHelper
    private ProjectBuilder builder

    protected final Vector<Closure> closures = new Vector<Closure>()
    protected LinkedHashMap projectProperties


    /**
     * Overrides i hver subklasse. Bruk {@link #builder() }-metode for isntansiering.
     */
    protected GradleProjectBuilder() {
        builder = ProjectBuilder.builder().withName(getClass().getSimpleName())
    }

    public static GradleProjectBuilder<GradleProjectBuilder> builder() {
        return new GradleProjectBuilder();
    }

    public static GradleProjectBuilder<GradleProjectBuilder> builder(String projectName) {
        return builder().withName(projectName)
    }

    public ProjectHelper build() {
        //forks a new project in a temp folder
        projectHelper = new ProjectHelper(builder.build())
        projectHelper.setProjectProperties(projectProperties)
        closures.each {
            it.delegate = projectHelper.project
            it()
        }
        return projectHelper
    }


    public T withName(String projectName) {
        builder.withName(projectName)
        return this
    }


    public T withParent(ProjectHelper projectHelper) {
        return withParent(projectHelper.project)
    }

    public T withParent(Project project) {
        builder.withParent(project)
        return this
    }

    public T withProjectDir(File dir) {
        builder.withProjectDir(dir)
        return this
    }

    public T withProjectProperties(Map properties) {
        if (projectProperties == null) {
            projectProperties = new LinkedHashMap(properties)
        } else {
            projectProperties.putAll(properties)
        }
        return this
    }


    public T applyJavaPlugin() {
        closures.add {
            apply plugin: JavaPlugin
        }
        return this
    }

    public T applyIdeaPlugin() {
        closures.add {
            apply plugin: IdeaPlugin
        }
        return this
    }

    public T applyWarPlugin() {
        closures.add {
            apply plugin: WarPlugin
        }
        return this
    }


}
