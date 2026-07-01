package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.compose.runtime.collectAsState
import com.example.ui.ActiveScreen
import com.example.ui.PresentationViewModel
import com.example.ui.screens.AboutDeveloperScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SlideshowScreen
import com.example.ui.screens.TemplateScreen
import com.example.ui.theme.MyApplicationTheme
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: PresentationViewModel

    // In-memory holder for json string to write on export callback
    private var pendingExportJson: String? = null

    // Register Create Document Launcher (Local Storage Export JSON)
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportJson != null) {
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(pendingExportJson!!.toByteArray())
                    Toast.makeText(this, "Presentation saved successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save presentation file.", Toast.LENGTH_SHORT).show()
            } finally {
                pendingExportJson = null
            }
        }
    }

    // Register Open Document Launcher (Local Storage Import JSON)
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val stringBuilder = StringBuilder()
                    var line: String? = reader.readLine()
                    while (line != null) {
                        stringBuilder.append(line)
                        line = reader.readLine()
                    }
                    val jsonContent = stringBuilder.toString()
                    viewModel.importPresentationFromJson(jsonContent)
                    Toast.makeText(this, "Presentation imported successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to parse presentation file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Instantiate AndroidViewModel
        viewModel = ViewModelProvider(this)[PresentationViewModel::class.java]

        // Bind SAF triggers
        viewModel.requestExportJson = { filename, json ->
            pendingExportJson = json
            createDocumentLauncher.launch(filename)
        }

        viewModel.requestImportJson = {
            openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
        }

        setContent {
            MyApplicationTheme {
                val screenState = viewModel.activeScreen.collectAsState().value

                when (screenState) {
                    is ActiveScreen.Home -> {
                        HomeScreen(viewModel = viewModel)
                    }
                    is ActiveScreen.TemplateSelection -> {
                        TemplateScreen(viewModel = viewModel)
                    }
                    is ActiveScreen.Editor -> {
                        EditorScreen(viewModel = viewModel)
                    }
                    is ActiveScreen.Slideshow -> {
                        SlideshowScreen(viewModel = viewModel, startIndex = screenState.startIndex)
                    }
                    is ActiveScreen.AboutDeveloper -> {
                        AboutDeveloperScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
