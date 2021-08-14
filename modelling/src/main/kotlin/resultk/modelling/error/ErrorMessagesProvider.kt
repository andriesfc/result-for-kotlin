package resultk.modelling.error

interface ErrorMessagesProvider<in E : Error> {
    fun getErrorMessage(error: E): String
    fun getDebugErrorMessage(error: E): String?
}

