package com.alexius.extractstring.analyzer

sealed class StringAnalysis {
    /**
     * A plain string with no interpolations.
     *
     * @property rawValue     The original Kotlin source text as it appears in the source file.
     * @property xmlSafeValue The XML-escaped form suitable for writing into a `strings.xml` resource file.
     */
    data class PlainString(
        val rawValue: String,
        val xmlSafeValue: String
    ) : StringAnalysis()

    /** A string template converted to a format string. */
    data class ParameterizedString(
        val formatString: String,   // e.g. "Hello %1$s, you have %2$d items"
        val arguments: List<TemplateArg>
    ) : StringAnalysis()

    /** One or more args had unsupported or unresolved types — abort. */
    data class UnsupportedArgs(
        val offenders: List<UnresolvedArg>
    ) : StringAnalysis()
}

/**
 * Represents a template argument whose type could not be resolved or is not supported.
 *
 * @property expressionText The source text of the expression (e.g. `"user.name"`).
 * @property typeName       The resolved type name, or `null` if the type could not be determined.
 */
data class UnresolvedArg(
    val expressionText: String,
    val typeName: String?
)

/**
 * A single interpolated argument from a Kotlin string template, paired with its printf-style format specifier.
 *
 * @property expressionText The source text of the expression (e.g. `"user.name"`, `"inbox.count()"`).
 * @property formatSpec     The printf-style format specifier for this argument (e.g. `"%1$s"`, `"%2$d"`).
 */
data class TemplateArg(
    val expressionText: String,
    val formatSpec: String
)
