package com.pennywiseai.parser.core.bank

import com.pennywiseai.parser.core.TransactionType
import com.pennywiseai.parser.core.test.ExpectedTransaction
import com.pennywiseai.parser.core.test.ParserTestCase
import com.pennywiseai.parser.core.test.ParserTestUtils
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.math.BigDecimal

class CIBCBankParserTest {

    private val parser = CIBCBankParser()

    @TestFactory
    fun `cibc parser handles key paths`(): List<DynamicTest> {
        val cases = listOf(
            ParserTestCase(
                name = "Credit card purchase typed as EXPENSE",
                message = "Sangar, you spent $17.99 at Tim Hortons with you card ending in 4199",
                sender = "242226",
                expected = ExpectedTransaction(
                    amount = BigDecimal("17.99"),
                    currency = "CAD",
                    type = TransactionType.EXPENSE,
                    merchant = "Tim Hortons",
                    accountLast4 = "4199"
                )
            ),
            ParserTestCase(
                name = "Credit card payment typed as INCOME or CREDIT. This is to be ignored to avoid double record of expenditure",
                message = "Sangar, you received a $100 payment to card ending in 4199",
                sender = "242226",
                shouldParse = false
            ),
            ParserTestCase(
                name = "INTERAC e-Transfer typed as EXPENSE",
                message = "Sangar, An INTERAC e-Transfer $500.00 was sent to Remitly. Sign on to CIBC Banking for details",
                sender = "242226",
                expected = ExpectedTransaction(
                    amount = BigDecimal("500.00"),
                    currency = "CAD",
                    type = TransactionType.EXPENSE,
                    merchant = "Remitly"
                )
            ),
            ParserTestCase(
                name = "Debit card purchase typed as EXPENSE",
                message = "Sangar, A purchase of $100.00 was made using card ending in 1234. Sign on to CIBC Banking for details",
                sender = "242226",
                expected = ExpectedTransaction(
                    amount = BigDecimal("100.00"),
                    currency = "CAD",
                    type = TransactionType.EXPENSE,
                    merchant = "Debit Purchase",
                    accountLast4 = "1234"
                )
            ),
            ParserTestCase(
                name = "ATM withdrawal typed as EXPENSE",
                message = "Sangar, An ATM withdrawal of $250.00 was debited from your account ending in 2291. Sign on to CIBC Banking for details.",
                sender = "242226",
                expected = ExpectedTransaction(
                    amount = BigDecimal("250.00"),
                    currency = "CAD",
                    type = TransactionType.EXPENSE,
                    merchant = "ATM Withdrawal",
                    accountLast4 = "2291"
                )
            ),
            ParserTestCase(
                name = "OTP security alert message is ignored",
                message = "Your CIBC verification code is 654321. Do not share this code with anyone.",
                sender = "242226",
                shouldParse = false
            )
        )

        return ParserTestUtils.runTestSuite(
            parser = parser,
            testCases = cases,
            handleCases = listOf(
                "242226" to true,
                "24222" to true,
                "CIBC" to true,
                "CIBCALERT" to true
            )
        )
    }
}