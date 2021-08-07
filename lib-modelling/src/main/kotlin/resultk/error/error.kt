package resultk.error

interface DomainError {
    val errorCode: String
    fun errorMessage(): String
    fun debugErrorMessage(): String?
}





