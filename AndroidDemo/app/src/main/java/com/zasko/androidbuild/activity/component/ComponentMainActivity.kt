package com.zasko.androidbuild.activity.component

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


class ComponentMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MessageCard("android")
        }
    }

    @Composable
    fun MessageCard(name: String) {
        Box(
            modifier = Modifier
                .wrapContentWidth()
                .height(100.dp)
        ) {
            Text(name, Modifier.padding(10.dp))
            Icon(Icons.Filled.Check, contentDescription = "")
        }

    }


    @Preview(showBackground = true)
    @Composable
    fun PreviewMessageCard() {
        val  context = this
        MessageCard("Android")
    }
}
