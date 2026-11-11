package com.marzec.todo.extensions

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.parser.MarkdownParser

fun String.toPlainText(): String {
    if (this.isEmpty()) return ""
    val flavour = GFMFlavourDescriptor()
    val parsedTree = MarkdownParser(flavour).buildMarkdownTreeFromString(this)
    val sb = StringBuilder()
    parsedTree.walk(this, sb)
    return sb.toString().trim()
}

private fun ASTNode.walk(originalText: String, sb: StringBuilder) {
    when (type) {
        MarkdownElementTypes.INLINE_LINK,
        MarkdownElementTypes.FULL_REFERENCE_LINK,
        MarkdownElementTypes.SHORT_REFERENCE_LINK,
        MarkdownElementTypes.IMAGE,
        MarkdownElementTypes.AUTOLINK -> walkLinkLike(originalText, sb)
        MarkdownElementTypes.LINK_TEXT -> walkLinkText(originalText, sb)
        else -> walkDefault(originalText, sb)
    }
}

private fun ASTNode.walkLinkLike(originalText: String, sb: StringBuilder) {
    val linkText = children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
    if (linkText != null) {
        linkText.walk(originalText, sb)
    } else {
        children.forEach { it.walk(originalText, sb) }
    }
}

private fun ASTNode.walkLinkText(originalText: String, sb: StringBuilder) {
    children.forEach { child ->
        if (child.type != MarkdownTokenTypes.LBRACKET && child.type != MarkdownTokenTypes.RBRACKET) {
            child.walk(originalText, sb)
        }
    }
}

private fun ASTNode.walkDefault(originalText: String, sb: StringBuilder) {
    if (children.isEmpty()) {
        if (type.isContentToken()) {
            sb.append(getTextInNode(originalText))
        }
    } else {
        children.forEach { it.walk(originalText, sb) }
    }
}

private fun IElementType.isContentToken(): Boolean = this in contentTokenTypes

private val contentTokenTypes = setOf(
    MarkdownTokenTypes.TEXT,
    MarkdownTokenTypes.CODE_LINE,
    MarkdownTokenTypes.WHITE_SPACE,
    MarkdownTokenTypes.ATX_CONTENT,
    MarkdownTokenTypes.SETEXT_CONTENT,
    MarkdownTokenTypes.HARD_LINE_BREAK,
    MarkdownTokenTypes.EOL,
    MarkdownTokenTypes.AUTOLINK,
    MarkdownTokenTypes.EMAIL_AUTOLINK
)
