package com.alexius.extractstring.detector

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile

enum class ProjectType { KMP, ANDROID, UNKNOWN }

data class DetectionResult(
    val type: ProjectType,
    val stringsFile: VirtualFile?,
    val stringsFilePath: String?
)

class ProjectTypeDetector {

    fun detect(file: PsiFile): DetectionResult {
        val virtualFile = file.virtualFile ?: return unknown()
        val module = ModuleUtilCore.findModuleForFile(virtualFile, file.project)
            ?: return unknown()
        val roots = ModuleRootManager.getInstance(module).contentRoots

        var kmpStrings: VirtualFile? = null
        var androidStrings: VirtualFile? = null

        for (root in roots) {
            kmpStrings = kmpStrings ?: findFile(root, "composeResources/values/strings.xml")
            androidStrings = androidStrings ?: findFile(root, "res/values/strings.xml")
        }

        return when {
            kmpStrings != null -> DetectionResult(ProjectType.KMP, kmpStrings, kmpStrings.path)
            androidStrings != null -> DetectionResult(ProjectType.ANDROID, androidStrings, androidStrings.path)
            else -> unknown()
        }
    }

    private fun findFile(root: VirtualFile, relativePath: String): VirtualFile? {
        var current: VirtualFile? = root
        for (segment in relativePath.split("/")) {
            current = current?.findChild(segment) ?: return null
        }
        return current
    }

    private fun unknown() = DetectionResult(ProjectType.UNKNOWN, null, null)

    companion object {
        fun toSnakeCase(input: String): String =
            input.trim()
                .lowercase()
                .replace(Regex("[^a-z0-9]+"), "_")
                .trim('_')
    }
}
