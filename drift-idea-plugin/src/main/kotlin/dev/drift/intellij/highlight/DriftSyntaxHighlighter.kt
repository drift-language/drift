package dev.drift.intellij.highlight

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dev.drift.intellij.lexer.DriftLexer
import dev.drift.intellij.lexer.DriftTokenTypes

class DriftSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = DriftLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> =
        when (tokenType) {
            DriftTokenTypes.KEYWORD                              -> pack(DriftColors.KEYWORD)
            DriftTokenTypes.BOOL, DriftTokenTypes.NULL_KW       -> pack(DriftColors.KEYWORD)
            DriftTokenTypes.STRING                               -> pack(DriftColors.STRING)
            DriftTokenTypes.NUMBER                               -> pack(DriftColors.NUMBER)
            DriftTokenTypes.IDENTIFIER                          -> pack(DriftColors.IDENTIFIER)
            DriftTokenTypes.LINE_COMMENT                        -> pack(DriftColors.LINE_COMMENT)
            DriftTokenTypes.OPERATOR,
            DriftTokenTypes.LPAR, DriftTokenTypes.RPAR,
            DriftTokenTypes.LBRACE, DriftTokenTypes.RBRACE,
            DriftTokenTypes.LBRACKET, DriftTokenTypes.RBRACKET  -> pack(DriftColors.OPERATOR)
            TokenType.BAD_CHARACTER                             -> pack(HighlighterColors.BAD_CHARACTER)
            else                                                -> EMPTY
        }
}
