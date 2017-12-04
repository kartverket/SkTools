/**
 * Dummy-klasse for å kunne avgjøre om en kjører tester ifra IntelliJ eller ikke.
 * <p/>
 * For at mekanikken skal fungere må disse linjene inn i gradle:
 * <p/>
 * <pre>
 * // Testfunksjonalitet som avgjør om en kjører tester ifra IntelliJ eller ikke.
 * // NB: Denne kildekoden skal IKKE med i gradle bygget!!
 * idea.module {
 *   sourceDirs += file('src/idea/source')
 * }
 * </pre>
 *
 * @since 1.2
 */
public class IntelliJDummy {
}
