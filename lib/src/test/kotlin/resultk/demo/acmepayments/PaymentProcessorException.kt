package resultk.demo.acmepayments

class PaymentProcessorException internal constructor(
    val error: PaymentProcessorError,
    message: String = error.message(),
) : RuntimeException(message)