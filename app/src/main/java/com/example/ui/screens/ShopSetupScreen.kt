package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.MainViewModel
import com.example.ui.theme.*

@Composable
fun ShopSetupScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val existingProfile by viewModel.shopProfile.collectAsState()

    var ownerName by remember(existingProfile) { mutableStateOf(existingProfile?.ownerName ?: "") }
    var shopName by remember(existingProfile) { mutableStateOf(existingProfile?.shopName ?: "محل ابوطلال لتجارة الخردة والسكراب") }
    var phone by remember(existingProfile) { mutableStateOf(existingProfile?.phoneNumber ?: "778215553") }
    var address by remember(existingProfile) { mutableStateOf(existingProfile?.address ?: "صنعاء") }

    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Icon & Title
            Surface(
                shape = CircleShape,
                color = BluePrimary,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "تسجيل بيانات المحل لأول مرة",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "يرجى تعبئة بيانات محل الخردة للبدء في استخدام النظام المحاسبي",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
            )

            // Setup Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Owner Name (الاسم)
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("الاسم (اسم صاحب المحل)") },
                        placeholder = { Text("مثال: عبدالحميد داوؤد") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_owner_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            focusedLabelColor = BluePrimary
                        )
                    )

                    // Shop Name (اسم المحل)
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("اسم المحل") },
                        placeholder = { Text("مثال: محل ابوطلال لتجارة الخردة") },
                        leadingIcon = {
                            Icon(Icons.Default.Store, contentDescription = null, tint = BluePrimary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_shop_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            focusedLabelColor = BluePrimary
                        )
                    )

                    // Phone (رقم الهاتف)
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الهاتف") },
                        placeholder = { Text("مثال: 778215553") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = BluePrimary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_phone_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            focusedLabelColor = BluePrimary
                        )
                    )

                    // Address (العنوان)
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان") },
                        placeholder = { Text("مثال: صنعاء - شارع الستين") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = BluePrimary)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_address_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BluePrimary,
                            focusedLabelColor = BluePrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Enter Button (دخول)
                    Button(
                        onClick = {
                            if (shopName.isBlank()) {
                                viewModel.showMessage("يرجى كتابة اسم المحل")
                            } else {
                                viewModel.saveProfile(
                                    ownerName = ownerName.ifBlank { "مالك المحل" },
                                    shopName = shopName,
                                    phone = phone,
                                    address = address
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("setup_submit_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text(
                            text = "دخول",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "ملاحظة: يمكنك تعديل جميع هذه البيانات لاحقاً من قسم الإعدادات",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
