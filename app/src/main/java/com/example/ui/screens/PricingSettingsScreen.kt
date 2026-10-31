package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.DailyPriceEntity
import com.example.ui.MainViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PricingSettingsScreen(
    priceType: String, // "PURCHASE" or "SALE"
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val isPurchase = priceType == "PURCHASE"
    val sectionSubtitle = if (isPurchase) "أسعار مشتريات الخردة اليومية" else "أسعار مبيعات الخردة اليومية"

    val shopProfile by viewModel.shopProfile.collectAsState()
    val currency = shopProfile?.currencySymbol ?: "ر.ي"

    val activePrice by if (isPurchase) viewModel.activePurchasePrice.collectAsState() else viewModel.activeSalePrice.collectAsState()
    val allPrices by if (isPurchase) viewModel.allPurchasePrices.collectAsState() else viewModel.allSalePrices.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AbuTalalTopBar(
                title = "ابوطلال سوفت",
                subtitle = sectionSubtitle,
                onBack = { viewModel.navigateBack() }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("إضافة تسعيرة جديدة", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("add_pricing_fab")
            )
        },
        containerColor = BackgroundLight
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isPurchase) "التسعيرة النشطة المعتمدة في الشراء" else "التسعيرة النشطة المعتمدة في البيع",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BluePrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (activePrice != null) GreenLight else RedLight
                            ) {
                                Text(
                                    text = if (activePrice != null) "نشطة ومعتمدة" else "غير محددة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activePrice != null) GreenSuccess else RedAccent,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (activePrice != null && activePrice!!.dateString.isNotBlank()) {
                            Text(
                                text = "${activePrice!!.dayName} - ${activePrice!!.dateString}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                            )
                            PricingGridPreview(price = activePrice!!, currency = currency)
                        } else {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "لا توجد تسعيرة نشطة حالياً. اضغط على زر (+ إضافة تسعيرة جديدة) بالأسفل لإدخال أسعار الكيلو الحقيقية لليوم.",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "سجل وأرشيف التسعيرات السابقة (${allPrices.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (allPrices.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("لا توجد تسعيرات سابقة محفوظة", color = TextSecondary)
                    }
                }
            } else {
                items(allPrices) { priceItem ->
                    PricingArchiveCard(
                        price = priceItem,
                        currency = currency,
                        isCurrentActive = priceItem.isActive,
                        onAdopt = { viewModel.adoptPricing(priceItem.id, priceType) },
                        onShare = {
                            val shareContent = buildString {
                                appendLine("📋 ابوطلال سوفت - ${if (isPurchase) "أسعار مشتريات الخردة" else "أسعار مبيعات الخردة"}")
                                appendLine("اليوم: ${priceItem.dayName} | التاريخ: ${priceItem.dateString}")
                                appendLine("--------------------------------")
                                appendLine("النحاس الأحمر: ${formatCurrency(priceItem.copperRedPrice, currency)}/كجم")
                                appendLine("النحاس الأصفر: ${formatCurrency(priceItem.copperYellowPrice, currency)}/كجم")
                                appendLine("المعدن الخفيف: ${formatCurrency(priceItem.metalLightPrice, currency)}/كجم")
                                appendLine("المعدن الثقيل: ${formatCurrency(priceItem.metalHeavyPrice, currency)}/كجم")
                                appendLine("الألمنيوم: ${formatCurrency(priceItem.aluminumPrice, currency)}/كجم")
                                appendLine("البطاريات: ${formatCurrency(priceItem.batteryPrice, currency)}/كجم")
                                appendLine("الحديد: ${formatCurrency(priceItem.ironPrice, currency)}/كجم")
                                appendLine("البلاستيك: ${formatCurrency(priceItem.plasticPrice, currency)}/كجم")
                                appendLine("--------------------------------")
                                appendLine("ابوطلال سوفت لإدارة الخردة")
                            }
                            shareText(context, "تسعيرة الخردة", shareContent)
                        },
                        onDelete = { viewModel.deleteDailyPrice(priceItem) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddDailyPriceDialog(
            priceType = priceType,
            initialActive = activePrice,
            currency = currency,
            onDismiss = { showAddDialog = false },
            onConfirm = { day, date, red, yellow, mLight, mHeavy, alu, bat, iron, plastic, setActive ->
                viewModel.saveNewDailyPricing(
                    type = priceType,
                    dateStr = date,
                    dayName = day,
                    copperRed = red,
                    copperYellow = yellow,
                    metalLight = mLight,
                    metalHeavy = mHeavy,
                    aluminum = alu,
                    battery = bat,
                    iron = iron,
                    plastic = plastic,
                    setActive = setActive
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun PricingGridPreview(price: DailyPriceEntity, currency: String) {
    val items = listOf(
        Triple("النحاس الأحمر", price.copperRedPrice, CopperRed),
        Triple("النحاس الأصفر", price.copperYellowPrice, CopperYellow),
        Triple("المعدن الخفيف", price.metalLightPrice, MetalLight),
        Triple("المعدن الثقيل", price.metalHeavyPrice, MetalHeavy),
        Triple("الألمنيوم", price.aluminumPrice, AluminumColor),
        Triple("البطاريات", price.batteryPrice, BatteryColor),
        Triple("الحديد", price.ironPrice, IronColor),
        Triple("البلاستيك", price.plasticPrice, PlasticColor)
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in items.indices step 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MiniPriceBadge(name = items[i].first, price = items[i].second, color = items[i].third, currency = currency, modifier = Modifier.weight(1f))
                if (i + 1 < items.size) {
                    MiniPriceBadge(name = items[i + 1].first, price = items[i + 1].second, color = items[i + 1].third, currency = currency, modifier = Modifier.weight(1f))
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun MiniPriceBadge(
    name: String,
    price: Double,
    color: Color,
    currency: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceVariantLight
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(text = formatCurrency(price, currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = BluePrimary)
        }
    }
}

@Composable
fun PricingArchiveCard(
    price: DailyPriceEntity,
    currency: String,
    isCurrentActive: Boolean,
    onAdopt: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isCurrentActive) androidx.compose.foundation.BorderStroke(1.5.dp, GreenSuccess) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${price.dayName} - ${price.dateString}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = BluePrimary
                    )
                }

                if (isCurrentActive) {
                    Surface(shape = RoundedCornerShape(6.dp), color = GreenLight) {
                        Text(
                            text = "معتمدة حالياً",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenSuccess,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PricingGridPreview(price = price, currency = currency)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCurrentActive) {
                    Button(
                        onClick = onAdopt,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اعتماد التسعيرة اليوم", fontSize = 12.sp)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = BlueSecondary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف", tint = RedAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun AddDailyPriceDialog(
    priceType: String,
    initialActive: DailyPriceEntity?,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double, Double, Double, Double, Double, Double, Double, Double, Boolean) -> Unit
) {
    val now = System.currentTimeMillis()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", Locale("ar"))
    val defaultDate = dateFormat.format(Date(now))
    val defaultDay = dayFormat.format(Date(now))

    var dayName by remember { mutableStateOf(defaultDay) }
    var dateString by remember { mutableStateOf(defaultDate) }
    var setActiveToday by remember { mutableStateOf(true) }

    // Start with empty strings or initial price if exists, without dummy pre-seeds
    var redPrice by remember { mutableStateOf(if ((initialActive?.copperRedPrice ?: 0.0) > 0) initialActive!!.copperRedPrice.toString() else "") }
    var yellowPrice by remember { mutableStateOf(if ((initialActive?.copperYellowPrice ?: 0.0) > 0) initialActive!!.copperYellowPrice.toString() else "") }
    var lightMetalPrice by remember { mutableStateOf(if ((initialActive?.metalLightPrice ?: 0.0) > 0) initialActive!!.metalLightPrice.toString() else "") }
    var heavyMetalPrice by remember { mutableStateOf(if ((initialActive?.metalHeavyPrice ?: 0.0) > 0) initialActive!!.metalHeavyPrice.toString() else "") }
    var aluPrice by remember { mutableStateOf(if ((initialActive?.aluminumPrice ?: 0.0) > 0) initialActive!!.aluminumPrice.toString() else "") }
    var batPrice by remember { mutableStateOf(if ((initialActive?.batteryPrice ?: 0.0) > 0) initialActive!!.batteryPrice.toString() else "") }
    var ironPrice by remember { mutableStateOf(if ((initialActive?.ironPrice ?: 0.0) > 0) initialActive!!.ironPrice.toString() else "") }
    var plasticPrice by remember { mutableStateOf(if ((initialActive?.plasticPrice ?: 0.0) > 0) initialActive!!.plasticPrice.toString() else "") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (priceType == "PURCHASE") "إضافة تسعيرة مشتريات خردة لليوم" else "إضافة تسعيرة مبيعات خردة لليوم",
                fontWeight = FontWeight.Bold,
                color = BluePrimary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dayName,
                        onValueChange = { dayName = it },
                        label = { Text("اليوم") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dateString,
                        onValueChange = { dateString = it },
                        label = { Text("التاريخ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text("أدخل سعر الكيلو ($currency/كجم):", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                PriceInputDialogRow("النحاس الأحمر", redPrice, { redPrice = it })
                PriceInputDialogRow("النحاس الأصفر", yellowPrice, { yellowPrice = it })
                PriceInputDialogRow("المعدن الخفيف", lightMetalPrice, { lightMetalPrice = it })
                PriceInputDialogRow("المعدن الثقيل", heavyMetalPrice, { heavyMetalPrice = it })
                PriceInputDialogRow("الألمنيوم", aluPrice, { aluPrice = it })
                PriceInputDialogRow("البطاريات", batPrice, { batPrice = it })
                PriceInputDialogRow("الحديد", ironPrice, { ironPrice = it })
                PriceInputDialogRow("البلاستيك", plasticPrice, { plasticPrice = it })

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = setActiveToday, onCheckedChange = { setActiveToday = it })
                    Text("اعتماد هذه التسعيرة فوراً للمشتريات/المبيعات اليوم", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        dayName,
                        dateString,
                        redPrice.toDoubleOrNull() ?: 0.0,
                        yellowPrice.toDoubleOrNull() ?: 0.0,
                        lightMetalPrice.toDoubleOrNull() ?: 0.0,
                        heavyMetalPrice.toDoubleOrNull() ?: 0.0,
                        aluPrice.toDoubleOrNull() ?: 0.0,
                        batPrice.toDoubleOrNull() ?: 0.0,
                        ironPrice.toDoubleOrNull() ?: 0.0,
                        plasticPrice.toDoubleOrNull() ?: 0.0,
                        setActiveToday
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) {
                Text("حفظ التسعيرة", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

@Composable
fun PriceInputDialogRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("0.0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}
