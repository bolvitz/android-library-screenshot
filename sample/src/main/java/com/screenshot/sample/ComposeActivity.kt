package com.screenshot.sample

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.screenshot.lib.ScreenshotBuilder
import com.screenshot.lib.ScreenshotCallback
import java.io.File

class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ComposeScreenshotTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScreenshotSampleScreen()
                }
            }
        }
    }
}

@Composable
fun ComposeScreenshotTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color.Black,
            secondary = Color.Black,
            background = Color.White,
            surface = Color.White,
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black
        ),
        content = content
    )
}

@Preview(name = "Theme Preview", showBackground = true)
@Composable
fun ThemePreview() {
    ComposeScreenshotTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Black & White Theme", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Button(onClick = { }) {
                Text("Sample Button")
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
            ) {
                Text("Sample Card", modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Preview(name = "Full Screen Preview", showBackground = true, showSystemUi = true)
@Composable
fun ScreenshotSampleScreenPreview() {
    ComposeScreenshotTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Screenshot Library - Compose Demo",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Text(
                        text = "This demo shows how to use the Screenshot Library with Jetpack Compose.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                Text(
                    text = "Sample Capturable Content",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                SampleCaptureableContent()

                Divider()

                Text(
                    text = "Capture Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Capture Compose Content")
                }

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Capture Full Screen")
                }

                CodeSnippetCard(
                    title = "Wrap Composables in AndroidView",
                    code = """
                        AndroidView(factory = { context ->
                            ComposeView(context).apply {
                                setContent {
                                    YourComposableContent()
                                }
                            }
                        })
                    """.trimIndent()
                )
            }
        }
    }
}

@Composable
fun ScreenshotSampleScreen() {
    val context = LocalContext.current
    var resultText by remember { mutableStateOf("") }
    var captureableView by remember { mutableStateOf<android.view.View?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        Text(
            text = "Screenshot Library - Compose Demo",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        // Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
        ) {
            Text(
                text = "This demo shows how to use the Screenshot Library with Jetpack Compose by wrapping composables in AndroidView.",
                modifier = Modifier.padding(16.dp),
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        // Sample Composable to Capture wrapped in AndroidView
        Text(
            text = "Sample Capturable Content",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                ComposeView(ctx).apply {
                    setContent {
                        ComposeScreenshotTheme {
                            SampleCaptureableContent()
                        }
                    }
                    captureableView = this
                }
            }
        )

        Divider()

        Text(
            text = "Capture Actions",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        // Capture button
        Button(
            onClick = {
                captureableView?.let { view ->
                    ScreenshotBuilder.quickCapture(
                        context = context,
                        view = view,
                        callback = object : ScreenshotCallback {
                            override fun onSuccess(file: File, bitmap: Bitmap?) {
                                resultText = "Screenshot saved: ${file.name}"
                                bitmap?.let {
                                    Toast.makeText(
                                        context,
                                        "Size: ${it.width}x${it.height}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            override fun onError(exception: Exception, message: String) {
                                resultText = "Error: $message"
                            }
                        }
                    )
                } ?: run {
                    resultText = "Error: View not ready"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Capture Compose Content")
        }

        // Capture entire screen
        Button(
            onClick = {
                val activity = context as? ComponentActivity
                activity?.window?.decorView?.rootView?.let { rootView ->
                    ScreenshotBuilder(context)
                        .view(rootView)
                        .format(Bitmap.CompressFormat.PNG)
                        .fileName("compose_fullscreen.png")
                        .saveToInternal()
                        .capture(
                            onSuccess = { file, bitmap ->
                                resultText = "Full screen captured: ${file.name}"
                                bitmap?.let {
                                    Toast.makeText(
                                        context,
                                        "Size: ${it.width}x${it.height}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onError = { _, message ->
                                resultText = "Error: $message"
                            }
                        )
                } ?: run {
                    resultText = "Error: Could not access root view"
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Capture Full Screen")
        }

        Divider()

        // Code Example
        Text(
            text = "How It Works",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        CodeSnippetCard(
            title = "Wrap Composables in AndroidView",
            code = """
                var captureableView: View? = null

                AndroidView(factory = { context ->
                    ComposeView(context).apply {
                        setContent {
                            YourComposableContent()
                        }
                        captureableView = this
                    }
                })

                // Then capture it
                ScreenshotBuilder.quickCapture(
                    context = context,
                    view = captureableView!!,
                    callback = object : ScreenshotCallback {
                        override fun onSuccess(file: File, bitmap: Bitmap?) {
                            // Handle success
                        }
                        override fun onError(exception: Exception, message: String) {
                            // Handle error
                        }
                    }
                )
            """.trimIndent()
        )

        CodeSnippetCard(
            title = "Alternative: Capture Full Screen",
            code = """
                val activity = context as? ComponentActivity
                val rootView = activity?.window?.decorView?.rootView

                ScreenshotBuilder(context)
                    .view(rootView!!)
                    .fileName("screenshot.png")
                    .capture(/* callbacks */)
            """.trimIndent()
        )

        // Result Display
        if (resultText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Text(
                    text = resultText,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SampleCaptureableContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            Text(
                text = "This Compose UI can be captured!",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF757575), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Box 1", color = Color.White, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF424242), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Box 2", color = Color.White, fontSize = 12.sp)
                }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color(0xFF212121), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Box 3", color = Color.White, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Compose makes it easy to build beautiful UIs, and with this library you can capture them as screenshots!",
                fontSize = 12.sp,
                color = Color(0xFF616161)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CodeSnippetCard(
    title: String = "Sample Code",
    code: String = """
        ScreenshotBuilder(context)
            .view(view)
            .fileName("screenshot.png")
            .capture(/* callbacks */)
    """.trimIndent()
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = code,
                    fontSize = 11.sp,
                    color = Color(0xFF424242),
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
