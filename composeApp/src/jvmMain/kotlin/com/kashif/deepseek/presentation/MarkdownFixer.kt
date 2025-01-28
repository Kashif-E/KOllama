package com.kashif.deepseek.presentation

data class FormatContext(
    val lastLineEndsWithPunctuation: Boolean,
    val lastLineStartsWithMarkdown: Boolean,
    val nextLineStartsWithMarkdown: Boolean,
    val isCodeBlock: Boolean,
    val isListItem: Boolean,
    val isHeader: Boolean,
    val isSectionBreak: Boolean,
    val isTableRow: Boolean,
    val isBlockquote: Boolean,
    val isTaskListItem: Boolean,
    val isHorizontalRule: Boolean,
    val isFencedCodeBlock: Boolean,
    val isFootnote: Boolean,
    val isDefinitionList: Boolean,
    val depth: Int,
    val listItemLevel: Int,
    val blockquoteLevel: Int,
    val codeBlockLanguage: String?,
    val tableColumnCount: Int,
    val isTableHeader: Boolean,
    val isTableDivider: Boolean,
    val hasInlineCode: Boolean,
    val hasEmphasis: Boolean,
    val hasStrongEmphasis: Boolean,
    val hasLinks: Boolean,
    val hasImages: Boolean
)

object MarkdownFormatting {

    val codeBlockMarkers = setOf("```", "~~~")
    val headerMarkers = setOf("#", "##", "###", "####", "#####", "######")
    val listMarkers = setOf("-", "*", "+")
    val blockquoteMarker = ">"
    val horizontalRuleMarkers = setOf("---", "___", "***", "- - -", "_ _ _", "* * *")


    val numberListPattern = Regex("^\\d+\\.")
    val tableRowPattern = Regex("^\\|.*\\|\\s*$")
    val tableDividerPattern = Regex("^\\|[-:| ]+\\|\\s*$")
    val taskListPattern = Regex("^- \\[[ xX]]\\]")
    val footnotePattern = Regex("^\\[\\^[\\w-]+\\]:")
    val definitionListPattern = Regex("^[\\w-]+:(?:\\s|$)")
    val inlineCodePattern = Regex("`[^`]+`")
    val emphasisPattern = Regex("(?<!\\*)\\*[^\\*]+\\*(?!\\*)|(?<!_)_[^_]+_(?!_)")
    val strongEmphasisPattern = Regex("\\*\\*[^\\*]+\\*\\*|__[^_]+__")
    val linkPattern = Regex("\\[([^\\]]+)\\]\\([^\\)]+\\)")
    val imagePattern = Regex("!\\[([^\\]]+)\\]\\([^\\)]+\\)")


    val sectionStarters = setOf(
        "In ",
        "The ",
        "This ",
        "These ",
        "Here ",
        "For ",
        "When ",
        "While ",
        "First",
        "Second",
        "Third",
        "Finally",
        "However",
        "Moreover",
        "Nevertheless",
        "Consequently",
        "Therefore",
        "Thus",
        "Hence",
        "Additionally"
    )
    val sentenceEnders = setOf('.', '!', '?', ':', ';')
    val markdownSpecialChars = setOf('#', '-', '*', '>', '`', '|', '+', '~', '{', '<', '[', '!')


    val blockElements = setOf(
        "```",
        "~~~",
        "###",
        "##",
        "#",
        "> ",
        "- ",
        "* ",
        "+ ",
        "1. ",
        "| ",
        "---",
        "___",
        "***",
        "- [ ]",
        "- [x]",
        "[^",
        ": "
    )
}

fun analyzeText(text: String): FormatContext {
    val lines = text.lines()
    val currentLine = lines.lastOrNull()?.trimEnd() ?: ""
    val nextLine = lines.getOrNull(lines.size - 1)?.trimStart() ?: ""
    var codeBlockLanguage: String? = null

    var inCodeBlock = false

    for (line in lines) {
        if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
            inCodeBlock = !inCodeBlock
            if (inCodeBlock) {

                codeBlockLanguage = when {
                    currentLine.startsWith("```") && currentLine.length > 3 -> currentLine.substring(
                        3
                    ).trim()

                    currentLine.startsWith("~~~") && currentLine.length > 3 -> currentLine.substring(
                        3
                    ).trim()

                    else -> null
                }
            }
        }
    }


    val blockquoteLevel = currentLine.takeWhile { it == '>' }.count()
    val listItemLevel = currentLine.takeWhile { it.isWhitespace() }.count() / 2


    val isTableRow = currentLine.matches(MarkdownFormatting.tableRowPattern)
    val isTableHeader = isTableRow && lines.getOrNull(lines.size - 2)
        ?.matches(MarkdownFormatting.tableDividerPattern) == true
    val isTableDivider = currentLine.matches(MarkdownFormatting.tableDividerPattern)
    val tableColumnCount = if (isTableRow) currentLine.count { it == '|' } - 1 else 0



    return FormatContext(lastLineEndsWithPunctuation = !inCodeBlock && currentLine.lastOrNull() in MarkdownFormatting.sentenceEnders,
        lastLineStartsWithMarkdown = !inCodeBlock && MarkdownFormatting.blockElements.any {
            currentLine.trimStart().startsWith(it)
        },
        nextLineStartsWithMarkdown = !inCodeBlock && MarkdownFormatting.blockElements.any {
            nextLine.startsWith(
                it
            )
        },
        isCodeBlock = inCodeBlock || currentLine.trimStart()
            .startsWith("```") || currentLine.trimStart().startsWith("~~~"),
        isListItem = !inCodeBlock && (MarkdownFormatting.listMarkers.any {
            currentLine.trimStart().startsWith("$it ")
        } || currentLine.trimStart().matches(MarkdownFormatting.numberListPattern)),
        isHeader = !inCodeBlock && MarkdownFormatting.headerMarkers.any {
            currentLine.trimStart().startsWith(it)
        },
        isSectionBreak = !inCodeBlock && MarkdownFormatting.sectionStarters.any {
            nextLine.startsWith(
                it
            )
        },
        isTableRow = !inCodeBlock && isTableRow,
        isBlockquote = !inCodeBlock && currentLine.trimStart().startsWith(">"),
        isTaskListItem = !inCodeBlock && currentLine.trimStart()
            .matches(MarkdownFormatting.taskListPattern),
        isHorizontalRule = !inCodeBlock && MarkdownFormatting.horizontalRuleMarkers.contains(
            currentLine.trim()
        ),
        isFencedCodeBlock = currentLine.trimStart().startsWith("```") || currentLine.trimStart()
            .startsWith("~~~"),
        isFootnote = !inCodeBlock && currentLine.matches(MarkdownFormatting.footnotePattern),
        isDefinitionList = !inCodeBlock && currentLine.matches(MarkdownFormatting.definitionListPattern),
        depth = if (!inCodeBlock) MarkdownFormatting.headerMarkers.find {
            currentLine.trimStart().startsWith(it)
        }?.length ?: 0 else 0,
        listItemLevel = if (!inCodeBlock) listItemLevel else 0,
        blockquoteLevel = if (!inCodeBlock) blockquoteLevel else 0,
        codeBlockLanguage = codeBlockLanguage,
        tableColumnCount = tableColumnCount,
        isTableHeader = isTableHeader,
        isTableDivider = isTableDivider,
        hasInlineCode = !inCodeBlock && MarkdownFormatting.inlineCodePattern.containsMatchIn(
            currentLine
        ),
        hasEmphasis = !inCodeBlock && MarkdownFormatting.emphasisPattern.containsMatchIn(currentLine),
        hasStrongEmphasis = !inCodeBlock && MarkdownFormatting.strongEmphasisPattern.containsMatchIn(
            currentLine
        ),
        hasLinks = !inCodeBlock && MarkdownFormatting.linkPattern.containsMatchIn(currentLine),
        hasImages = !inCodeBlock && MarkdownFormatting.imagePattern.containsMatchIn(currentLine)
    )
}

data class FormattingDecision(
    val newlines: Int,
    val indent: Int,
    val addCodeBlockLanguage: Boolean,
    val preserveSpacing: Boolean,
    val shouldAlignTable: Boolean,
    val shouldAddTableDivider: Boolean,
    val shouldIndentBlockquote: Boolean,
    val shouldIndentList: Boolean
)

fun calculateNewlines(current: FormatContext, next: FormatContext): Int {
    return when {

        current.isFencedCodeBlock || next.isFencedCodeBlock -> {
            when {

                !current.isFencedCodeBlock && next.isFencedCodeBlock -> 2

                current.isFencedCodeBlock && !next.isFencedCodeBlock -> 2

                else -> 1
            }
        }


        next.isHeader || next.isHorizontalRule -> 2
        current.isHeader || current.isHorizontalRule -> 2


        next.isDefinitionList || next.isFootnote -> 2
        current.isDefinitionList || current.isFootnote -> 1


        current.isListItem && next.isListItem -> {
            when {
                current.listItemLevel != next.listItemLevel -> 1
                current.isTaskListItem || next.isTaskListItem -> 1
                else -> 1
            }
        }

        current.isBlockquote && next.isBlockquote -> {
            when {
                current.blockquoteLevel != next.blockquoteLevel -> 1
                else -> 1
            }
        }


        current.isTableRow && next.isTableRow -> {
            when {
                current.isTableHeader -> 0
                next.isTableDivider -> 0
                else -> 1
            }
        }


        current.lastLineEndsWithPunctuation && next.isSectionBreak -> 2
        current.lastLineEndsWithPunctuation && next.nextLineStartsWithMarkdown -> 1


        else -> 1
    }
}

fun calculateIndent(current: FormatContext, next: FormatContext): Int {
    return when {
        next.isListItem -> 2 * next.listItemLevel
        next.isBlockquote -> 2 * next.blockquoteLevel
        next.isDefinitionList -> 4
        next.isFootnote -> 4
        next.isFencedCodeBlock -> 0
        else -> 0
    }
}

fun determineFormatting(current: String, next: String): FormattingDecision {
    val currentContext = analyzeText(current)
    val nextContext = analyzeText(next)

    return FormattingDecision(
        newlines = calculateNewlines(currentContext, nextContext),
        indent = calculateIndent(currentContext, nextContext),
        addCodeBlockLanguage = shouldAddCodeBlockLanguage(currentContext, nextContext),
        preserveSpacing = shouldPreserveSpacing(currentContext, nextContext),
        shouldAlignTable = shouldAlignTable(currentContext, nextContext),
        shouldAddTableDivider = shouldAddTableDivider(currentContext, nextContext),
        shouldIndentBlockquote = shouldIndentBlockquote(currentContext, nextContext),
        shouldIndentList = shouldIndentList(currentContext, nextContext)
    )
}

private fun shouldAddCodeBlockLanguage(current: FormatContext, next: FormatContext): Boolean =
    !current.isFencedCodeBlock && next.isFencedCodeBlock && next.codeBlockLanguage == null

private fun shouldPreserveSpacing(current: FormatContext, next: FormatContext): Boolean =
    current.isFencedCodeBlock || next.isFencedCodeBlock || current.isTableRow || next.isTableRow || current.hasInlineCode || next.hasInlineCode

private fun shouldAlignTable(current: FormatContext, next: FormatContext): Boolean =
    (current.isTableRow && next.isTableRow) || (current.isTableHeader && next.isTableDivider)

private fun shouldAddTableDivider(current: FormatContext, next: FormatContext): Boolean =
    current.isTableHeader && !next.isTableDivider

private fun shouldIndentBlockquote(current: FormatContext, next: FormatContext): Boolean =
    next.isBlockquote && next.blockquoteLevel > current.blockquoteLevel

private fun shouldIndentList(current: FormatContext, next: FormatContext): Boolean =
    next.isListItem && next.listItemLevel > current.listItemLevel

fun cleanupFormatting(text: String): String {
    var inCodeBlock = false
    return text.lines().joinToString("\n") { line ->
        if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
            inCodeBlock = !inCodeBlock

            when {
                line.startsWith("```") -> {
                    val lang = line.substring(3).trim()
                    if (lang.isNotEmpty()) "```$lang" else "```"
                }

                line.startsWith("~~~") -> {
                    val lang = line.substring(3).trim()
                    if (lang.isNotEmpty()) "~~~$lang" else "~~~"
                }

                else -> line
            }
        } else if (inCodeBlock) {
            line
        } else {
            line.replace(Regex("(#+)\\s*([^\\n]+)"), "\n$1 $2\n")
                .replace(Regex("^([-*+])\\s+"), "$1 ").replace(Regex("\\n([-*+])\\s+"), "\n$1 ")
                .replace(Regex("^(\\d+\\.)\\s+"), "$1 ").replace(Regex("\\n(\\d+\\.)\\s+"), "\n$1 ")
                .replace(Regex("^(- \\[[ xX]\\])\\s+"), "$1 ")
                .replace(Regex("\\n(- \\[[ xX]\\])\\s+"), "\n$1 ").replace(Regex("^>\\s*"), "> ")
                .replace(Regex("\\n>\\s*"), "\n> ").replace(Regex("\\|\\s{2,}"), "| ")
                .replace(Regex("\\s{2,}\\|"), " |").replace(Regex("\\|\\s*\\n\\s*\\|"), "|\n|")
                .replace(Regex("^([\\w-]+):\\s{2,}"), "$1: ")
                .replace(Regex("\\n([\\w-]+):\\s{2,}"), "\n$1: ")
                .replace(Regex("^(\\[\\^[\\w-]+\\]):\\s{2,}"), "$1: ")
                .replace(Regex("\\n(\\[\\^[\\w-]+\\]):\\s{2,}"), "\n$1: ")
                .replace(Regex("\\n[-*_]\\s*[-*_]\\s*[-*_][\\s-*_]*\\n"), "\n---\n")
                .replace(Regex("\n{3,}"), "\n\n").replace(Regex("`\\s+"), "`")
                .replace(Regex("\\s+`"), "`").replace(Regex("\\*\\s+"), "*")
                .replace(Regex("\\s+\\*"), "*").replace(Regex("_\\s+"), "_")
                .replace(Regex("\\s+_"), "_")

        }
    }.trimEnd()
}