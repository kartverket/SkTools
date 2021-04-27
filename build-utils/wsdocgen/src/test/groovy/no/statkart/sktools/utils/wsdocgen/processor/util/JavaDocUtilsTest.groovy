package no.statkart.sktools.utils.wsdocgen.processor.util


import org.testng.Assert
import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.entry

/**
 * Tester {@link JavaDocUtils}
 *
 * @since 1.3 - ny grunnbok sprint 30
 * @author Leif Lislegård
 */
class JavaDocUtilsTest {

    @Test
    void parseText() {
        final JavaDocUtils parser = JavaDocUtils.parse('Dokumentasjon' + '\n' +
                'linje 2' + '\n' +
                '@secret tag' + '\n' +
                'linje 4' + '\n' +
                '')

        assertThat(parser.text).isEqualTo("Dokumentasjon\nlinje 2");
    }

    /**
     * @since 1.3 - SKTOOLS-108
     */
    @Test
    void parseInlineDocletTag() {
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
    void parseInlineDocletTagMultiples() {
        final JavaDocUtils parser = JavaDocUtils.parse('Dokumentasjon' + '\n' +
                'linje 2' + '\n' +
                '{@secret tag}' + '\n' +
                '{@secret <tag>}' + '\n' +
                'linje 5' + '\n' +
                '')

        Assert.assertEquals(parser.text, 'Dokumentasjon\nlinje 2\n<span class="javadoc_tag_secret">tag</span>\n<span class="javadoc_tag_secret"><tag></span>\nlinje 5', "forventet html-tekst");
    }



    @Test
    void parseReturnUndocumented() {
        final JavaDocUtils parser = JavaDocUtils.parse('''Dokumentasjon
            linje 2
            @return
            ''')
        Assert.assertEquals(parser.return, parser.allTags['return'][''], "dokumentasjon av returverdi");
        Assert.assertEquals(parser.return, null, "dokumentasjon av returverdi");
    }



    @Test
    void parseReturnDocumented() {
        final JavaDocUtils parser = JavaDocUtils.parse('''@unknown tag
            @return un dos tres
            @unknown tag
            ''')
        Assert.assertEquals(parser.return, parser.allTags['return'][''], "dokumentasjon av returverdi");
        Assert.assertEquals(parser.return, 'un dos tres', "dokumentasjon av returverdi");
    }



    @Test
    void parseOtherTags() {
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
    void parseParamTags() {
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
    void parseThrowsTags() {
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

    /**
     * SKTOOLS-224
     *
     * Fra javadoc guiden:
     *  > The first line that begins with an "@" character ends the description. There is only one description block per doc comment; you cannot continue the description following block tags.
     */
    @Test
    void multilinesInTagBlock() {
        final JavaDocUtils parser = JavaDocUtils.parse('''
            Beskrivelse
            end of beskrivelse!
            @throws ex1
                    end of tag1!
            @throws ex2 start of tag2
                    end of tag2!
            @param tag3 start of tag3
                    end of tag3!
            @return start of tag4
                    end of tag4!
            ''')

        assertThat(parser.getThrows()).containsOnly(
            entry("ex1", "end of tag1!"),
            entry("ex2", "start of tag2\n                    end of tag2!"));

        assertThat(parser.getParams()).containsOnly(
            entry("tag3", "start of tag3\n                    end of tag3!"));

        assertThat(parser.getReturn()).isEqualTo("start of tag4\n                    end of tag4!")
    }

    @Test
    void emptyDescriptionParsesTags() {
        ['@return Return tag description!',
         ' @return Return tag description!',
         '\n@return Return tag description!',
        ].each {
            JavaDocUtils javaDocUtils = JavaDocUtils.parse(it)
            assertThat(javaDocUtils.getText() as String).describedAs("Forventer ingen description for '%s'", it).isNullOrEmpty()
            assertThat(javaDocUtils.getReturn()).isEqualTo("Return tag description!")
        }
    }

    @Test
    void inlineTagletOnTags() {
        final JavaDocUtils parser = JavaDocUtils.parse('''
        Description
        @return Return {@code true} description!''')

        assertThat(parser.getText() as String).isEqualTo("Description")
        assertThat(parser.getReturn()).isEqualTo("Return <span class=\"javadoc_tag_code\">true</span> description!")
    }

}
