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
    initialKey: String,
    private val formatPreview: String? = null
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
        var row = 0

        // String value preview
        gbc.gridx = 0; gbc.gridy = row++; gbc.gridwidth = 2; gbc.weightx = 1.0
        panel.add(
            JLabel("String value: \"${stringValue.take(60)}${if (stringValue.length > 60) "…" else ""}\""),
            gbc
        )

        // Format string preview — only shown for parameterized strings
        if (formatPreview != null) {
            gbc.gridy = row++
            panel.add(
                JLabel("Format: \"${formatPreview.take(80)}${if (formatPreview.length > 80) "…" else ""}\""),
                gbc
            )
        }

        // Key name field
        gbc.gridwidth = 1; gbc.weightx = 0.0
        gbc.gridx = 0; gbc.gridy = row
        panel.add(JLabel("Resource key:"), gbc)
        gbc.gridx = 1; gbc.weightx = 1.0
        panel.add(keyField, gbc)

        panel.preferredSize = Dimension(480, if (formatPreview != null) 110 else 80)
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
