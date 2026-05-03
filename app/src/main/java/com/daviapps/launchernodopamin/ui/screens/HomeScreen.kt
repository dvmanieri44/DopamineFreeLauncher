package com.daviapps.launchernodopamin.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daviapps.launchernodopamin.data.RedZoneAppConfig
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLaunchApp: (String) -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val limitedRedZoneCount = uiState.redZoneConfigs.values.count { config ->
        config.timeLimitMinutes != null
    }
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshEnforcementState()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(
        enabled = uiState.isRedZoneLimitDialogVisible ||
            uiState.isAppListVisible ||
            uiState.isSettingsVisible ||
            uiState.isRedZoneSelectionVisible ||
            uiState.isAddictiveUsageVisible
    ) {
        when {
            uiState.isRedZoneLimitDialogVisible -> viewModel.hideRedZoneLimitDialog()
            uiState.isAppListVisible -> viewModel.hideAppList()
            uiState.isAddictiveUsageVisible -> viewModel.hideAddictiveUsage()
            uiState.isRedZoneSelectionVisible -> viewModel.hideRedZoneSelection()
            uiState.isSettingsVisible -> viewModel.hideSettings()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(uiState.isAppListVisible, uiState.isSettingsVisible) {
                if (uiState.isAppListVisible || uiState.isSettingsVisible) {
                    return@pointerInput
                }

                var totalDragX = 0f
                var totalDragY = 0f
                var gestureHandled = false
                var startedInBottomHalf = false

                detectDragGestures(
                    onDragStart = { offset ->
                        totalDragX = 0f
                        totalDragY = 0f
                        gestureHandled = false
                        startedInBottomHalf = offset.y >= size.height / 2f
                    },
                    onDrag = { _, dragAmount ->
                        if (gestureHandled) {
                            return@detectDragGestures
                        }

                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y

                        val horizontalDistance = kotlin.math.abs(totalDragX)
                        val verticalDistance = kotlin.math.abs(totalDragY)

                        if (
                            startedInBottomHalf &&
                            totalDragY <= -swipeThreshold &&
                            verticalDistance > horizontalDistance
                        ) {
                            viewModel.showAppList()
                            gestureHandled = true
                        } else if (
                            totalDragX <= -swipeThreshold &&
                            horizontalDistance > verticalDistance
                        ) {
                            viewModel.showSettings()
                            gestureHandled = true
                        }
                    },
                    onDragEnd = {
                        totalDragX = 0f
                        totalDragY = 0f
                        gestureHandled = false
                        startedInBottomHalf = false
                    },
                    onDragCancel = {
                        totalDragX = 0f
                        totalDragY = 0f
                        gestureHandled = false
                        startedInBottomHalf = false
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = uiState.currentTime,
                color = Color.White,
                fontSize = 40.sp
            )

            if (uiState.currentDate.isNotBlank()) {
                Text(
                    text = uiState.currentDate,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 16.sp
                )
            }

            if (
                limitedRedZoneCount > 0 &&
                !uiState.redZoneEnforcementState.isMonitoringActive
            ) {
                Text(
                    text = "Bloqueio RedZone inativo. Libere as permissões nas configurações.",
                    color = Color.White.copy(alpha = 0.56f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (!uiState.isAppListVisible && !uiState.isSettingsVisible) {
            AppDrawerHandle(
                modifier = Modifier.align(Alignment.BottomCenter),
                onOpen = viewModel::showAppList
            )
        }

        AnimatedVisibility(
            visible = uiState.isAppListVisible,
            enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut()
        ) {
            AppsOverlay(
                uiState = uiState,
                swipeThreshold = swipeThreshold,
                onDismiss = viewModel::hideAppList,
                onLaunchApp = { packageName ->
                    viewModel.hideAppList()
                    onLaunchApp(packageName)
                }
            )
        }

        AnimatedVisibility(
            visible = uiState.isSettingsVisible,
            enter = slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth }) + fadeOut()
        ) {
            SettingsOverlay(
                uiState = uiState,
                swipeThreshold = swipeThreshold,
                onDismiss = viewModel::hideSettings,
                onOpenRedZone = viewModel::showRedZoneSelection,
                onOpenAddictiveUsage = viewModel::showAddictiveUsage,
                onBackFromRedZone = viewModel::hideRedZoneSelection,
                onBackFromAddictiveUsage = viewModel::hideAddictiveUsage,
                onOpenRedZoneLimitDialog = viewModel::showRedZoneLimitDialog,
                onDismissRedZoneLimitDialog = viewModel::hideRedZoneLimitDialog,
                onSelectRedZoneLimit = viewModel::selectRedZoneLimit,
                onConfirmRedZoneLimitSelection = viewModel::confirmRedZoneLimitSelection,
                onRemoveRedZoneApp = viewModel::removeRedZoneAppFromDialog,
                onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings
            )
        }
    }
}

@Composable
private fun AppDrawerHandle(
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DragHandle(
            shape = RoundedCornerShape(999.dp),
            color = Color.White.copy(alpha = 0.9f)
        )
        Text(
            text = "Arraste para cima para ver todos os apps",
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SettingsOverlay(
    uiState: HomeUiState,
    swipeThreshold: Float,
    onDismiss: () -> Unit,
    onOpenRedZone: () -> Unit,
    onOpenAddictiveUsage: () -> Unit,
    onBackFromRedZone: () -> Unit,
    onBackFromAddictiveUsage: () -> Unit,
    onOpenRedZoneLimitDialog: (String, String) -> Unit,
    onDismissRedZoneLimitDialog: () -> Unit,
    onSelectRedZoneLimit: (Int?) -> Unit,
    onConfirmRedZoneLimitSelection: () -> Unit,
    onRemoveRedZoneApp: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredApps = uiState.apps.filter { app ->
        app.label.contains(searchQuery.trim(), ignoreCase = true)
    }
    val limitedRedZoneCount = uiState.redZoneConfigs.values.count { config ->
        config.timeLimitMinutes != null
    }
    val screenTitle = when {
        uiState.isRedZoneSelectionVisible -> "RedZone"
        uiState.isAddictiveUsageVisible -> "Tempo de uso"
        else -> "Configurações"
    }
    val screenSubtitle = when {
        uiState.isRedZoneSelectionVisible ->
            "Defina quais apps precisam de fricção e limite diário."
        uiState.isAddictiveUsageVisible ->
            "Acompanhe o comportamento dos apps que mais puxam sua atenção."
        else -> "Ajustes rápidos para manter o launcher intencional."
    }
    val onBackOrClose = {
        when {
            uiState.isRedZoneSelectionVisible -> onBackFromRedZone()
            uiState.isAddictiveUsageVisible -> onBackFromAddictiveUsage()
            else -> onDismiss()
        }
    }

    LaunchedEffect(uiState.isRedZoneSelectionVisible, uiState.isAddictiveUsageVisible) {
        if (!uiState.isRedZoneSelectionVisible) {
            searchQuery = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080808))
            .pointerInput(uiState.isRedZoneSelectionVisible, uiState.isAddictiveUsageVisible) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag >= swipeThreshold) {
                            onBackOrClose()
                            totalDrag = 0f
                        }
                    },
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                )
            }
            .padding(horizontal = 20.dp, vertical = 34.dp)
    ) {
        SettingsHeader(
            title = screenTitle,
            subtitle = screenSubtitle,
            actionLabel = if (
                uiState.isRedZoneSelectionVisible ||
                uiState.isAddictiveUsageVisible
            ) {
                "Voltar"
            } else {
                "Fechar"
            },
            onAction = onBackOrClose
        )
        Spacer(modifier = Modifier.size(22.dp))

        if (uiState.isRedZoneSelectionVisible) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SettingsSearchField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Pesquisar apps para a RedZone"
                    )
                }
                item {
                    RedZoneStatsRow(
                        selectedCount = uiState.redZonePackageNames.size,
                        limitedCount = limitedRedZoneCount
                    )
                }
                item {
                    RedZoneEnforcementCard(
                        uiState = uiState,
                        limitedRedZoneCount = limitedRedZoneCount,
                        onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings
                    )
                }

                when {
                    uiState.isLoadingApps -> {
                        item {
                            LoadingSettingsState(text = "Carregando apps instalados")
                        }
                    }

                    filteredApps.isEmpty() -> {
                        item {
                            EmptySettingsState(text = "Nenhum app corresponde a essa pesquisa")
                        }
                    }

                    else -> {
                        item {
                            SettingsSectionLabel(text = "Apps instalados")
                        }
                        items(
                            items = filteredApps,
                            key = { app -> app.packageName }
                        ) { app ->
                            RedZoneAppRow(
                                label = app.label,
                                isSelected = uiState.redZonePackageNames.contains(app.packageName),
                                selectedLimitLabel = uiState.redZoneConfigs[app.packageName]
                                    .toLimitLabel(),
                                onClick = {
                                    onOpenRedZoneLimitDialog(app.packageName, app.label)
                                }
                            )
                        }
                    }
                }
            }
        } else if (uiState.isAddictiveUsageVisible) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    SettingsStaticCard(
                        title = "Tempo de uso dos apps viciantes",
                        description = "Esta área vai mostrar quanto tempo os apps viciantes estão sendo usados ao longo do dia."
                    )
                }
                item {
                    SettingsStaticCard(
                        title = "Base para a próxima etapa",
                        description = if (uiState.redZonePackageNames.isEmpty()) {
                            "Nenhum app foi marcado na RedZone ainda. Quando você selecionar apps na RedZone, eles poderão ser usados aqui."
                        } else {
                            "${uiState.redZonePackageNames.size} app(s) já estão marcados na RedZone e poderão alimentar esse painel futuramente."
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    RedZoneEnforcementCard(
                        uiState = uiState,
                        limitedRedZoneCount = limitedRedZoneCount,
                        onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                        onOpenAccessibilitySettings = onOpenAccessibilitySettings
                    )
                }
                item {
                    SettingsSectionLabel(text = "Ferramentas")
                }
                item {
                    SettingsOptionRow(
                        title = "RedZone",
                        description = "Escolha apps para limitar e reduzir uso impulsivo.",
                        trailingText = "${uiState.redZonePackageNames.size} apps",
                        detailText = "$limitedRedZoneCount com limite",
                        onClick = onOpenRedZone
                    )
                }
                item {
                    SettingsOptionRow(
                        title = "Tempo de uso",
                        description = "Área reservada para acompanhar os padrões de uso.",
                        trailingText = "Em breve",
                        detailText = "Leitura diária",
                        onClick = onOpenAddictiveUsage
                    )
                }
            }
        }
    }

    if (uiState.isRedZoneLimitDialogVisible) {
        RedZoneLimitDialog(
            appLabel = uiState.redZoneDialogAppLabel.orEmpty(),
            selectedLimitMinutes = uiState.redZoneDialogSelectedLimitMinutes,
            isAlreadySelected = uiState.redZoneDialogPackageName in uiState.redZonePackageNames,
            onDismiss = onDismissRedZoneLimitDialog,
            onSelectLimit = onSelectRedZoneLimit,
            onConfirm = onConfirmRedZoneLimitSelection,
            onRemove = onRemoveRedZoneApp
        )
    }
}

@Composable
private fun SettingsHeader(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                modifier = Modifier.padding(top = 8.dp),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
        TextButton(
            onClick = onAction,
            modifier = Modifier
                .padding(start = 12.dp)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = actionLabel,
                color = Color.White.copy(alpha = 0.86f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.035f),
                shape = RoundedCornerShape(18.dp)
            ),
        singleLine = true,
        placeholder = {
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.45f)
            )
        },
        colors = fieldColors()
    )
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        color = Color.White.copy(alpha = 0.48f),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun RedZoneStatsRow(
    selectedCount: Int,
    limitedCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SettingsMetric(
            label = "Na RedZone",
            value = selectedCount.toString(),
            modifier = Modifier.weight(1f)
        )
        SettingsMetric(
            label = "Com limite",
            value = limitedCount.toString(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SettingsMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                color = Color.White.copy(alpha = 0.045f),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = label,
            modifier = Modifier.padding(top = 4.dp),
            color = Color.White.copy(alpha = 0.58f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun LoadingSettingsState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CircularProgressIndicator(color = Color.White)
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 14.sp
        )
    }
}

@Composable
private fun EmptySettingsState(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.035f),
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 18.dp, vertical = 24.dp),
        color = Color.White.copy(alpha = 0.76f),
        fontSize = 15.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun SettingsOptionRow(
    title: String,
    description: String,
    trailingText: String,
    detailText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.045f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.09f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = trailingText,
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = description,
            modifier = Modifier.padding(top = 8.dp),
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Text(
            text = detailText,
            modifier = Modifier.padding(top = 14.dp),
            color = Color.White.copy(alpha = 0.46f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SettingsStaticCard(
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = 0.045f),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.09f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            modifier = Modifier.padding(top = 6.dp),
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun RedZoneEnforcementCard(
    uiState: HomeUiState,
    limitedRedZoneCount: Int,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit
) {
    val enforcementState = uiState.redZoneEnforcementState
    val statusLabel = when {
        enforcementState.isMonitoringActive && limitedRedZoneCount > 0 -> "Ativo"
        enforcementState.isMonitoringActive -> "Pronto"
        else -> "Inativo"
    }
    val headline = when {
        enforcementState.isMonitoringActive && limitedRedZoneCount > 0 ->
            "Bloqueio automático ativo"
        enforcementState.isMonitoringActive ->
            "Bloqueio automático pronto"
        else -> "Bloqueio automático inativo"
    }
    val description = when {
        enforcementState.isMonitoringActive && limitedRedZoneCount > 0 ->
            "Os limites da RedZone vão bloquear $limitedRedZoneCount app(s) e trazer você de volta para a home."
        enforcementState.isMonitoringActive ->
            "As permissões já foram liberadas. Agora basta definir um limite em algum app da RedZone."
        limitedRedZoneCount > 0 ->
            "Para aplicar os limites da RedZone, libere Acesso de uso e Acessibilidade."
        else ->
            "Libere as permissões necessárias para ativar o bloqueio automático quando você definir limites por app."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White.copy(alpha = if (enforcementState.isMonitoringActive) 0.06f else 0.035f),
                shape = RoundedCornerShape(22.dp)
            )
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = if (enforcementState.isMonitoringActive) 0.18f else 0.09f),
                shape = RoundedCornerShape(22.dp)
            )
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = headline,
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = statusLabel,
                modifier = Modifier
                    .background(
                        color = Color.White.copy(alpha = if (enforcementState.isMonitoringActive) 0.14f else 0.07f),
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = description,
            modifier = Modifier.padding(top = 8.dp),
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        if (!enforcementState.isUsageAccessGranted || !enforcementState.isAccessibilityServiceEnabled) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!enforcementState.isUsageAccessGranted) {
                    RedZonePermissionButton(
                        text = "Acesso de uso",
                        isPrimary = false,
                        onClick = onOpenUsageAccessSettings,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (!enforcementState.isAccessibilityServiceEnabled) {
                    RedZonePermissionButton(
                        text = "Acessibilidade",
                        isPrimary = true,
                        onClick = onOpenAccessibilitySettings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RedZonePermissionButton(
    text: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.then(
            if (isPrimary) {
                Modifier.background(
                    color = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            } else {
                Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        )
    ) {
        Text(
            text = text,
            color = if (isPrimary) Color.Black else Color.White.copy(alpha = 0.82f),
            fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
private fun RedZoneAppRow(
    label: String,
    isSelected: Boolean,
    selectedLimitLabel: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) {
                    Color.White.copy(alpha = 0.065f)
                } else {
                    Color.White.copy(alpha = 0.032f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    Color.White.copy(alpha = 0.18f)
                } else {
                    Color.White.copy(alpha = 0.07f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 18.sp
            )

            if (selectedLimitLabel != null) {
                Text(
                    text = selectedLimitLabel,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Color.White.copy(alpha = 0.62f),
                    fontSize = 13.sp
                )
            }
        }
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )
    }
}

@Composable
private fun RedZoneLimitDialog(
    appLabel: String,
    selectedLimitMinutes: Int?,
    isAlreadySelected: Boolean,
    onDismiss: () -> Unit,
    onSelectLimit: (Int?) -> Unit,
    onConfirm: () -> Unit,
    onRemove: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(max = 360.dp),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF090909),
            contentColor = Color.White,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = if (appLabel.isBlank()) {
                        "Configurar RedZone"
                    } else {
                        appLabel
                    },
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Escolha o limite diário. Sem limitador mantém o app marcado, mas não bloqueia.",
                    color = Color.White.copy(alpha = 0.68f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RedZoneLimitOptionRow(
                        label = "1 min",
                        isSelected = selectedLimitMinutes == 1,
                        onClick = { onSelectLimit(1) }
                    )
                    RedZoneLimitOptionRow(
                        label = "5 min",
                        isSelected = selectedLimitMinutes == 5,
                        onClick = { onSelectLimit(5) }
                    )
                    RedZoneLimitOptionRow(
                        label = "10 min",
                        isSelected = selectedLimitMinutes == 10,
                        onClick = { onSelectLimit(10) }
                    )
                    RedZoneLimitOptionRow(
                        label = "Sem limitador",
                        isSelected = selectedLimitMinutes == null,
                        onClick = { onSelectLimit(null) }
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isAlreadySelected) {
                        TextButton(
                            onClick = onRemove,
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 1.dp,
                                    color = Color.White.copy(alpha = 0.16f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Text(
                                text = "Remover",
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text(
                            text = "Cancelar",
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }

                    TextButton(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = Color.White,
                                shape = RoundedCornerShape(16.dp)
                            )
                    ) {
                        Text(
                            text = "Salvar",
                            color = Color.Black,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RedZoneLimitOptionRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) {
                    Color.White.copy(alpha = 0.06f)
                } else {
                    Color.White.copy(alpha = 0.025f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .border(
                width = 1.dp,
                color = if (isSelected) {
                    Color.White.copy(alpha = 0.36f)
                } else {
                    Color.White.copy(alpha = 0.12f)
                },
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick
        )
        Text(
            text = label,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun AppsOverlay(
    uiState: HomeUiState,
    swipeThreshold: Float,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredApps = uiState.apps.filter { app ->
        app.label.contains(searchQuery.trim(), ignoreCase = true)
    }

    LaunchedEffect(uiState.isAppListVisible) {
        if (!uiState.isAppListVisible) {
            searchQuery = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = Color(0xFF0A0A0A),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag >= swipeThreshold) {
                            onDismiss()
                            totalDrag = 0f
                        }
                    },
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                )
            }
            .padding(top = 88.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DragHandle(
                shape = RoundedCornerShape(999.dp),
                color = Color.White.copy(alpha = 0.9f)
            )
            Text(
                text = "Todos os apps",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Arraste para baixo para fechar",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Pesquisar apps",
                        color = Color.White.copy(alpha = 0.45f)
                    )
                },
                colors = fieldColors()
            )
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

        when {
            uiState.isLoadingApps -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }
            }

            uiState.apps.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Nenhum app encontrado",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White,
                        fontSize = 20.sp
                    )
                }
            }

            filteredApps.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Nenhum app corresponde a essa pesquisa",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White.copy(alpha = 0.82f),
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = filteredApps,
                        key = { app -> app.packageName }
                    ) { app ->
                        Text(
                            text = app.label,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLaunchApp(app.packageName) }
                                .padding(vertical = 14.dp),
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DragHandle(
    shape: Shape,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(width = 56.dp, height = 5.dp)
            .background(color = color, shape = shape)
    )
}

@Composable
private fun fieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedIndicatorColor = Color.White.copy(alpha = 0.6f),
    unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
    cursorColor = Color.White,
    focusedPlaceholderColor = Color.White.copy(alpha = 0.45f),
    unfocusedPlaceholderColor = Color.White.copy(alpha = 0.45f)
)

private fun RedZoneAppConfig?.toLimitLabel(): String? {
    return when (val limit = this?.timeLimitMinutes) {
        null -> if (this == null) null else "Sem limitador"
        else -> "$limit min"
    }
}
