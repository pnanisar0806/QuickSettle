package com.quicksettle.domain.usecase

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

/**
 * Generates NPCI-compliant UPI deep-link URIs and human-readable share messages.
 *
 * URI format: upi://pay?pa={vpa}&pn={urlEncoded(name)}&am={amount 2dp}&cu=INR&tn={urlEncoded(desc)}
 */
class GenerateUpiLinkUseCase @Inject constructor() {

    /**
     * Returns a UPI deep-link URI ready to be handed to GPay, PhonePe, or Paytm.
     *
     * [vpa] — recipient's Virtual Payment Address (e.g. "merchant@upi")
     * [name] — display name of the payee; will be URL-encoded
     * [amount] — amount in INR; formatted to exactly 2 decimal places
     * [description] — transaction note; will be URL-encoded
     */
    fun generateUri(vpa: String, name: String, amount: Double, description: String): String {
        val encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name())
        val encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8.name())
        val formattedAmount = "%.2f".format(amount)
        return "upi://pay?pa=$vpa&pn=$encodedName&am=$formattedAmount&cu=INR&tn=$encodedDesc"
    }

    /**
     * Returns a WhatsApp / SMS-ready share message.
     *
     * Example output:
     *   "Hey Rahul, your share for Dinner is ₹250.00. Pay here: upi://pay?pa=..."
     */
    fun generateShareMessage(
        name: String,
        amount: Double,
        description: String,
        upiLink: String,
    ): String {
        val formattedAmount = "%.2f".format(amount)
        return "Hey $name, your share for $description is ₹$formattedAmount. Pay here: $upiLink"
    }
}
