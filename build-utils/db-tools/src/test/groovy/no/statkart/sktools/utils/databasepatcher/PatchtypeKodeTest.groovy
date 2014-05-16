package no.statkart.sktools.utils.databasepatcher

import org.testng.annotations.Test
import org.testng.Assert

/**
 * Test av {@link PatchtypeKode}
 *
 * @since 1.3
 * @author Leif Lislegård
 */
class PatchtypeKodeTest {


    @Test
    void testIsTypeOf() {
        final PatchtypeKode indexSybtypeA = PatchtypeKode.fromString("INDEX_TYPE_A")

        Assert.assertTrue(PatchtypeKode.INDEX.isTypeOf(PatchtypeKode.INDEX), 'Forventer subtype av INDEX')
        Assert.assertTrue(indexSybtypeA.isTypeOf(PatchtypeKode.INDEX), 'Forventer subtype av INDEX')

        final PatchtypeKode typeSybtypeA = PatchtypeKode.fromString("TYPE_INDEX_A")

        Assert.assertFalse(typeSybtypeA.isTypeOf(PatchtypeKode.INDEX), 'Forventer ikke subtype av INDEX')
        Assert.assertFalse(typeSybtypeA.isTypeOf(indexSybtypeA), 'Forventer ikke subtype')

        final PatchtypeKode type = PatchtypeKode.fromString("TYPE")
        Assert.assertTrue(typeSybtypeA.isTypeOf(type), 'Forventet subtype')
        Assert.assertFalse(indexSybtypeA.isTypeOf(type), 'Forventet ikke subtype')
    }

    @Test
    void testIsContainedBy() {
        final PatchtypeKode indexSybtypeA = PatchtypeKode.fromString("INDEX_TYPE_A")
        final PatchtypeKode typeSybtypeA = PatchtypeKode.fromString("TYPE_INDEX_A")

        final types = Collections.singleton(PatchtypeKode.INDEX)

        Assert.assertTrue(PatchtypeKode.INDEX.isContaintedBy(types), 'Forventer subtype av INDEX')
        Assert.assertFalse(PatchtypeKode.DATA.isContaintedBy(types), 'Ikke subtype av typer i listen')

        Assert.assertTrue(indexSybtypeA.isContaintedBy(types), 'Forventer subtype av INDEX')
        Assert.assertFalse(typeSybtypeA.isContaintedBy(types), 'Ikke subtype av typer i listen')


        //spesialtilfeller
        Assert.assertTrue(PatchtypeKode.ALL.isContaintedBy(types), 'Forventer subtype av INDEX')

    }

}
