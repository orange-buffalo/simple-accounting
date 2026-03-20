package io.orangebuffalo.simpleaccounting.business.documents

private val mimeTypesByExtension = mapOf(
    "pdf" to "application/pdf",
    "txt" to "text/plain",
    "jpg" to "image/jpeg",
    "jpeg" to "image/jpeg",
    "png" to "image/png",
    "gif" to "image/gif",
    "bmp" to "image/bmp",
    "svg" to "image/svg+xml",
    "webp" to "image/webp",
    "tiff" to "image/tiff",
    "tif" to "image/tiff",
    "doc" to "application/msword",
    "docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "xls" to "application/vnd.ms-excel",
    "xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "ppt" to "application/vnd.ms-powerpoint",
    "pptx" to "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "csv" to "text/csv",
    "zip" to "application/zip",
    "html" to "text/html",
    "htm" to "text/html",
)

private const val DEFAULT_MIME_TYPE = "application/octet-stream"

fun getMimeTypeByFileName(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase()
    if (extension.isEmpty() || extension == fileName.lowercase()) {
        return DEFAULT_MIME_TYPE
    }
    return mimeTypesByExtension[extension] ?: DEFAULT_MIME_TYPE
}
