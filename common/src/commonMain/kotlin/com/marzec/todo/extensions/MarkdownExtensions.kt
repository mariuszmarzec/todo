package com.marzec.todo.extensions

import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.CompositeASTNode
import org.intellij.markdown.ast.LeafASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMTokenTypes

/**
 * Extracts a plain-text representation of the first physical line of a Markdown
 * document by walking the JetBrains Markdown AST produced by the MikePenz
 * renderer's parser.
 *
 * Only the first physical line (up to the first `\n`) is considered, matching the
 * compact list preview behaviour of [descriptionWithProgress].
 *
 * Markdown syntax markers (heading prefixes, list bullets, emphasis markers,
 * link/image brackets, code fences, blockquote prefixes, ...) are dropped,
 * while the human-readable text — including link and image alt text — is kept.
 *
 * The original [content] is never mutated; this function only produces a
 * transformed preview string.
 */
fun String.toMarkdownPlainTextFirstLine(): String {
    val firstLine = lines().firstOrNull() ?: return ""
    if (firstLine.isEmpty()) return ""

    val flavour = org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor()
    val parser = org.intellij.markdown.parser.MarkdownParser(
        flavour = flavour,
        cancellationToken = org.intellij.markdown.parser.CancellationToken.NonCancellable
    )
    val ast = parser.buildMarkdownTreeFromString(firstLine)

    val builder = StringBuilder()
    collectPlainText(ast, firstLine, builder)
    return builder.toString().trim()
}

private fun collectPlainText(
    node: ASTNode,
    content: CharSequence,
    builder: StringBuilder,
) {
    // Link targets and reference label markers are pure syntax URLs, never human-readable preview text:
    // `[label](https://…)` keeps `label`, `![alt](https://…)` keeps `alt`,
    // `[label][ref]` keeps `label`, `[label]` keeps `label`.
    if (node.type == MarkdownElementTypes.LINK_DESTINATION) {
        return
    }
    when (node) {
        is LeafASTNode -> {
            val text = node.type.extractText(content, node.startOffset, node.endOffset)
            if (text != null) {
                builder.append(text)
            }
        }
        is CompositeASTNode -> {
            node.children.forEach { collectPlainText(it, content, builder) }
        }
    }
}

/**
 * Returns the plain text contributed by a leaf node, or `null` when the leaf is a
 * pure Markdown marker that should be omitted from the plain-text preview.
 *
 * Keepers:
 *  - [MarkdownTokenTypes.TEXT] — regular text inside paragraphs, headings, links, ...
 *  - [MarkdownTokenTypes.WHITE_SPACE] — separators between inline elements
 *  - [MarkdownTokenTypes.CODE_LINE] — text inside code spans
 *  - [MarkdownTokenTypes.ATX_CONTENT] / [MarkdownTokenTypes.SETEXT_CONTENT] — heading body
 *  - [MarkdownTokenTypes.AUTOLINK] / [MarkdownTokenTypes.EMAIL_AUTOLINK] / [GFMTokenTypes.GFM_AUTOLINK] — autolink URL
 *  - [MarkdownTokenTypes.LINK_ID] — reference link labels
 *
 * Droppers: all structural markers (`#`, `*`, `_`, `[`, `]`, `(`, `)`, `!`, `>`,
 * backticks, list bullets, ...). Link destinations are skipped separately in
 * [collectPlainText].
 */
private fun IElementType.extractText(
    content: CharSequence,
    startOffset: Int,
    endOffset: Int,
): String? = when (this) {
    MarkdownTokenTypes.TEXT,
    MarkdownTokenTypes.WHITE_SPACE,
    MarkdownTokenTypes.CODE_LINE,
    MarkdownTokenTypes.ATX_CONTENT,
    MarkdownTokenTypes.SETEXT_CONTENT,
    MarkdownTokenTypes.AUTOLINK,
    MarkdownTokenTypes.EMAIL_AUTOLINK,
    GFMTokenTypes.GFM_AUTOLINK,
    MarkdownTokenTypes.LINK_ID -> content.subSequence(startOffset, endOffset).toString()

    else -> null
}