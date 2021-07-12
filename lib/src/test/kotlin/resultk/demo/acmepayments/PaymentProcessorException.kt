package resultk.demo.acmepayments

import java.util.ResourceBundle.getBundle

class PaymentProcessorException internal constructor(val error: PaymentProcessorError) : RuntimeException(
    getBundle(PaymentProcessorError.PAYMENT_PROCESSOR_MESSAGES).getString("paymentProcessorException.message")
        .format(error.code, error.message())
)