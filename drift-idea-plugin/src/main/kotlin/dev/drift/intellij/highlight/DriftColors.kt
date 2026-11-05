package dev.drift.intellij.highlight

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object DriftColors {
    @JvmField
    val KEYWORD = TextAttributesKey.createTextAttributesKey(
        "DRIFT_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD
    )
    @JvmField
    val IDENTIFIER = TextAttributesKey.createTextAttributesKey(
        "DRIFT_IDENTIFIER", DefaultLanguageHighlighterColors.IDENTIFIER
    )
    @JvmField
    val STRING = TextAttributesKey.createTextAttributesKey(
        "DRIFT_STRING", DefaultLanguageHighlighterColors.STRING
    )
    @JvmField
    val NUMBER = TextAttributesKey.createTextAttributesKey(
        "DRIFT_NUMBER", DefaultLanguageHighlighterColors.NUMBER
    )
    @JvmField
    val OPERATOR = TextAttributesKey.createTextAttributesKey(
        "DRIFT_OPERATOR", DefaultLanguageHighlighterColors.OPERATION_SIGN
    )
    @JvmField
    val LINE_COMMENT = TextAttributesKey.createTextAttributesKey(
        "DRIFT_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT
    )
}
