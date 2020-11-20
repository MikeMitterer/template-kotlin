package at.mikemitterer.template._basics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Weitere Infos:
 *      https://www.baeldung.com/kotlin-delegation-pattern
 *      https://yalantis.com/blog/how-to-work-with-delegation-in-kotlin/
 *
 * @since   20.11.20, 09:31
 */
class DelegationInheritanceTest {
    @Test
    fun testDelegateToPerson() {
        val delegateToPerson = DelegateToPerson(PersonImpl("Mike", 54))
        assertThat(delegateToPerson.name).isEqualTo("Mike")
    }

    @Test
    fun testDelegateToGerda() {
        val delegateToPerson = DelegateToConcretePerson()
        assertThat(delegateToPerson.name).isEqualTo("Gerda")
    }
}