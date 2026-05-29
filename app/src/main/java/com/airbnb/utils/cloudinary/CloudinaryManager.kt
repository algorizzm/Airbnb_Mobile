package com.airbnb.utils.cloudinary

import android.content.Context
import android.net.Uri
import android.util.Log
import com.airbnb.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Centralised Cloudinary upload layer.
 *
 * Uses a direct HTTPS multipart POST to the Cloudinary Upload API — no Android SDK,
 * no WorkManager, no callback bridging. This avoids the silent-failure mode where
 * the Cloudinary Android SDK dispatches a WorkManager request but never fires the
 * callback on some devices.
 *
 * Initialise once in [com.airbnb.AirbnbApplication.onCreate]:
 *   CloudinaryManager.init(this)
 *
 * Then call from any coroutine:
 *   CloudinaryManager.uploadImage(context, uri)
 *       .onSuccess { url -> /* store url */ }
 *       .onFailure { e  -> /* show error */ }
 *
 * Security: unsigned upload preset only — API secret never stored in the APK.
 */
object CloudinaryManager {

    private const val TAG = "CloudinaryManager"

    private var cloudName: String = ""
    private var uploadPreset: String = ""

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)   // allow time for large image upload
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    /**
     * Reads credentials from [BuildConfig] (baked in at build time from local.properties).
     * Must be called before any upload attempt — typically in Application.onCreate.
     * Logs a warning rather than crashing if credentials are missing.
     */
    fun init(context: Context) {
        cloudName    = BuildConfig.CLOUDINARY_CLOUD_NAME.trim()
        uploadPreset = BuildConfig.CLOUDINARY_UPLOAD_PRESET.trim()

        if (cloudName.isBlank() || uploadPreset.isBlank()) {
            Log.w(TAG, "Cloudinary credentials missing! " +
                "Set CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET in local.properties, " +
                "then rebuild the project.")
        } else {
            Log.d(TAG, "Cloudinary initialised — cloud: $cloudName, preset: $uploadPreset")
        }
    }

    // ── Single upload ─────────────────────────────────────────────────────────

    /**
     * Uploads a single image to Cloudinary and returns its hosted [secure_url].
     *
     * Runs on [Dispatchers.IO]. Safe to call from any coroutine context.
     *
     * @param context  Used to read the image bytes from the content URI.
     * @param uri      Source image URI (content:// or file://).
     * @param folder   Target Cloudinary folder (e.g. "listings", "avatars").
     * @return [Result.success] with the HTTPS URL, or [Result.failure] with a descriptive exception.
     */
    suspend fun uploadImage(
        context: Context,
        uri: Uri,
        folder: String = "listings"
    ): Result<String> = withContext(Dispatchers.IO) {

        // Guard: credentials must be configured
        if (cloudName.isBlank() || uploadPreset.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException(
                    "Cloudinary credentials not set. " +
                    "Add CLOUDINARY_CLOUD_NAME and CLOUDINARY_UPLOAD_PRESET to local.properties " +
                    "and rebuild the project."
                )
            )
        }

        try {
            // 1. Read image bytes from URI
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return@withContext Result.failure(
                    IOException("Cannot read image from URI: $uri")
                )

            Log.d(TAG, "Uploading ${bytes.size / 1024} KB to Cloudinary ($folder)…")

            // 2. Build multipart body
            val imageBody = bytes.toRequestBody("image/jpeg".toMediaType())
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("upload_preset", uploadPreset)
                .addFormDataPart("folder", folder)
                .addFormDataPart("file", "upload.jpg", imageBody)
                .build()

            // 3. Execute the request
            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e(TAG, "Upload failed (${response.code}): $responseBody")
                return@withContext Result.failure(
                    IOException("Cloudinary upload failed (${response.code}): $responseBody")
                )
            }

            // 4. Parse secure_url from JSON response
            val json = JSONObject(responseBody)
            val secureUrl = json.optString("secure_url", "")

            if (secureUrl.isBlank()) {
                Log.e(TAG, "secure_url missing in response: $responseBody")
                return@withContext Result.failure(
                    IOException("Cloudinary response missing secure_url")
                )
            }

            Log.d(TAG, "Upload success → $secureUrl")
            Result.success(secureUrl)

        } catch (e: Exception) {
            Log.e(TAG, "Upload exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    // ── Multi-image upload ────────────────────────────────────────────────────

    /**
     * Uploads multiple images sequentially.
     *
     * @param onProgress Invoked after each image: (finishedCount, totalCount).
     * @return [Result.success] with ordered URL list if all succeed,
     *         [Result.failure] with details if any fail
     *         (successfully uploaded URLs are still returned in the exception message).
     */
    suspend fun uploadImages(
        context: Context,
        uris: List<Uri>,
        folder: String = "listings",
        onProgress: ((uploaded: Int, total: Int) -> Unit)? = null
    ): Result<List<String>> {
        val urls   = mutableListOf<String>()
        val errors = mutableListOf<String>()

        uris.forEachIndexed { index, uri ->
            uploadImage(context, uri, folder)
                .onSuccess { url ->
                    urls.add(url)
                    onProgress?.invoke(index + 1, uris.size)
                }
                .onFailure { e ->
                    errors.add("Image ${index + 1}: ${e.message}")
                }
        }

        return if (errors.isEmpty()) {
            Result.success(urls)
        } else {
            Result.failure(
                RuntimeException(
                    "Failed to upload ${errors.size}/${uris.size} image(s):\n" +
                    errors.joinToString("\n")
                )
            )
        }
    }
}
