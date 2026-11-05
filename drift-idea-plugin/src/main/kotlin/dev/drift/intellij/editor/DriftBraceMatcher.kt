package dev.drift.intellij.editor

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import dev.drift.intellij.lexer.DriftTokenTypes

class DriftBraceMatcher : PairedBraceMatcher {

    private val PAIRS = arrayOf(
        BracePair(DriftTokenTypes.LPAR,     DriftTokenTypes.RPAR,     false),
        BracePair(DriftTokenTypes.LBRACE,   DriftTokenTypes.RBRACE,   true),
        BracePair(DriftTokenTypes.LBRACKET, DriftTokenTypes.RBRACKET, false),
    )

    override fun getPairs(): Array<BracePair> = PAIRS

    override fun isPairedBracesAllowedBeforeType(lbraceType: IElementType, contextType: IElementType?): Boolean = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int = openingBraceOffset
}
