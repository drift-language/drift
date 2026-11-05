package dev.drift.intellij

import com.intellij.lang.Language

class DriftLanguage private constructor() : Language("Drift") {
    companion object {
        @JvmField
        val INSTANCE: DriftLanguage = DriftLanguage()
    }
}
