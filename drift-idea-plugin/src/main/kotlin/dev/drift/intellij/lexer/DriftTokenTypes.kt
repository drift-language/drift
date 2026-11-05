package dev.drift.intellij.lexer

import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dev.drift.intellij.DriftLanguage

object DriftTokenTypes {
    @JvmField val KEYWORD       = IElementType("DRIFT_KEYWORD",    DriftLanguage.INSTANCE)
    @JvmField val IDENTIFIER    = IElementType("DRIFT_IDENT",      DriftLanguage.INSTANCE)
    @JvmField val NUMBER        = IElementType("DRIFT_NUMBER",     DriftLanguage.INSTANCE)
    @JvmField val STRING        = IElementType("DRIFT_STRING",     DriftLanguage.INSTANCE)
    @JvmField val BOOL          = IElementType("DRIFT_BOOL",       DriftLanguage.INSTANCE)
    @JvmField val NULL_KW       = IElementType("DRIFT_NULL",       DriftLanguage.INSTANCE)
    @JvmField val LINE_COMMENT  = IElementType("DRIFT_LINE_CMT",   DriftLanguage.INSTANCE)
    @JvmField val OPERATOR      = IElementType("DRIFT_OPERATOR",   DriftLanguage.INSTANCE)
    @JvmField val LPAR          = IElementType("DRIFT_LPAR",       DriftLanguage.INSTANCE)
    @JvmField val RPAR          = IElementType("DRIFT_RPAR",       DriftLanguage.INSTANCE)
    @JvmField val LBRACE        = IElementType("DRIFT_LBRACE",     DriftLanguage.INSTANCE)
    @JvmField val RBRACE        = IElementType("DRIFT_RBRACE",     DriftLanguage.INSTANCE)
    @JvmField val LBRACKET      = IElementType("DRIFT_LBRACKET",   DriftLanguage.INSTANCE)
    @JvmField val RBRACKET      = IElementType("DRIFT_RBRACKET",   DriftLanguage.INSTANCE)
    @JvmField val WHITE_SPACE   = TokenType.WHITE_SPACE
    @JvmField val BAD_CHARACTER = TokenType.BAD_CHARACTER
}
