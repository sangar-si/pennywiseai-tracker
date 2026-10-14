package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.ParsedTransaction
import com.pennywiseai.parser.core.TransactionType

import java.math.BigDecimal

class CIBCBankParser : BankParser()  {
    override fun getBankName() = "CIBC"

    override fun getCurrency() = "CAD"
    override val countryCode: String = "CA"
    override val senderIds: List<String> = listOf("242226", "24222", "CIBC", "CIBCALERT")


    // 1. Debit Card Purchase Pattern
    // Example: "Sangar, A purchase of $100.00 was made using card ending in 1234. Sign on to CIBC Banking for details"
    private val debitPurchaseRegex = Regex(
        """.*?\bA purchase of \$?([\d,]+\.\d{2}) was made using card ending in (\d{4})\. Sign on to CIBC""",
        RegexOption.IGNORE_CASE
    )


    // 2. Credit Card Purchase Pattern
    // Example: "Sangar, you spent $17.99 at Tim Hortons with you card ending in 4199"
    private val ccPurchaseRegex = Regex(
        """.*?\byou spent \$?([\d,]+\.\d{2}) at (.*?) with (?:you|your) card ending in (\d{4})""",
        RegexOption.IGNORE_CASE
    )

    // 3. Interac e-Transfer Pattern
    // Example: "Sangar, An INTERAC e-Transfer $500.00 was sent to Remitly. Sign on to CIBC Banking for details"
    private val eTransferRegex = Regex(
        """.*?\bAn INTERAC e-Transfer \$?([\d,]+\.\d{2}) was sent to (.*?)\. Sign on to CIBC""",
        RegexOption.IGNORE_CASE
    )

    // 4. Refund Pattern
    // Example: "Sangar, $50.00 has been credited by Amazon.com to your card ending in 4199."
    private val refundRegex = Regex(
        """.*?\$?([\d,]+\.\d{2}) has been credited by (.*?) to your card ending in (\d{4})""",
        RegexOption.IGNORE_CASE
    )

    // 5. ATM Withdrawal Pattern
    // Example: "Sangar, An ATM withdrawal of $250.00 was debited from your account ending in 2291. Sign on to CIBC Banking for details."
    private val atmWithdrawalRegex = Regex(
        """.*?\bAn ATM withdrawal of \$?([\d,]+\.\d{2}) was debited from your account ending in (\d{4})\. Sign on to CIBC""",
        RegexOption.IGNORE_CASE
    )


    // // 6. Credit Card Payment Pattern NOTE: This will be skipped in the parser to avoid double record of expenditure
    // // Example: "Sangar, you received a $100 payment to card ending in 4199"
    // private val ccPaymentRegex = Regex(
    //     """.*?\byou received a \$?([\d,]+(?:\.\d{2})?) payment to card ending in (\d{4})""",
    //     RegexOption.IGNORE_CASE
    // )

    override fun parse(smsBody: String, sender: String, timestamp: Long): ParsedTransaction? {
        // Skip non-transaction messages

        if (!canHandle(sender)) {
            return null
        }

        val transactionCategory = getTransactionCategory(smsBody)

        if (transactionCategory == 0) {
            return null
        }
        else if (transactionCategory == 1) {
            // Debit card purchase
            // Example: "Sangar, A purchase of $100.00 was made using card ending in 1234. Sign on to CIBC Banking for details"

            debitPurchaseRegex.find(smsBody)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
                val accountLast4 = match.groupValues[2]

                if (amount != null) {
                    return ParsedTransaction(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        merchant = null,
                        reference = null,
                        accountLast4 = accountLast4,
                        balance = null,
                        creditLimit = null,
                        smsBody = smsBody,
                        sender = sender,
                        timestamp = timestamp,
                        bankName = getBankName(),
                        isFromCard = true,
                        currency = getCurrency()
                    )
                }
            }
        }
        else if (transactionCategory == 2) {
            // Credit card purchase
            // Example: "Sangar, you spent $17.99 at Tim Hortons with you card ending in 4199"
            ccPurchaseRegex.find(smsBody)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
                val merchant = match.groupValues[2].trim()
                val accountLast4 = match.groupValues[3]

                if (amount != null) {
                    return ParsedTransaction(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        merchant = merchant,
                        reference = null,
                        accountLast4 = accountLast4,
                        balance = null,
                        creditLimit = null,
                        smsBody = smsBody,
                        sender = sender,
                        timestamp = timestamp,
                        bankName = getBankName(),
                        isFromCard = true,
                        currency = getCurrency()
                    )
                }
            }
        }
        else if (transactionCategory == 3) {
            // Interac e-Transfer
            // Example: "Sangar, An INTERAC e-Transfer $500.00 was sent to Remitly. Sign on to CIBC Banking for details"
            eTransferRegex.find(smsBody)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
                val merchant = match.groupValues[2].trim()

                if (amount != null) {
                    return ParsedTransaction(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        merchant = merchant,
                        reference = null,
                        accountLast4 = null,
                        balance = null,
                        creditLimit = null,
                        smsBody = smsBody,
                        sender = sender,
                        timestamp = timestamp,
                        bankName = getBankName(),
                        isFromCard = false,
                        currency = getCurrency()
                    )
                }
            }
        }
        else if (transactionCategory == 4) {
            // Credit card refund
            // Example: "Sangar, $50.00 has been credited by Amazon.com to your card ending in 4199."
            refundRegex.find(smsBody)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
                val merchant = match.groupValues[2].trim()
                val accountLast4 = match.groupValues[3]

                if (amount != null) {
                    return ParsedTransaction(
                        amount = amount,
                        type = TransactionType.INCOME,
                        merchant = merchant,
                        reference = null,
                        accountLast4 = accountLast4,
                        balance = null,
                        creditLimit = null,
                        smsBody = smsBody,
                        sender = sender,
                        timestamp = timestamp,
                        bankName = getBankName(),
                        isFromCard = true,
                        currency = getCurrency()
                    )
                }
            }
        }
        else if (transactionCategory == 5) {
            // ATM withdrawal
            // Example: "Sangar, An ATM withdrawal of $250.00 was debited from your account ending in 2291. Sign on to CIBC Banking for details."
            atmWithdrawalRegex.find(smsBody)?.let { match ->
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull() ?: return null
                val accountLast4 = match.groupValues[2]

                if (amount != null) {
                    return ParsedTransaction(
                        amount = amount,
                        type = TransactionType.EXPENSE,
                        merchant = "ATM Withdrawal",
                        reference = null,
                        accountLast4 = accountLast4,
                        balance = null,
                        creditLimit = null,
                        smsBody = smsBody,
                        sender = sender,
                        timestamp = timestamp,
                        bankName = getBankName(),
                        isFromCard = true,
                        currency = getCurrency()
                    )
                }
            }
        }
        return null
    }

    open fun getTransactionCategory(message: String): Int {
        val validPatterns: List<String> = listOf(
            "A purchase of", // Debit card purchase
            "you spent", // Credit card purchase
            "An INTERAC e-Transfer", // Interac transfer expenditure
            "has been credited by", // Refund to credit card
            "An ATM withdrawal of" // ATM withdrawal expenditure
        )

        val ignorePatterns: List<String> = listOf(
            "you received a", // Credit card payment
            "was added as an INTERAC e-Transfer contact" // Adding new contact
        )

        val lowerMessage = message.lowercase()

        if (ignorePatterns.any { lowerMessage.contains(it.lowercase()) }) {
            return 0
        }

        validPatterns.forEachIndexed { index, pattern ->
            if (lowerMessage.contains(pattern.lowercase())) {
                return index + 1
            }
        }
        return 0
    }

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return senderIds.any { normalizedSender == it || normalizedSender.contains(it) }
    }
}