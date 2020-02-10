package no.statkart.sktools.gradle.testutils.builder

import no.statkart.sktools.gradle.testutils.ProjectHelper
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

/**
 * Verktøy for bygging av gradle test prosjekter.
 *
 * Subklasser kan legge til plugin spesifik konfigurasjon.
 *
 * @author Leif Lislegård
 */
class GradleProjectBuilder<T extends GradleProjectBuilder<T>> {

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

    public static GradleProjectBuilder<? extends GradleProjectBuilder> builder() {
        return new GradleProjectBuilder();
    }

    public static GradleProjectBuilder<? extends GradleProjectBuilder> builder(String projectName) {
        return builder().withName(projectName)
    }

    public ProjectHelper build(Closure closure = null) {
        //forks a new project in a temp folder
        projectHelper = new ProjectHelper(builder.build())
        projectHelper.setProjectProperties(projectProperties)
        closures.each {
            projectHelper.configureProject it
        }
        projectHelper.configureProject closure
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


}
