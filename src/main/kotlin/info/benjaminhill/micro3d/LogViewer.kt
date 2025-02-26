package info.benjaminhill.micro3d

import java.awt.BorderLayout
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.SwingUtilities

object LogViewer : AutoCloseable {

    private val textArea = JTextArea()
    private var frame: JFrame? = null

    init {
        SwingUtilities.invokeLater {
            frame = JFrame("Log Viewer")
            val scrollPane = JScrollPane(textArea)

            textArea.isEditable = false // Make it read-only
            frame!!.contentPane.add(scrollPane, BorderLayout.CENTER)
            frame!!.setSize(600, 400)
            frame!!.setLocationRelativeTo(null) // Center on screen
            frame!!.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            frame!!.isVisible = true

            // Simulate adding log messages
            appendLog("Application started.")
        }
    }

     fun appendLog(message: String) {
        SwingUtilities.invokeLater {
            textArea.append("$message\n")
        }
    }

    fun appendRaw(message: String) {
        SwingUtilities.invokeLater {
            textArea.append(message)
        }
    }

    override fun close() {
        SwingUtilities.invokeLater {
            frame?.dispose() // Dispose of the JFrame
            frame = null // Clear the reference
        }
    }
}

