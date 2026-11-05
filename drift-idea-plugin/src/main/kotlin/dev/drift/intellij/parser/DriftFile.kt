package dev.drift.intellij.parser

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import dev.drift.intellij.DriftFileType
import dev.drift.intellij.DriftLanguage

class DriftFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, DriftLanguage.INSTANCE) {
    override fun getFileType(): FileType = DriftFileType.INSTANCE
    override fun toString(): String = "Drift File"
}
