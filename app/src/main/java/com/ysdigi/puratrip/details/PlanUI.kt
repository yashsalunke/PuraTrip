package com.ysdigi.puratrip.details

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class WebAppInterface(private val onStylesChanged: (Set<String>) -> Unit) {
    @JavascriptInterface
    fun updateStyle(styles: String) {
        // styles will be a comma separated string e.g., "bold,italic"
        onStylesChanged(styles.split(',').filter { it.isNotBlank() }.toSet())
    }
}

@Composable
fun PlanScreen(
    plan: String,
    onPlanChanged: (String) -> Unit,
    isEditMode: Boolean,
    onEditModeChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    var showLinkDialog by remember { mutableStateOf(false) }
    var activeStyles by remember { mutableStateOf<Set<String>>(emptySet()) }
    var displayedPlan by remember { mutableStateOf(plan) }

    val webView = remember(context) {
        WebView(context).apply {
            settings.javaScriptEnabled = true
        }
    }

    LaunchedEffect(plan) {
        displayedPlan = plan
    }

    LaunchedEffect(isEditMode) {
        if (isEditMode) {
            val escapedPlan = displayedPlan
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
            webView.evaluateJavascript("setContent(\"$escapedPlan\");", null)
            webView.post {
                webView.requestFocus()
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
                webView.evaluateJavascript("focusEditor();", null)
            }
        }
    }

    if (isEditMode) {
        Column(modifier = Modifier.padding(16.dp)) {
            // --- Toolbar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val boldModifier = if (activeStyles.contains("bold")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier
                val italicModifier = if (activeStyles.contains("italic")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier
                val underlineModifier = if (activeStyles.contains("underline")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier
                val highlightModifier = if (activeStyles.contains("highlight")) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier

                IconButton(onClick = { webView.evaluateJavascript("execCmd('bold');", null) }, modifier = boldModifier) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                }
                IconButton(onClick = { webView.evaluateJavascript("execCmd('italic');", null) }, modifier = italicModifier) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                }
                IconButton(onClick = { webView.evaluateJavascript("execCmd('underline');", null) }, modifier = underlineModifier) {
                    Icon(Icons.Default.FormatUnderlined, contentDescription = "Underline")
                }
                IconButton(
                    onClick = {
                        val command = if (activeStyles.contains("highlight")) {
                            "execCmd('hiliteColor', 'transparent');"
                        } else {
                            "execCmd('hiliteColor', 'yellow');"
                        }
                        webView.evaluateJavascript(command, null)
                    },
                    modifier = highlightModifier
                ) {
                    Icon(Icons.Default.Highlight, contentDescription = "Highlight")
                }
                IconButton(onClick = { showLinkDialog = true }) {
                    Icon(Icons.Default.Link, contentDescription = "Hyperlink")
                }

                var expanded by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { expanded = true }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Font Size")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        (1..7).forEach { size ->
                            DropdownMenuItem(
                                text = { Text("Size $size") },
                                onClick = {
                                    webView.evaluateJavascript("execCmd('fontSize', '$size');", null)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                IconButton(onClick = { webView.evaluateJavascript("execCmd('removeFormat');", null) }) {
                    Icon(Icons.Default.FormatClear, contentDescription = "Clear Formatting")
                }
            }

            // --- WebView editor ---
            AndroidView(
                factory = {
                    webView.apply {
                        addJavascriptInterface(WebAppInterface { activeStyles = it }, "Android")

                        webViewClient = object : WebViewClient() {
                            @Deprecated("shouldOverrideUrlLoading is deprecated.")
                            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                                if (url != null) {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                    return true
                                }
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val escapedPlan = displayedPlan
                                    .replace("\"", "\\\"")
                                    .replace("\n", "\\n")
                                view?.evaluateJavascript("setContent(\"$escapedPlan\");", null)
                            }
                        }

                        val editorHtml = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
                            <style>
                                body { margin: 0; padding: 0; font-family: sans-serif; }
                                #editor {
                                    height: 100vh;
                                    padding: 8px;
                                    outline: none;
                                }
                            </style>
                        </head>
                        <body>
                            <div id="editor" contenteditable="true"></div>
                            <script>
                                var editor = document.getElementById('editor');

                                document.addEventListener('selectionchange', function() {
                                    var styles = [];
                                    if (document.queryCommandState('bold')) styles.push('bold');
                                    if (document.queryCommandState('italic')) styles.push('italic');
                                    if (document.queryCommandState('underline')) styles.push('underline');

                                    var highlightColor = document.queryCommandValue('hiliteColor');
                                    if (highlightColor && highlightColor.toLowerCase() !== 'transparent' && highlightColor !== 'rgba(0, 0, 0, 0)') {
                                        styles.push('highlight');
                                    }
                                    Android.updateStyle(styles.join(','));
                                });

                                function execCmd(command, value) {
                                    document.execCommand(command, false, value || null);
                                }

                                function setContent(newContent) {
                                    if (editor.innerHTML !== newContent) {
                                        editor.innerHTML = newContent;
                                    }
                                }

                                function getContent() {
                                    return editor.innerHTML;
                                }

                                function focusEditor() {
                                    editor.focus();
                                    var range = document.createRange();
                                    range.selectNodeContents(editor);
                                    range.collapse(false);
                                    var sel = window.getSelection();
                                    sel.removeAllRanges();
                                    sel.addRange(range);
                                }
                            </script>
                        </body>
                        </html>
                    """.trimIndent()

                        loadDataWithBaseURL(null, editorHtml, "text/html", "UTF-8", null)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    webView.evaluateJavascript("getContent()") { html ->
                        val cleanedHtml = html
                            ?.removePrefix("\"")
                            ?.removeSuffix("\"")
                            ?.replace("\\u003C", "<")
                            ?.replace("\\\"", "\"")
                            ?.replace("\\n", "")
                            ?.replace("\\r", "")
                            ?.replace("\\\'", "'")
                        if (cleanedHtml != null) {
                            displayedPlan = cleanedHtml
                            onPlanChanged(cleanedHtml)
                        }
                        onEditModeChanged(false)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Plan")
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .clickable { onEditModeChanged(true) }
        ) {
            AndroidView(
                factory = {
                    TextView(it).apply {
                        text = android.text.Html.fromHtml(displayedPlan, android.text.Html.FROM_HTML_MODE_COMPACT)
                        movementMethod = android.text.method.LinkMovementMethod.getInstance()
                    }
                },
                update = {
                    it.text = android.text.Html.fromHtml(displayedPlan, android.text.Html.FROM_HTML_MODE_COMPACT)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // --- Add Link Dialog ---
    if (showLinkDialog) {
        var url by remember { mutableStateOf("https://") }
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Add Hyperlink") },
            text = {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    webView.evaluateJavascript("execCmd('createLink', '$url');", null)
                    showLinkDialog = false
                }) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("Cancel") }
            }
        )
    }
}
