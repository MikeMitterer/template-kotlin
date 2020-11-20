package at.mikemitterer.template._basics

interface Person {
    val name: String
    val age: Int
}

data class PersonImpl(
    override val name: String,
    override val age: Int) : Person

class DelegateToPerson(person: Person) : Person by person

class DelegateToConcretePerson : Person by PersonImpl("Gerda", 55)