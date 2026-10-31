package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.components.AbuTalalTopBar
import com.example.ui.components.PhoneContactRow
import com.example.ui.components.openPhoneCall
import com.example.ui.components.openWhatsApp
import com.example.ui.theme.*

@Composable
fun DeveloperInfoScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = "حقوق المطور والبرمجة",
                onBack = { viewModel.navigateBack() },
                showDeveloperAction = false
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = BluePrimary,
                modifier = Modifier.size(90.dp),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            Text(
                text = "المبرمج والمطور",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            Text(
                text = "عبدالحميد داوؤد",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = BluePrimary,
                textAlign = TextAlign.Center
            )

            Surface(
                color = RedAccent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "تطبيق ابوطلال سوفت لإدارة الخردة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

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
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "بيانات التواصل المباشر (اتصال وواتساب):",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )

                    PhoneContactRow(
                        phoneNumber = "778215553",
                        onCall = { openPhoneCall(context, "778215553") },
                        onWhatsApp = { openWhatsApp(context, "778215553") }
                    )

                    PhoneContactRow(
                        phoneNumber = "700897775",
                        onCall = { openPhoneCall(context, "700897775") },
                        onWhatsApp = { openWhatsApp(context, "700897775") }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceVariantLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "عن النظام:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                    Text(
                        text = "نظام ابوطلال سوفت تم تصميمه خصيصاً لتلبية طبيعة تجارة الخردة والسكراب وتتبع الأوزان المتعددة وتسعيرات الشراء والبيع اليومية، وحساب ديون العملاء والموردين بكل دقة وسلاسة وبدون أي بيانات تجريبية.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }
        }
    }
}
