package com.alexius.extractstring.writer

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile

class StringResourceWriter {

    /** Returns the existing value for [key], or null if not found. */
    fun findEntry(stringsFile: VirtualFile, key: String): String? {
        val content = String(stringsFile.contentsToByteArray())
        val regex = Regex("""<string\s+name="${Regex.escape(key)}"\s*>(.*?)</string>""", RegexOption.DOT_MATCHES_ALL)
        return regex.find(content)?.groupValues?.get(1)?.trim()
    }

    /** Writes or overwrites a <string name="key">value</string> entry. */
    fun writeEntry(project: Project, stringsFile: VirtualFile, key: String, value: String) {
        WriteCommandAction.runWriteCommandAction(project, "Extract String Resource", null, {
            val psiManager = PsiManager.getInstance(project)
            val xmlFile = psiManager.findFile(stringsFile) as? XmlFile ?: run {
                writeRaw(stringsFile, key, value)
                return@runWriteCommandAction
            }
            val resources = xmlFile.rootTag ?: run {
                writeRaw(stringsFile, key, value)
                return@runWriteCommandAction
            }

            // Remove existing entry with this key if present
            resources.subTags
                .filter { it.name == "string" && it.getAttributeValue("name") == key }
                .forEach { it.delete() }

            // Add new entry
            val newTag = resources.createChildTag("string", "", value, false)
            newTag.setAttribute("name", key)
            resources.addSubTag(newTag, false)
        })
    }

    private fun writeRaw(stringsFile: VirtualFile, key: String, value: String) {
        WriteAction.run<Exception> {
            val current = String(stringsFile.contentsToByteArray())
            val entry = """    <string name="$key">$value</string>"""
            val updated = if (current.contains("</resources>")) {
                current.replace("</resources>", "$entry\n</resources>")
            } else {
                throw IllegalStateException("strings.xml at ${stringsFile.path} has no </resources> closing tag — cannot write entry.")
            }
            stringsFile.setBinaryContent(updated.toByteArray())
        }
    }
}
