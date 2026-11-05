package dev.drift.intellij.highlight

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import dev.drift.intellij.DriftIcons
import javax.swing.Icon

class DriftColorSettingsPage : ColorSettingsPage {

    private val ATTRS = arrayOf(
        AttributesDescriptor("Keyword",      DriftColors.KEYWORD),
        AttributesDescriptor("Identifier",   DriftColors.IDENTIFIER),
        AttributesDescriptor("String",       DriftColors.STRING),
        AttributesDescriptor("Number",       DriftColors.NUMBER),
        AttributesDescriptor("Operator",     DriftColors.OPERATOR),
        AttributesDescriptor("Line comment", DriftColors.LINE_COMMENT),
    )

    override fun getIcon(): Icon = DriftIcons.FILE
    override fun getHighlighter(): SyntaxHighlighter = DriftSyntaxHighlighter()
    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null
    override fun getAttributeDescriptors(): Array<AttributesDescriptor> = ATTRS
    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY
    override fun getDisplayName(): String = "Drift"

    override fun getDemoText(): String = """
        // Sample Drift code
        import greet from "greet"

        class Counter(start: Int) {
            var count = start
            static fun zero() { return Counter(0) }
            fun increment() { var count = count + 1 }
        }

        fun greet(name: String): String {
            let msg = "Hello, " + name
            return msg
        }

        let c = Counter(start = 0)
        for 1..5 { as i c.increment() }

        if c.count > 3 {
            let result = greet("World")
        } else {
            let result = null
        }
    """.trimIndent()
}
