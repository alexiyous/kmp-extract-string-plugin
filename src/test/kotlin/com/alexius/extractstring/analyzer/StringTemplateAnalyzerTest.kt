package com.alexius.extractstring.analyzer

import org.junit.Assert.*
import org.junit.Test

class StringTemplateAnalyzerTest {

    private val analyzer = StringTemplateAnalyzer()

    @Test
    fun `xmlEscape replaces all special chars`() {
        assertEquals("a &amp; b &lt;c&gt; &quot;d&quot; &apos;e&apos;",
            analyzer.xmlEscape("a & b <c> \"d\" 'e'"))
    }

    @Test
    fun `toSnakeCase converts mixed string`() {
        assertEquals("pick_location", StringTemplateAnalyzer.toSnakeCase("Pick Location"))
        assertEquals("hello_world", StringTemplateAnalyzer.toSnakeCase("Hello World!"))
    }

    @Test
    fun `formatSpecForTypeName returns d for Int types`() {
        assertEquals("%1\$d", analyzer.formatSpecForTypeName("Int", 1))
        assertEquals("%2\$d", analyzer.formatSpecForTypeName("Long", 2))
        assertEquals("%1\$d", analyzer.formatSpecForTypeName("Short", 1))
        assertEquals("%1\$d", analyzer.formatSpecForTypeName("Byte", 1))
    }

    @Test
    fun `formatSpecForTypeName returns f for Float and Double`() {
        assertEquals("%1\$f", analyzer.formatSpecForTypeName("Float", 1))
        assertEquals("%1\$f", analyzer.formatSpecForTypeName("Double", 1))
    }

    @Test
    fun `formatSpecForTypeName returns c for Char`() {
        assertEquals("%1\$c", analyzer.formatSpecForTypeName("Char", 1))
    }

    @Test
    fun `formatSpecForTypeName returns s for String`() {
        assertEquals("%1\$s", analyzer.formatSpecForTypeName("String", 1))
    }

    @Test
    fun `formatSpecForTypeName returns null for unknown type`() {
        assertNull(analyzer.formatSpecForTypeName("MyDataClass", 1))
        assertNull(analyzer.formatSpecForTypeName(null, 1))
    }
}
