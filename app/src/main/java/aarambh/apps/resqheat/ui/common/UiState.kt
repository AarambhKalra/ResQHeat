package aarambh.apps.resqheat.ui.common

/**
 * Represents the state of UI operations
 */
sealed class UiState<out T> {
    /**
     * Initial state or loading state
     */
    object Loading : UiState<Nothing>()
    
    /**
     * Success state with data
     */
    data class Success<T>(val data: T) : UiState<T>()
    
    /**
     * Error state with message and optional throwable
     */
    data class Error(val message: String, val throwable: Throwable? = null) : UiState<Nothing>()
    
    /**
     * Empty state (no data available)
     */
    object Empty : UiState<Nothing>()
    
    /**
     * Check if state is loading
     */
    val isLoading: Boolean
        get() = this is Loading
    
    /**
     * Check if state is error
     */
    val isError: Boolean
        get() = this is Error
    
    /**
     * Check if state is success
     */
    val isSuccess: Boolean
        get() = this is Success
    
    /**
     * Get data if state is success, null otherwise
     */
    fun getDataOrNull(): T? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }
    
    /**
     * Get error message if state is error, null otherwise
     */
    fun getErrorMessageOrNull(): String? {
        return when (this) {
            is Error -> message
            else -> null
        }
    }
}

