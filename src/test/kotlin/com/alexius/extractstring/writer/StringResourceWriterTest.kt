package com.alexius.extractstring.writer

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class StringResourceWriterTest : BasePlatformTestCase() {

    fun `test findEntry returns null when key missing`() {
        val stringsFile = myFixture.tempDirFixture.createFile(
            "strings.xml",
            """<resources><string name="existing">Hello</string></resources>"""
        )
        val writer = StringResourceWriter()
        assertNull(writer.findEntry(stringsFile, "missing_key"))
    }

    fun `test findEntry returns value when key exists`() {
        val stringsFile = myFixture.tempDirFixture.createFile(
            "strings.xml",
            """<resources><string name="greeting">Hello</string></resources>"""
        )
        val writer = StringResourceWriter()
        assertEquals("Hello", writer.findEntry(stringsFile, "greeting"))
    }
}
