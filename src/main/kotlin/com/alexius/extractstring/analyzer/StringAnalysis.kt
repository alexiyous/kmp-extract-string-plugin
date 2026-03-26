package com.alexius.extractstring.analyzer

sealed class StringAnalysis {
    /** A plain string with no interpolations. */
    data class PlainString(
        val rawValue: String,
        val xmlSafeValue: String
    ) : StringAnalysis()

    /** A string template converted to a format string. */
    data class ParameterizedString(
        val formatString: String,   // e.g. "Hello %1$s, you have %2$d items"
        val args: List<TemplateArg>
    ) : StringAnalysis()

    /** One or more args had unsupported or unresolved types — abort. */
    data class UnsupportedArgs(
        val offenders: List<Pair<String, String?>> // expression text → resolved type name (null = unresolved)
    ) : StringAnalysis()
}

data class TemplateArg(
    val expressionText: String, // "user.name", "inbox.count()"
    val formatSpec: String      // "%1$s", "%2$d"
)
