import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AnagramServiceTest : FunSpec({

    val service = AnagramService()

    test("generate anagram - reverse word") {
        val word = "jelena"

        service.generateAnagram(word) shouldBe "anelej"
    }

    test("generate anagram - empty string") {
        val word = ""

        service.generateAnagram(word) shouldBe ""
    }

    test("generate anagram - single letter") {
        val word = "a"

        service.generateAnagram(word) shouldBe "a"
    }

    test("generate anagram - preserve letter case") {
        val word = "Kotlin"

        service.generateAnagram(word) shouldBe "niltoK"
    }

    test("generate anagram - palindrome") {
        val word = "radar"

        service.generateAnagram(word) shouldBe "radar"
    }
})