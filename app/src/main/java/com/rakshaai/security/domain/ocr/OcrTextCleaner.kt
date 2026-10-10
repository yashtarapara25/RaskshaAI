package com.rakshaai.security.domain.ocr

object OcrTextCleaner {

    fun clean(rawOcrText: String): String {
        if (rawOcrText.isBlank()) return ""

        var text = rawOcrText
            .replace("\r", "")
            .replace(Regex("""[^\S\r\n]+"""), " ") // Normalize multiple spaces
            .trim()

        // Fix common OCR typos in URL protocols while preserving the text
        text = text.replace(Regex("""(?i)\bh[tT]{2}p[sS]?\s*:\s*/\s*/"""), "https://")
            .replace(Regex("""(?i)\bwww\s*\.\s*"""), "www.")

        return text
    }
}
