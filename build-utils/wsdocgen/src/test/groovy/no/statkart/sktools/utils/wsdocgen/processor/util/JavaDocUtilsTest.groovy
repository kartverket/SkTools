package no.statkart.sktools.utils.wsdocgen.processor.util

import org.testng.Assert
import org.testng.annotations.Test

/**
 * Tester {@link JavaDocUtils}
 *
 * @since 1.3 - ny grunnbok sprint 30
 * @author Leif Lislegård
 */
class JavaDocUtilsTest {

    @Test
    public void parseText() {
        final JavaDocUtils parser = JavaDocUtils.parse('Dokumentasjon' + '\n' +
                'linje 2' + '\n' +
                '@secret tag' + '\n' +
                'linje 4' + '\n' +
                '')

        Assert.assertEquals(parser.text, 'Dokumentasjon\nlinje 2\nlinje 4', "forventet tekst");
    }

    /**
     * @since 1.3 - SKTOOLS-108
     */
    @Test
    public void parseInlineDocletTag() {
        final JavaDocUtils parser = JavaDocUtils.parse('Dokumentasjon' + '\n' +
                'linje 2' + '\n' +
                '{@secret tag}' + '\n' +
                'linje 4' + '\n' +
                '')

        Assert.assertEquals(parser.text, 'Dokumentasjon\nlinje 2\n<span class="javadoc_tag_secret">tag</span>\nlinje 4', "forventet html-tekst");
    }

    /**
     * @since 1.3 - SKTOOLS-108
     */
    @Test
    public void parseInlineDocletTagMultiples() {
        final JavaDocUtils parser = JavaDocUtils.parse('Dokumentasjon' + '\n' +
                'linje 2' + '\n' +
                '{@secret tag}' + '\n' +
                '{@secret <tag>}' + '\n' +
                'linje 5' + '\n' +
                '')

        Assert.assertEquals(parser.text, 'Dokumentasjon\nlinje 2\n<span class="javadoc_tag_secret">tag</span>\n<span class="javadoc_tag_secret"><tag></span>\nlinje 5', "forventet html-tekst");
    }



    @Test
    public void parseReturnUndocumented() {
        final JavaDocUtils parser = JavaDocUtils.parse('''Dokumentasjon
            linje 2
            @return
            ''')
        Assert.assertEquals(parser.return, parser.allTags['return'][''], "dokumentasjon av returverdi");
        Assert.assertEquals(parser.return, null, "dokumentasjon av returverdi");
    }



    @Test
    public void parseReturnDocumented() {
        final JavaDocUtils parser = JavaDocUtils.parse('''@unknown tag
            @return un dos tres
            @unknown tag
            ''')
        Assert.assertEquals(parser.return, parser.allTags['return'][''], "dokumentasjon av returverdi");
        Assert.assertEquals(parser.return, 'un dos tres', "dokumentasjon av returverdi");
    }



    @Test
    public void parseOtherTags() {
        final JavaDocUtils parser = JavaDocUtils.parse('''@unknown unknown-tag
            beskrivelse
            @return un dos tres
            @unknown other unknown tag
            @unknown
            @test
            ''')

        Assert.assertEquals(parser.allTags['return'][''], 'un dos tres', "dokumentasjon av returverdi");
        Assert.assertEquals(parser.allTags['unknown'][''], null, "dokumentasjon av @unknown");
        Assert.assertEquals(parser.allTags['test'][''], null, "dokumentasjon av @test");
    }



    @Test
    public void parseParamTags() {
        final JavaDocUtils parser = JavaDocUtils.parse('''
            beskrivelse
            @param uno first param in list
            @param dos second param in list
            @param tres last param...
            @test
            ''')

        Assert.assertEquals(parser.allTags['param'], parser.params, "dokumentasjon av parameters");
        Assert.assertEquals(parser.params.keySet() as List, ['uno', 'dos', 'tres'] as List, "parameters");
        Assert.assertEquals(parser.params['uno'], 'first param in list', "dokumentasjon av parameter");
        Assert.assertEquals(parser.params['dos'], 'second param in list', "dokumentasjon av parameter");
        Assert.assertEquals(parser.params['tres'], 'last param...', "dokumentasjon av parameter");
    }

    @Test
    public void parseThrowsTags() {
        final JavaDocUtils parser = JavaDocUtils.parse('''
            beskrivelse
            @throws java.lang.Throwable first ex in list
            @throws Exception second ex in list
            @throws MyImplementationException last ex...
            ''')

        Assert.assertEquals(parser.allTags['throws'], parser.throws, "dokumentasjon av exceptions");
        Assert.assertEquals(parser.throws.keySet() as List, ['java.lang.Throwable', 'Exception', 'MyImplementationException'] as List, "exceptions");

        Assert.assertEquals(parser.throws['java.lang.Throwable'], 'first ex in list', "dokumentasjon av exception");
        Assert.assertEquals(parser.throws['Exception'], 'second ex in list', "dokumentasjon av exception");
        Assert.assertEquals(parser.throws['MyImplementationException'], 'last ex...', "dokumentasjon av exception");
    }

}
