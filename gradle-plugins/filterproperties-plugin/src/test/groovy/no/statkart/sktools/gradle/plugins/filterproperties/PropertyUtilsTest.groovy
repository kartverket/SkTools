package no.statkart.sktools.gradle.plugins.filterproperties

import org.testng.annotations.Test
import no.statkart.sktools.gradle.testutils.ProjectHelper
import no.statkart.sktools.gradle.testutils.builder.FilterPropertiesProjectBuilder
import no.statkart.sktools.gradle.plugins.filterproperties.extention.PropertyUtils
import org.testng.Assert
import org.gradle.api.Project

/**
 * Test av {@link PropertyUtils}
 *
 * @author Leif Lislegård
 */
class PropertyUtilsTest {

    /**
     * Tester innlesing av properties ifra fil
     *
     * @see PropertyUtils#fromFile(Object)
     */
    @Test
    void testLoadProperties() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().build()
        projectHelper.project.file('custom.properties').withPrintWriter('UTF-8') {
            it.println('hei=hopp')
            it.println('hopp=hei')
        }

        PropertyUtils propertyUtils = new PropertyUtils(projectHelper.project);
        Map<String, ?> properties = propertyUtils.fromFile('custom.properties')

        Assert.assertEquals(2, properties.size(), "Antall properties")
        Assert.assertEquals('hopp', properties.get('hei'), "Forventet value")

    }


    /**
     * Tester substituering av properties
     *
     * @see PropertyUtils#assignPropertiesToProject(Map<java.lang.String,?>[])
     */
    @Test
    void testAssignPropertiesToProject() {
        //forks a new project in a temp folder
        Project project = FilterPropertiesProjectBuilder.builder().build().project;
        PropertyUtils propertyUtils = new PropertyUtils(project);

        Map<String, String> myProperties = ['hei':'hopp', 'hopp':'hei']
        propertyUtils.assignPropertiesToProject(myProperties);

        Assert.assertEquals('hopp', project.hei, "Forventet value")
        Assert.assertEquals('hei', project.properties['hopp'], "Forventet value")
    }


    /**
     * Tester ekspandering av properties
     *
     * @see PropertyUtils#expandProjectProperties()
     */
    @Test
    void testExpandProperties() {
        //forks a new project in a temp folder
        Project project = FilterPropertiesProjectBuilder.builder().build().project;
        PropertyUtils propertyUtils = new PropertyUtils(project);

        project.ext.hei = 'hei${hopp}!'
        project.ext.hopp = 'sann'
        project.ext.heiarop = '${hei} ${hei} ${hopp}!'

        propertyUtils.expandProjectProperties();

        Assert.assertEquals('sann', project.properties['hopp'], "Forventet value")
        Assert.assertEquals('heisann!', project.properties['hei'], "Forventet value")
        Assert.assertEquals('heisann! heisann! sann!', project.properties['heiarop'], "Forventet value")
    }

    /**
     * Tester at extension fungerer
     */
    @Test
    void testExtention() {
        Project project = FilterPropertiesProjectBuilder.builder().applyFilterPropertiesPlugin().build().project;

        Object extensionObject = project.getExtensions().getByName('propertyUtils')
        Assert.assertNotNull("Forventet instans")
        Assert.assertTrue(extensionObject instanceof PropertyUtils, "Forventet PropertyUtils klasse")

    }




}