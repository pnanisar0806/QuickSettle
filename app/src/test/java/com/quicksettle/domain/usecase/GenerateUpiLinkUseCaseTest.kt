package com.quicksettle.domain.usecase

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.URI

class GenerateUpiLinkUseCaseTest {

    private lateinit var useCase: GenerateUpiLinkUseCase

    @BeforeEach
    fun setUp() {
        useCase = GenerateUpiLinkUseCase()
    }

    // ──────────────────────────────────────────────────────────────
    // generateUri — structure
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `generateUri starts with upi scheme`() {
        val uri = useCase.generateUri(
            vpa = "merchant@upi",
            name = "Rahul",
            amount = 250.0,
            description = "Dinner",
        )
        assertThat(uri).startsWith("upi://pay?")
    }

    @Test
    fun `generateUri contains all required query params`() {
        val uri = useCase.generateUri(
            vpa = "merchant@upi",
            name = "Rahul",
            amount = 250.0,
            description = "Dinner",
        )
        assertThat(uri).contains("pa=merchant@upi")
        assertThat(uri).contains("pn=")
        assertThat(uri).contains("am=")
        assertThat(uri).contains("cu=INR")
        assertThat(uri).contains("tn=")
    }

    @Test
    fun `generateUri amount has exactly 2 decimal places for whole number`() {
        val uri = useCase.generateUri(
            vpa = "test@upi",
            name = "Test",
            amount = 100.0,
            description = "Test",
        )
        assertThat(uri).contains("am=100.00")
    }

    @Test
    fun `generateUri amount has exactly 2 decimal places for fractional value`() {
        val uri = useCase.generateUri(
            vpa = "test@upi",
            name = "Test",
            amount = 49.5,
            description = "Test",
        )
        assertThat(uri).contains("am=49.50")
    }

    @Test
    fun `generateUri amount never omits trailing zero`() {
        val uri = useCase.generateUri(
            vpa = "test@upi",
            name = "Test",
            amount = 1000.0,
            description = "Test",
        )
        // Must NOT be "am=1000" or "am=1000.0"
        assertThat(uri).contains("am=1000.00")
        assertThat(uri).doesNotContain("am=1000&")
        assertThat(uri).doesNotContain("am=1000.0&")
    }

    @Test
    fun `generateUri name with spaces is URL-encoded`() {
        val uri = useCase.generateUri(
            vpa = "test@upi",
            name = "Priya Sharma",
            amount = 100.0,
            description = "Lunch",
        )
        // URLEncoder encodes spaces as '+' (application/x-www-form-urlencoded)
        assertThat(uri).contains("pn=Priya+Sharma")
    }

    @Test
    fun `generateUri description with special characters is URL-encoded`() {
        val uri = useCase.generateUri(
            vpa = "test@upi",
            name = "Alice",
            amount = 75.0,
            description = "Dinner & drinks",
        )
        // '&' must be encoded so it doesn't break the query string
        assertThat(uri).doesNotContain("tn=Dinner & drinks")
        assertThat(uri).contains("tn=Dinner+%26+drinks")
    }

    @Test
    fun `generateUri description with hash and equals is URL-encoded`() {
        val uri = useCase.generateUri(
            vpa = "test@upi",
            name = "Bob",
            amount = 50.0,
            description = "Bill #1 = split",
        )
        assertThat(uri).doesNotContain("#")
        assertThat(uri).doesNotContain("tn=Bill #1")
    }

    @Test
    fun `generateUri produces parseable URI`() {
        val uriString = useCase.generateUri(
            vpa = "merchant@upi",
            name = "Rahul Kumar",
            amount = 250.75,
            description = "Team Lunch",
        )
        // Should not throw
        val uri = URI(uriString)
        assertThat(uri.scheme).isEqualTo("upi")
    }

    @Test
    fun `generateUri vpa is not encoded`() {
        val vpa = "merchant@okaxis"
        val uri = useCase.generateUri(
            vpa = vpa,
            name = "Test",
            amount = 100.0,
            description = "Test",
        )
        assertThat(uri).contains("pa=$vpa")
    }

    @Test
    fun `generateUri currency is always INR`() {
        val uri = useCase.generateUri(
            vpa = "test@upi",
            name = "Test",
            amount = 100.0,
            description = "Test",
        )
        assertThat(uri).contains("cu=INR")
    }

    // ──────────────────────────────────────────────────────────────
    // generateShareMessage — format
    // ──────────────────────────────────────────────────────────────

    @Test
    fun `generateShareMessage has correct format`() {
        val link = "upi://pay?pa=merchant@upi&pn=Rahul&am=250.00&cu=INR&tn=Dinner"
        val message = useCase.generateShareMessage(
            name = "Rahul",
            amount = 250.0,
            description = "Dinner",
            upiLink = link,
        )
        assertThat(message).isEqualTo(
            "Hey Rahul, your share for Dinner is ₹250.00. Pay here: $link"
        )
    }

    @Test
    fun `generateShareMessage starts with Hey name`() {
        val message = useCase.generateShareMessage(
            name = "Anjali",
            amount = 100.0,
            description = "Coffee",
            upiLink = "upi://pay?pa=cafe@upi&pn=Anjali&am=100.00&cu=INR&tn=Coffee",
        )
        assertThat(message).startsWith("Hey Anjali,")
    }

    @Test
    fun `generateShareMessage contains rupee symbol`() {
        val message = useCase.generateShareMessage(
            name = "Arjun",
            amount = 500.0,
            description = "Movie",
            upiLink = "upi://pay?pa=test@upi&pn=Arjun&am=500.00&cu=INR&tn=Movie",
        )
        assertThat(message).contains("₹500.00")
    }

    @Test
    fun `generateShareMessage amount shows 2 decimal places for whole number`() {
        val message = useCase.generateShareMessage(
            name = "Dev",
            amount = 200.0,
            description = "Taxi",
            upiLink = "upi://pay?pa=test@upi",
        )
        assertThat(message).contains("₹200.00")
        assertThat(message).doesNotContain("₹200 ")
        assertThat(message).doesNotContain("₹200.0 ")
    }

    @Test
    fun `generateShareMessage includes the upi link`() {
        val upiLink = "upi://pay?pa=vendor@upi&pn=Pooja&am=75.50&cu=INR&tn=Snacks"
        val message = useCase.generateShareMessage(
            name = "Pooja",
            amount = 75.50,
            description = "Snacks",
            upiLink = upiLink,
        )
        assertThat(message).contains("Pay here: $upiLink")
    }

    @Test
    fun `generateShareMessage contains description`() {
        val message = useCase.generateShareMessage(
            name = "Vikram",
            amount = 300.0,
            description = "Birthday Party",
            upiLink = "upi://pay?pa=test@upi",
        )
        assertThat(message).contains("Birthday Party")
    }
}
