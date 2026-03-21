package com.alexius.extractstring.intention

import com.alexius.extractstring.detector.ProjectType
import com.alexius.extractstring.detector.ProjectTypeDetector
import com.alexius.extractstring.dialog.DiffDialogResult
import com.alexius.extractstring.dialog.DiffPreviewDialog
import com.alexius.extractstring.dialog.ExtractStringDialog
import com.alexius.extractstring.writer.StringResourceWriter
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.ImportPath
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

class ExtractKmpStringIntention : IntentionAction {

    override fun getText() = "Extract to string resource (KMP/Android)"
    override fun getFamilyName() = "Extract string resource"
    override fun startInWriteAction() = false

    override fun isAvailable(project: Project, editor: Editor, file: PsiFile): Boolean {
        if (!file.name.endsWith(".kt")) return false
        val element = file.findElementAt(editor.caretModel.offset) ?: return false
        val stringExpr = PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java)
            ?: return false
        // Only plain strings — no ${} interpolations
        if (stringExpr.entries.size != 1) return false
        return stringExpr.entries[0] is KtLiteralStringTemplateEntry
    }

    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        val element = file.findElementAt(editor.caretModel.offset) ?: return
        val stringExpr = PsiTreeUtil.getParentOfType(element, KtStringTemplateExpression::class.java)
            ?: return

        val rawValue = stringExpr.entries.firstOrNull()?.text ?: return
        val xmlSafeValue = rawValue
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

        val detector = ProjectTypeDetector()
        val detection = detector.detect(file)

        if (detection.type == ProjectType.UNKNOWN || detection.stringsFile == null) {
            Messages.showErrorDialog(
                project,
                "Could not find strings.xml in this module.\n" +
                    "Expected at composeResources/values/strings.xml or res/values/strings.xml.",
                "Extract String Resource"
            )
            return
        }

        val writer = StringResourceWriter()
        var keyName = promptForKey(project, rawValue, ProjectTypeDetector.toSnakeCase(rawValue))
            ?: return

        while (true) {
            val existingValue = writer.findEntry(detection.stringsFile, keyName)
            when {
                existingValue == null -> {
                    writer.writeEntry(project, detection.stringsFile, keyName, xmlSafeValue)
                    replaceInFile(project, file, stringExpr, keyName, detection.type)
                    return
                }
                existingValue == xmlSafeValue -> {
                    replaceInFile(project, file, stringExpr, keyName, detection.type)
                    return
                }
                else -> {
                    val diffDialog = DiffPreviewDialog(project, keyName, existingValue, xmlSafeValue)
                    diffDialog.show()
                    when (diffDialog.result) {
                        DiffDialogResult.USE_NEW -> {
                            writer.writeEntry(project, detection.stringsFile, keyName, xmlSafeValue)
                            replaceInFile(project, file, stringExpr, keyName, detection.type)
                            return
                        }
                        DiffDialogResult.CHOOSE_DIFFERENT_KEY -> {
                            keyName = promptForKey(project, rawValue, keyName) ?: return
                        }
                        DiffDialogResult.CANCEL -> return
                    }
                }
            }
        }
    }

    private fun promptForKey(project: Project, rawValue: String, initialKey: String): String? {
        val dialog = ExtractStringDialog(project, rawValue, initialKey)
        return if (dialog.showAndGet()) dialog.keyName else null
    }

    private fun replaceInFile(
        project: Project,
        file: PsiFile,
        stringExpr: KtStringTemplateExpression,
        key: String,
        projectType: ProjectType
    ) {
        val isComposable = isInsideComposable(stringExpr)
        val replacement = buildReplacement(key, projectType, isComposable)
        val stringResourceImport = buildImport(projectType, isComposable)
        val resImport = if (projectType == ProjectType.KMP) findResClassFqn(project) else null

        WriteCommandAction.runWriteCommandAction(project, "Extract String Resource", null, {
            stringExpr.replace(KtPsiFactory(project).createExpression(replacement))
            if (stringResourceImport != null) addImportIfMissing(file, stringResourceImport)
            if (resImport != null) addImportIfMissing(file, resImport)
        })
    }

    private fun buildReplacement(key: String, projectType: ProjectType, isComposable: Boolean): String =
        when {
            projectType == ProjectType.KMP && isComposable -> "stringResource(Res.string.$key)"
            projectType == ProjectType.KMP -> "Res.string.$key"
            isComposable -> "stringResource(R.string.$key)"
            else -> "R.string.$key"
        }

    private fun buildImport(projectType: ProjectType, isComposable: Boolean): String? =
        when {
            projectType == ProjectType.KMP && isComposable -> "org.jetbrains.compose.resources.stringResource"
            projectType == ProjectType.KMP -> null  // only Res import needed, handled by findResClassFqn
            isComposable -> "androidx.compose.ui.res.stringResource"
            else -> null  // R is auto-imported in Android projects
        }

    private fun isInsideComposable(element: PsiElement): Boolean {
        var current: PsiElement? = element.parent
        while (current != null) {
            if (current is KtNamedFunction) {
                return current.annotationEntries.any { it.shortName?.asString() == "Composable" }
            }
            current = current.parent
        }
        return false
    }

    /**
     * Scans all .kt files in the project for an existing import matching
     * "*.generated.resources.Res" and returns its FQN.
     * This works for any KMP module regardless of its package name.
     */
    private fun findResClassFqn(project: Project): String? {
        val scope = GlobalSearchScope.projectScope(project)
        val ktFiles = FilenameIndex.getAllFilesByExt(project, "kt", scope)
        for (vFile in ktFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(vFile) as? KtFile ?: continue
            val match = psiFile.importDirectives
                .mapNotNull { it.importedFqName?.asString() }
                .firstOrNull { it.endsWith(".generated.resources.Res") }
            if (match != null) return match
        }
        return null
    }

    private fun addImportIfMissing(file: PsiFile, importFqn: String) {
        val ktFile = file as? KtFile ?: return
        val exists = ktFile.importDirectives.any { it.importedFqName?.asString() == importFqn }
        if (!exists) {
            val factory = KtPsiFactory(file.project)
            val directive = factory.createImportDirective(ImportPath(FqName(importFqn), false))
            val importList = ktFile.importList
            if (importList != null) {
                importList.add(directive)
            } else {
                // No import list — add after the package directive or at the start of the file
                val anchor = ktFile.packageDirective ?: ktFile.firstChild
                if (anchor != null) {
                    ktFile.addAfter(directive, anchor)
                }
            }
        }
    }
}
