package com.airbnb.utils.cloudinary

/**
 * Represents the state of a Cloudinary image upload operation.
 *
 * Used by [CloudinaryManager] and observed by ViewModels to drive UI feedback.
 *
 * Usage in ViewModel state:
 *   data class MyState(val uploadState: UploadState = UploadState.Idle)
 */
sealed class UploadState {

    /** No upload in progress. Default initial state. */
    object Idle : UploadState()

    /** Upload has started; indeterminate progress. */
    object Uploading : UploadState()

    /**
     * Upload in progress with known percentage.
     * @param percent 0–100 inclusive.
     */
    data class Progress(val percent: Int) : UploadState()

    /**
     * All images uploaded successfully.
     * @param urls Ordered list of Cloudinary secure_url values.
     */
    data class Success(val urls: List<String>) : UploadState()

    /**
     * Upload failed (fully or partially).
     * @param message Human-readable error description.
     */
    data class Error(val message: String) : UploadState()
}
