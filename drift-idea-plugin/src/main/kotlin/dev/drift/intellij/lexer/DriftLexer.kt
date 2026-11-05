package dev.drift.intellij.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import kotlin.math.min

class DriftLexer : LexerBase() {

    private var buffer: CharSequence = ""
    private var bufferEnd: Int = 0
    private var tokenStart: Int = 0
    private var tokenEnd: Int = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.bufferEnd = endOffset
        this.tokenStart = startOffset
        this.tokenEnd = startOffset
        this.tokenType = null
        advance()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = bufferEnd

    override fun advance() {
        tokenStart = tokenEnd
        if (tokenStart >= bufferEnd) {
            tokenType = null
            return
        }
        tokenType = nextToken()
    }

    private fun nextToken(): IElementType {
        val c = buffer[tokenStart]

        // Line comment: //
        if (c == '/' && tokenStart + 1 < bufferEnd && buffer[tokenStart + 1] == '/') {
            var i = tokenStart + 2
            while (i < bufferEnd && buffer[i] != '\n' && buffer[i] != '\r') i++
            tokenEnd = i
            return DriftTokenTypes.LINE_COMMENT
        }

        // Whitespace and newlines
        if (c.isWhitespace()) {
            var i = tokenStart + 1
            while (i < bufferEnd && buffer[i].isWhitespace()) i++
            tokenEnd = i
            return DriftTokenTypes.WHITE_SPACE
        }

        // String literal: "..."
        if (c == '"') {
            var i = tokenStart + 1
            while (i < bufferEnd && buffer[i] != '"') {
                if (buffer[i] == '\\') i++ // skip escaped char
                i++
            }
            tokenEnd = if (i < bufferEnd) i + 1 else i
            return DriftTokenTypes.STRING
        }

        // Numeric literal
        if (c.isDigit()) {
            var i = tokenStart + 1
            while (i < bufferEnd && buffer[i].isDigit()) i++
            tokenEnd = i
            return DriftTokenTypes.NUMBER
        }

        // Word: keyword / bool / null / identifier
        if (c.isLetter() || c == '_' || c == '$') {
            var i = tokenStart + 1
            while (i < bufferEnd && (buffer[i].isLetterOrDigit() || buffer[i] == '_' || buffer[i] == '$')) i++
            tokenEnd = i
            return classifyWord(buffer.subSequence(tokenStart, i).toString())
        }

        // Three-char operators
        if (tokenStart + 3 <= bufferEnd) {
            val s3 = buffer.subSequence(tokenStart, tokenStart + 3).toString()
            if (s3 == "..<") {
                tokenEnd = tokenStart + 3
                return DriftTokenTypes.OPERATOR
            }
        }

        // Two-char operators
        if (tokenStart + 2 <= bufferEnd) {
            val s2 = buffer.subSequence(tokenStart, tokenStart + 2).toString()
            if (s2 in TWO_CHAR_OPS) {
                tokenEnd = tokenStart + 2
                return DriftTokenTypes.OPERATOR
            }
        }

        // Single-char braces (need dedicated types for brace matching)
        tokenEnd = tokenStart + 1
        return when (c) {
            '(' -> DriftTokenTypes.LPAR
            ')' -> DriftTokenTypes.RPAR
            '{' -> DriftTokenTypes.LBRACE
            '}' -> DriftTokenTypes.RBRACE
            '[' -> DriftTokenTypes.LBRACKET
            ']' -> DriftTokenTypes.RBRACKET
            '+', '-', '*', '/', '=', ':', '?', ',', '<', '>', '|', '&', '!', '.' ->
                DriftTokenTypes.OPERATOR
            else -> DriftTokenTypes.BAD_CHARACTER
        }
    }

    private fun classifyWord(word: String): IElementType = when (word) {
        "true", "false" -> DriftTokenTypes.BOOL
        "null"          -> DriftTokenTypes.NULL_KW
        in KEYWORDS     -> DriftTokenTypes.KEYWORD
        else            -> DriftTokenTypes.IDENTIFIER
    }

    companion object {
        private val KEYWORDS = setOf(
            "if", "else", "fun", "return", "for", "class",
            "let", "var", "as", "leave", "static", "import", "init"
        )
        private val TWO_CHAR_OPS = setOf(
            "==", "!=", "<=", ">=", "&&", "||", "->", "..", "><"
        )
    }
}
