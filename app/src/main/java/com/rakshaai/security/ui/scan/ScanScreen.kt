package com.rakshaai.security.ui.scan

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.rakshaai.security.R
import com.rakshaai.security.data.model.ContentType
import com.rakshaai.security.data.repository.ScanRepository
import com.rakshaai.security.domain.ocr.OcrHelper
import com.rakshaai.security.ui.components.OfflineStatusBadge
import com.rakshaai.security.ui.theme.BorderSubtle
import com.rakshaai.security.ui.theme.BrandTeal
import com.rakshaai.security.ui.theme.BrandTealLight
import com.rakshaai.security.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ScanScreen(
    initialContentType: ContentType = ContentType.MESSAGE,
    onStartScan: (ContentType, String, String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current

    var selectedType by remember { mutableStateOf(initialContentType) }
    var inputText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isExtractingOcr by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for Gallery Picker
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            coroutineScope.launch {
                isExtractingOcr = true
                inputText = OcrHelper.extractTextFromImage(context, uri)
                isExtractingOcr = false
            }
        }
    }

    // Launcher for Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            val uri = tempCameraUri!!
            selectedImageUri = uri
            coroutineScope.launch {
                isExtractingOcr = true
                inputText = OcrHelper.extractTextFromImage(context, uri)
                isExtractingOcr = false
            }
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val photoUri = createTempImageUri(context)
            tempCameraUri = photoUri
            cameraLauncher.launch(photoUri)
        }
    }

    LaunchedEffect(initialContentType) {
        selectedType = initialContentType
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.scan_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = stringResource(R.string.scan_subtitle),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                )
            }
            OfflineStatusBadge()
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 2x2 Two-Row Scan Tabs Layout
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ROW 1: Message | Link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScanTabCard(
                    title = stringResource(R.string.scan_tab_message),
                    icon = Icons.Default.Message,
                    isSelected = selectedType == ContentType.MESSAGE,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedType = ContentType.MESSAGE
                        selectedImageUri = null
                    }
                )

                ScanTabCard(
                    title = stringResource(R.string.scan_tab_link),
                    icon = Icons.Default.Link,
                    isSelected = selectedType == ContentType.URL,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedType = ContentType.URL
                        selectedImageUri = null
                    }
                )
            }

            // ROW 2: Screenshot | Voice/Photo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ScanTabCard(
                    title = stringResource(R.string.scan_tab_screenshot),
                    icon = Icons.Default.Image,
                    isSelected = selectedType == ContentType.SCREENSHOT,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedType = ContentType.SCREENSHOT
                    }
                )

                ScanTabCard(
                    title = stringResource(R.string.scan_tab_voice),
                    icon = Icons.Default.CameraAlt,
                    isSelected = selectedType == ContentType.PHOTO,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedType = ContentType.PHOTO
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        // Screenshot & Photo Image Selection Actions
        if (selectedType == ContentType.SCREENSHOT || selectedType == ContentType.PHOTO) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BrandTeal)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_choose_screenshot),
                                color = BrandTeal,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        if (selectedType == ContentType.PHOTO) {
                            Button(
                                onClick = {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandTeal)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.btn_take_photo),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    if (selectedImageUri != null) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = "Preview",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }

        // Main Input Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (selectedType) {
                            ContentType.MESSAGE -> stringResource(R.string.scan_tab_message)
                            ContentType.URL -> stringResource(R.string.scan_tab_link)
                            ContentType.SCREENSHOT -> stringResource(R.string.scan_tab_screenshot)
                            ContentType.PHOTO -> stringResource(R.string.scan_tab_voice)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 15.sp
                        )
                    )

                    Row {
                        if (selectedType == ContentType.MESSAGE || selectedType == ContentType.URL) {
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()
                                    if (clip != null) {
                                        inputText = clip.text
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = stringResource(R.string.btn_paste),
                                    tint = BrandTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (inputText.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    inputText = ""
                                    selectedImageUri = null
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.btn_clear),
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isExtractingOcr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = BrandTeal,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Extracting text on device...",
                                style = MaterialTheme.typography.bodyMedium.copy(color = BrandTeal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                } else {
                    val placeholderText = when (selectedType) {
                        ContentType.MESSAGE -> stringResource(R.string.scan_placeholder_message)
                        ContentType.URL -> stringResource(R.string.scan_placeholder_link)
                        ContentType.SCREENSHOT -> stringResource(R.string.scan_placeholder_screenshot)
                        ContentType.PHOTO -> stringResource(R.string.scan_placeholder_photo)
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = placeholderText,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextSecondary.copy(alpha = 0.7f),
                                    fontSize = 15.sp
                                )
                            )
                        },
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (selectedType == ContentType.URL) 100.dp else 150.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandTeal,
                            unfocusedBorderColor = BorderSubtle,
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                }

                // Quick Examples for Message Scan
                if (selectedType == ContentType.MESSAGE) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Quick examples",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SampleChip(label = "Gujarati KYC") {
                            inputText = ScanRepository.presetSamples[0].text
                        }
                        SampleChip(label = "Hindi UPI") {
                            inputText = ScanRepository.presetSamples[1].text
                        }
                        SampleChip(label = "Electricity") {
                            inputText = ScanRepository.presetSamples[2].text
                        }
                        SampleChip(label = "Prize Scam") {
                            inputText = ScanRepository.presetSamples[3].text
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Primary Check Message Button
        Button(
            onClick = {
                if (inputText.isNotBlank()) {
                    onStartScan(selectedType, inputText, selectedImageUri?.toString())
                }
            },
            enabled = inputText.isNotBlank() && !isExtractingOcr,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandTeal,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.btn_analyze_offline),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ScanTabCard(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) BrandTeal else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isSelected) BrandTeal else BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) Color.White else BrandTeal,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp
                )
            )
        }
    }
}

@Composable
fun SampleChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .background(BrandTealLight.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                color = BrandTeal,
                fontSize = 12.sp
            )
        )
    }
}

private fun createTempImageUri(context: Context): Uri {
    val tempFile = File.createTempFile("camera_photo_", ".jpg", context.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        tempFile
    )
}
