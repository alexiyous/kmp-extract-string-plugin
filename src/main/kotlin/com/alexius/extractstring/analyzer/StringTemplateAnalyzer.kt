package com.alexius.extractstring.analyzer

import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtSimpleNameStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * Analyzes a [KtStringTemplateExpression] and produces a [StringAnalysis] describing
 * how it should be extracted to a string resource.
 */
class StringTemplateAnalyzer {

    /**
     * Analyzes the given string template expression.
     *
     * @return [StringAnalysis.PlainString] for simple literals,
     *         [StringAnalysis.ParameterizedString] when all interpolated types are supported,
     *         [StringAnalysis.UnsupportedArgs] when any type is unsupported or unresolved.
     */
    fun analyze(expr: KtStringTemplateExpression): StringAnalysis {
        val entries = expr.entries

        // Plain string — single literal entry
        if (entries.size == 1 && entries[0] is KtLiteralStringTemplateEntry) {
            val raw = entries[0].text
            return StringAnalysis.PlainString(raw, xmlEscape(raw))
        }

        val formatParts = mutableListOf<String>()
        val arguments = mutableListOf<TemplateArg>()
        val offenders = mutableListOf<UnresolvedArg>()
        var argIndex = 1

        for (entry in entries) {
            when (entry) {
                is KtLiteralStringTemplateEntry -> formatParts.add(xmlEscape(entry.text))
                is KtSimpleNameStringTemplateEntry -> {
                    val refExpr = entry.expression ?: continue
                    val exprText = refExpr.text
                    val typeName = resolveTypeName(refExpr)
                    val spec = formatSpecForTypeName(typeName, argIndex)
                    if (spec == null) {
                        offenders.add(UnresolvedArg(exprText, typeName))
                    } else {
                        formatParts.add(spec)
                        arguments.add(TemplateArg(exprText, spec))
                        argIndex++
                    }
                }
                is KtBlockStringTemplateEntry -> {
                    val blockExpr = entry.expression ?: continue
                    val exprText = blockExpr.text
                    val typeName = resolveTypeName(blockExpr)
                    val spec = formatSpecForTypeName(typeName, argIndex)
                    if (spec == null) {
                        offenders.add(UnresolvedArg(exprText, typeName))
                    } else {
                        formatParts.add(spec)
                        arguments.add(TemplateArg(exprText, spec))
                        argIndex++
                    }
                }
                else -> formatParts.add(xmlEscape(entry.text))
            }
        }

        if (offenders.isNotEmpty()) return StringAnalysis.UnsupportedArgs(offenders)
        return StringAnalysis.ParameterizedString(formatParts.joinToString(""), arguments)
    }

    private fun resolveTypeName(expression: KtExpression): String? =
        try {
            analyze(expression) {
                expression.expressionType?.let { type ->
                    type.toString()
                        .removePrefix("kotlin.")
                        .substringBefore("?")
                        .substringAfterLast(".")
                }
            }
        } catch (_: Exception) {
            null
        }

    /**
     * Returns the positional printf-style format specifier for a known Kotlin primitive type,
     * or `null` if the type is unsupported.
     */
    fun formatSpecForTypeName(typeName: String?, index: Int): String? = when (typeName) {
        "Int", "Short", "Byte", "Long" -> "%$index\$d"
        "Float", "Double"              -> "%$index\$f"
        "Char"                         -> "%$index\$c"
        "String"                       -> "%$index\$s"
        else                           -> null
    }

    /** Escapes XML special characters for safe inclusion in strings.xml values. */
    fun xmlEscape(raw: String): String = raw
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    companion object {
        /** Converts a display string to a valid snake_case resource key. */
        fun toSnakeCase(input: String): String =
            input.trim()
                .replace(Regex("[^a-zA-Z0-9]+"), "_")
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .lowercase()
                .trimStart('_')
                .trimEnd('_')
                .ifEmpty { "string" }
    }
}
