package com.attendify.android.ui.student

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.attendify.android.ui.theme.*
import com.attendify.shared.viewmodel.AuthViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentQRScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val user = authState.user
    val qrToken = user?.qrToken ?: ""

    val qrBitmap = remember(qrToken) {
        if (qrToken.isNotEmpty()) generateQrBitmap(qrToken) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My QR Code", color = OnDarkBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnDarkBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Show this QR to mark attendance",
                style = MaterialTheme.typography.bodyMedium,
                color = OnDarkSurface
            )

            Surface(
                modifier = Modifier.size(280.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.White
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "Your QR Code",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AttendifyPrimary)
                    }
                }
            }

            // Student info card
            user?.let { u ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(label = "Name", value = u.name)
                        InfoRow(label = "Class Roll No.", value = u.classRollNo)
                        InfoRow(label = "University Roll No.", value = u.universityRollNo)
                    }
                }
            }

            Text(
                text = "This QR is unique to you. Do not share it.",
                style = MaterialTheme.typography.bodySmall,
                color = ColorAbsent.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnDarkSurface)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = OnDarkBackground)
    }
}

fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}
