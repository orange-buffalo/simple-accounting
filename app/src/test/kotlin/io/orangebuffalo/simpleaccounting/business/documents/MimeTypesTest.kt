package io.orangebuffalo.simpleaccounting.business.documents

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class MimeTypesTest {

    @ParameterizedTest
    @CsvSource(
        "document.pdf, application/pdf",
        "DOCUMENT.PDF, application/pdf",
        "photo.jpg, image/jpeg",
        "photo.jpeg, image/jpeg",
        "image.png, image/png",
        "notes.txt, text/plain",
        "animation.gif, image/gif",
        "bitmap.bmp, image/bmp",
        "vector.svg, image/svg+xml",
        "photo.webp, image/webp",
        "scan.tiff, image/tiff",
        "scan.tif, image/tiff",
        "letter.doc, application/msword",
        "letter.docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "data.xls, application/vnd.ms-excel",
        "data.xlsx, application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "slides.ppt, application/vnd.ms-powerpoint",
        "slides.pptx, application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "export.csv, text/csv",
        "archive.zip, application/zip",
        "page.html, text/html",
        "page.htm, text/html",
    )
    fun `should resolve mime type for known extensions`(fileName: String, expectedMimeType: String) {
        getMimeTypeByFileName(fileName).shouldBe(expectedMimeType)
    }

    @ParameterizedTest
    @CsvSource(
        "file.unknown",
        "file.exe",
        "file.bin",
        "noextension",
        "file.",
    )
    fun `should return octet-stream for unknown extensions`(fileName: String) {
        getMimeTypeByFileName(fileName).shouldBe("application/octet-stream")
    }
}
