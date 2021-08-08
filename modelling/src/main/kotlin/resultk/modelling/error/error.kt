package resultk.modelling.error

interface ErrorMessage {
    val errorMessage: String
    val debugErrorMessage: String?
}

interface Error {
    val errorCode: String
}

interface DomainError : Error, ErrorMessage


