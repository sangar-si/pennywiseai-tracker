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
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull()
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
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull()
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
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull()
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
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull()
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
                val amount = match.groupValues[1].replace(",", "").toBigDecimalOrNull()
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
            "you received a" // Credit card payment,
            "was added as an INTERAC e-Transfer contact" // Adding new contact
        )

        val lowerMessage = message.lowercase()

        if (lowerMessage.contains("you received a") || lowerMessage.contains("was added as an INTERAC e-Transfer contact")) {
            return 0
        }

        var i = 1
        for (pattern in validPatterns) {
            if (lowerMessage.contains(pattern.lowercase())) {
                return i
            }
            i++
        }
        return 0
    }

    override fun canHandle(sender: String): Boolean {
        val normalizedSender = sender.uppercase()
        return senderIds.any { normalizedSender == it || normalizedSender.contains(it) }
    }

    override fun isTransactionMessage(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // Skip Credit Card payment message to avoid double record of expenditure
        if (lowerMessage.contains("you received a") || lowerMessage.contains("was added as an INTERAC e-Transfer contact")) {
            return false
        }

        // CIBC specific transaction keywords
        val cibcKeywords = listOf(
            "A purchase of", // Credit card purchase
            "you spent", // Debit card purchase
            "An INTERAC e-Transfer", // Interac transfer
            "has been credited by", // Refund to credit card
            "An ATM withdrawal of" // ATM withdrawal
        )

        return cibcKeywords.any { lowerMessage.contains(it) }
    }

    override fun extractTransactionType(message: String): TransactionType? {
        val lowerMessage = message.lowercase()

        return when {
            // Refund is income
            lowerMessage.contains("has been credited by") -> TransactionType.INCOME

            // Charges are expenses
            lowerMessage.contains("A purchase of") -> TransactionType.EXPENSE
            lowerMessage.contains("you spent") -> TransactionType.EXPENSE
            lowerMessage.contains("An INTERAC e-Transfer") -> TransactionType.EXPENSE
            lowerMessage.contains("An ATM withdrawal of") -> TransactionType.EXPENSE

            else -> super.extractTransactionType(message)
        }
    }

    override fun extractAmount(message: String): BigDecimal? {
        // Pattern 1: Credit card charge - "for EGP 118.00" or "for EUR 1,234.56"

        val chargePatter = Regex("""\$(\d+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        chargePattern.find(message)?.let { match ->
            val amountStr = match.groupValues[2].replace(",", "")
            return try {
                BigDecimal(amountStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractAmount(message)
    }

    override fun extractCurrency(message: String): String? {
        // Pattern: "for EGP 118.00" or "with EUR .93"
        val currencyPattern = Regex(
            """(?:for|with)\s+([A-Z]{3})\s+[0-9,]*\.?\d+""",
            RegexOption.IGNORE_CASE
        )
        currencyPattern.find(message)?.let { match ->
            return match.groupValues[1].uppercase()
        }

        return super.extractCurrency(message)
    }

    override fun extractAccountLast4(message: String): String? {
        super.extractAccountLast4(message)?.let { return it }
        // Pattern 1: "credit card ending with#8016" or "credit card#8016"
        val cardEndingPattern = Regex(
            """(?:credit\s+card|card)\s*(?:ending\s+with)?#(\d{4})""",
            RegexOption.IGNORE_CASE
        )
        cardEndingPattern.find(message)?.let { match ->
            return match.groupValues[1]
        }

        return null
    }

    override fun extractMerchant(message: String, sender: String): String? {
        val lowerMessage = message.lowercase()

        // Pattern 1: Charge transaction - "at SAOOD MARKET on"
        if (lowerMessage.contains("was charged") || lowerMessage.contains("was debited") ||
            lowerMessage.contains("was spent")
        ) {
            val atMerchantPattern = Regex(
                """at\s+([A-Z0-9\s\/&\-]+?)\s+on\s+\d""",
                RegexOption.IGNORE_CASE
            )
            atMerchantPattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }

        // Pattern 2: Refund transaction - "from ORACLE IRELAND with"
        if (lowerMessage.contains("refunded")) {
            val fromMerchantPattern = Regex(
                """from\s+([A-Z0-9\s\/&\-]+?)\s+with\s+[A-Z]{3}""",
                RegexOption.IGNORE_CASE
            )
            fromMerchantPattern.find(message)?.let { match ->
                val merchant = cleanMerchantName(match.groupValues[1].trim())
                if (isValidMerchantName(merchant)) {
                    return merchant
                }
            }
        }

        return super.extractMerchant(message, sender)
    }

    override fun extractAvailableLimit(message: String): BigDecimal? {
        // Pattern: "Card available limit is EGP  10000.21"
        val limitPattern = Regex(
            """(?:Card\s+)?available\s+limit\s+is\s+[A-Z]{3}\s+([0-9,]+(?:\.\d{2})?)""",
            RegexOption.IGNORE_CASE
        )
        limitPattern.find(message)?.let { match ->
            val limitStr = match.groupValues[1].replace(",", "")
            return try {
                BigDecimal(limitStr)
            } catch (e: NumberFormatException) {
                null
            }
        }

        return super.extractAvailableLimit(message)
    }

    override fun detectIsCard(message: String): Boolean {
        val lowerMessage = message.lowercase()

        // CIB messages explicitly mention credit card
        return lowerMessage.contains("credit card") ||
                lowerMessage.contains("debit card") ||
                lowerMessage.contains("card ending") ||
                lowerMessage.contains("card#")
    }

}