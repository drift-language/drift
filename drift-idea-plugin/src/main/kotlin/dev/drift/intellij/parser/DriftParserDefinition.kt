package dev.drift.intellij.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import dev.drift.intellij.DriftLanguage
import dev.drift.intellij.lexer.DriftLexer
import dev.drift.intellij.lexer.DriftTokenTypes

class DriftParserDefinition : ParserDefinition {

    companion object {
        @JvmField
        val FILE = IFileElementType(DriftLanguage.INSTANCE)
    }

    override fun createLexer(project: Project?): Lexer = DriftLexer()

    override fun createParser(project: Project?): PsiParser =
        throw UnsupportedOperationException("Drift plugin does not provide a full parser")

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet =
        TokenSet.create(TokenType.WHITE_SPACE)

    override fun getCommentTokens(): TokenSet =
        TokenSet.create(DriftTokenTypes.LINE_COMMENT)

    override fun getStringLiteralElements(): TokenSet =
        TokenSet.create(DriftTokenTypes.STRING)

    override fun createElement(node: ASTNode): PsiElement =
        throw UnsupportedOperationException("Drift plugin does not provide PSI elements")

    override fun createFile(viewProvider: FileViewProvider): PsiFile = DriftFile(viewProvider)
}
