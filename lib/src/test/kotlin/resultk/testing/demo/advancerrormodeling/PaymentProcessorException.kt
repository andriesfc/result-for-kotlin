package resultk.testing.demo.advancerrormodeling

import java.util.ResourceBundle.getBundle

class PaymentProcessorException internal constructor(val error: PaymentProcessorError) : RuntimeException(
    getBundle(PaymentProcessorError.PAYMENT_PROCESSOR_MESSAGES)
        .getString("paymentProcessorException.message")
        .format(error.code, error.message())
)