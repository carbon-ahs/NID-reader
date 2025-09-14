package com.example.nidreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.nidreader.ui.theme.NIDReaderTheme
import androidx.compose.material3.Button
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NIDReaderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}


@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    var showCamera by remember { mutableStateOf(false) }

    if (showCamera) {
        CameraScreenWithBox(onBack = { showCamera = false })
    } else {
        Column {
            Text(
                text = "Hello $name!",
                modifier = modifier
            )
            Button(onClick = {
                showCamera = true
            }) {
                Text("Open Camera")
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NIDReaderTheme {
        Box {
            Greeting("Nid Reader")
        }
    }
}