package dev.drift.intellij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

class DriftFileType private constructor() : LanguageFileType(DriftLanguage.INSTANCE) {

    companion object {
        @JvmField
        val INSTANCE: DriftFileType = DriftFileType()
        const val EXTENSION = "drift"
    }

    override fun getName(): String = "Drift"
    override fun getDescription(): String = "Drift language source file"
    override fun getDefaultExtension(): String = EXTENSION
    override fun getIcon(): Icon = DriftIcons.FILE
}
