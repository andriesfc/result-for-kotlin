package resultk.modelling.testing.evovle.error

import resultk.error.Error

sealed class BatchProcessingError : Error<String, BatchProcessingError> {

    override fun error(): BatchProcessingError = this
}
