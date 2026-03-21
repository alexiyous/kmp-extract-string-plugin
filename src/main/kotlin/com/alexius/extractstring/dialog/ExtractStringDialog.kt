package com.alexius.extractstring.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class ExtractStringDialog(
    project: Project,
    private val stringValue: String,
    initialKey: String
) : DialogWrapper(project) {

    private val keyField = JTextField(initialKey, 40)

    val keyName: String get() = keyField.text.trim()

    init {
        title = "Extract String Resource"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            fill = GridBagConstraints.HORIZONTAL
        }

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2
        panel.add(
            JLabel("String value: \"${stringValue.take(60)}${if (stringValue.length > 60) "…" else ""}\""),
            gbc
        )

        gbc.gridwidth = 1
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.0
        panel.add(JLabel("Resource key:"), gbc)

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 1.0
        panel.add(keyField, gbc)

        panel.preferredSize = Dimension(420, 80)
        return panel
    }

    override fun doValidate(): ValidationInfo? {
        val key = keyField.text.trim()
        if (key.isEmpty()) return ValidationInfo("Key name cannot be empty", keyField)
        if (!key.matches(Regex("[a-z][a-z0-9_]*"))) {
            return ValidationInfo(
                "Key must be lowercase, start with a letter, use only a-z, 0-9, _",
                keyField
            )
        }
        return null
    }

    override fun getPreferredFocusedComponent(): JComponent = keyField
}
