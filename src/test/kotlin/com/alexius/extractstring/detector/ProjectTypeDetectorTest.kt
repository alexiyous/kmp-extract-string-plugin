package com.alexius.extractstring.detector

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ProjectTypeDetectorTest : BasePlatformTestCase() {

    fun `test toSnakeCase converts correctly`() {
        assertEquals("pilih_lokasi", ProjectTypeDetector.toSnakeCase("Pilih Lokasi"))
        assertEquals("hello_world", ProjectTypeDetector.toSnakeCase("Hello World"))
        assertEquals("my_string_123", ProjectTypeDetector.toSnakeCase("My String 123"))
        assertEquals("already_snake", ProjectTypeDetector.toSnakeCase("already_snake"))
    }

    fun `test toSnakeCase handles special characters`() {
        assertEquals("hello_world", ProjectTypeDetector.toSnakeCase("Hello, World!"))
        assertEquals("test", ProjectTypeDetector.toSnakeCase("  test  "))
    }
}
