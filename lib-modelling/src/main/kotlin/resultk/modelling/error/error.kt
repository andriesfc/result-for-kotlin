package resultk.modelling.error

interface ErrorMessage {
    fun message(): String
    fun developerMessage(): String?
}

interface Error : ErrorMessage {
    val code: String
}