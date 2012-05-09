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

        [['custom.properties'], ['custom.properties', 'noneExistant.properties']].each {
            Map<String, ?> properties = propertyUtils.fromFile(it as String[])

            Assert.assertEquals(properties.size(), 2, "Antall properties")
            Assert.assertEquals(properties.get('hei'), 'hopp', "Forventet value")
        }

    }

    /**
     * Tester innlesing av properties ifra fil som ikke finnes
     */
    @Test
    void testLoadPropertiesNoneExistantResource() {
        //forks a new project in a temp folder
        ProjectHelper projectHelper = FilterPropertiesProjectBuilder.builder().build()

        PropertyUtils propertyUtils = new PropertyUtils(projectHelper.project);
        Map<String, ?> properties = propertyUtils.fromFile('noneExistant.properties')

        Assert.assertNotNull(properties, 'properties')
        Assert.assertEquals(properties.size(), 0, "Antall properties")
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

        Assert.assertEquals(project.hei, 'hopp', "Forventet value")
        Assert.assertEquals(project.properties['hopp'], 'hei', "Forventet value")
    }


    /**
     * Tester ekspandering av properties
     *
     * @see PropertyUtils#expandProjectProperties()
     */
    @Test
    void testExpandProjectProperties() {
        //forks a new project in a temp folder
        Project project = FilterPropertiesProjectBuilder.builder().build().project;
        PropertyUtils propertyUtils = new PropertyUtils(project);

        project.ext.hei = 'hei${hopp}!'
        project.ext.hopp = 'sann'
        project.ext.heiarop = '${hei} ${hei} ${hopp}!'

        propertyUtils.expandProjectProperties();

        Assert.assertEquals(project.properties['hopp'], 'sann', "Forventet value")
        Assert.assertEquals(project.properties['hei'], 'heisann!', "Forventet value")
        Assert.assertEquals(project.properties['heiarop'], 'heisann! heisann! sann!', "Forventet value")
    }

    /**
     * Tester ekspandering av properties
     *
     * @see PropertyUtils#expandProperties(Map)
     */
    @Test
    void testExpandProperties() {
        //forks a new project in a temp folder
        Project project = FilterPropertiesProjectBuilder.builder().build().project;
        project.ext.hei = 'heisann!'
        project.ext.hopp = 'sann'

        PropertyUtils propertyUtils = new PropertyUtils(project);
        Map myProps = [:]

        //demonstrerer expanding der en substituerer inn verdier ifra prosjekt-properties
        myProps += [
                heiarop: '${hei} ${hei} ${hopp}!',
        ]
        propertyUtils.expandProperties(myProps)
        Assert.assertEquals(myProps['heiarop'], 'heisann! heisann! sann!', "Forventet value")


        //demonstrerer expanding der en overstyrer property verdi (key:'hei')
        myProps += [
                heiarop: '${hei} ${hei} ${hopp}!',
                hei: 'hallo${hopp}!',
        ]
        propertyUtils.expandProperties(myProps)
        Assert.assertEquals(myProps['heiarop'], 'hallosann! hallosann! sann!', "Forventet value")

        //demonstrerer expanding med rekursiv key
        myProps = [
                hei: '${hei}',
        ]
        propertyUtils.expandProperties(myProps)
        Assert.assertEquals(myProps['hei'], 'heisann!', "Forventet value")
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