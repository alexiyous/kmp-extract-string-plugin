package com.alexius.extractstring.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.ActionEvent
import javax.swing.*

enum class DiffDialogResult { USE_NEW, CHOOSE_DIFFERENT_KEY, CANCEL }

class DiffPreviewDialog(
    project: Project,
    private val key: String,
    private val existingValue: String,
    private val newValue: String
) : DialogWrapper(project) {

    var result: DiffDialogResult = DiffDialogResult.CANCEL
        private set

    init {
        title = "Key Already Exists"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(6, 8, 6, 8)
            fill = GridBagConstraints.HORIZONTAL
            gridx = 0; weightx = 1.0
        }

        gbc.gridy = 0
        panel.add(JLabel("<html>Key <b>\"$key\"</b> already exists with a different value:</html>"), gbc)

        gbc.gridy = 1
        panel.add(JLabel("Existing value:"), gbc)

        gbc.gridy = 2
        val existingArea = JTextArea(existingValue, 2, 40).apply {
            isEditable = false; lineWrap = true; wrapStyleWord = true
        }
        panel.add(JScrollPane(existingArea), gbc)

        gbc.gridy = 3
        panel.add(JLabel("New value:"), gbc)

        gbc.gridy = 4
        val newArea = JTextArea(newValue, 2, 40).apply {
            isEditable = false; lineWrap = true; wrapStyleWord = true
        }
        panel.add(JScrollPane(newArea), gbc)

        panel.preferredSize = Dimension(480, 220)
        return panel
    }

    override fun createActions(): Array<Action> {
        val useNew = object : DialogWrapperAction("Use new value") {
            override fun doAction(e: ActionEvent?) {
                result = DiffDialogResult.USE_NEW
                close(OK_EXIT_CODE)
            }
        }
        val chooseDifferent = object : DialogWrapperAction("Choose different key") {
            override fun doAction(e: ActionEvent?) {
                result = DiffDialogResult.CHOOSE_DIFFERENT_KEY
                close(CANCEL_EXIT_CODE)
            }
        }
        val cancel = object : DialogWrapperAction("Cancel") {
            override fun doAction(e: ActionEvent?) {
                result = DiffDialogResult.CANCEL
                close(CANCEL_EXIT_CODE)
            }
        }
        return arrayOf(useNew, chooseDifferent, cancel)
    }
}
