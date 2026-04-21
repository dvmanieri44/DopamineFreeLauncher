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
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onLaunchApp: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }

    BackHandler(
        enabled = uiState.isAppListVisible ||
            uiState.isSettingsVisible ||
            uiState.isRedZoneSelectionVisible ||
            uiState.isAddictiveUsageVisible
    ) {
        when {
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

                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag <= -swipeThreshold) {
                            viewModel.showSettings()
                            totalDrag = 0f
                        }
                    },
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
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
        }

        if (!uiState.isAppListVisible && !uiState.isSettingsVisible) {
            AppDrawerHandle(
                modifier = Modifier.align(Alignment.BottomCenter),
                swipeThreshold = swipeThreshold,
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
                onToggleRedZoneApp = viewModel::toggleRedZoneApp
            )
        }
    }
}

@Composable
private fun AppDrawerHandle(
    modifier: Modifier = Modifier,
    swipeThreshold: Float,
    onOpen: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp)
            .pointerInput(Unit) {
                var totalDrag = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag <= -swipeThreshold) {
                            onOpen()
                            totalDrag = 0f
                        }
                    },
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                )
            }
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
    onToggleRedZoneApp: (String) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredApps = uiState.apps.filter { app ->
        app.label.contains(searchQuery.trim(), ignoreCase = true)
    }

    LaunchedEffect(uiState.isRedZoneSelectionVisible, uiState.isAddictiveUsageVisible) {
        if (!uiState.isRedZoneSelectionVisible) {
            searchQuery = ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
            .pointerInput(uiState.isRedZoneSelectionVisible) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDrag += dragAmount
                        if (totalDrag >= swipeThreshold) {
                            if (uiState.isRedZoneSelectionVisible) {
                                onBackFromRedZone()
                            } else if (uiState.isAddictiveUsageVisible) {
                                onBackFromAddictiveUsage()
                            } else {
                                onDismiss()
                            }
                            totalDrag = 0f
                        }
                    },
                    onDragEnd = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f }
                )
            }
            .padding(horizontal = 24.dp, vertical = 40.dp)
    ) {
        Text(
            text = when {
                uiState.isRedZoneSelectionVisible -> "RedZone"
                uiState.isAddictiveUsageVisible -> "Tempo de uso de Apps adquitivos"
                else -> "Configuracoes"
            },
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = when {
                uiState.isRedZoneSelectionVisible ->
                    "Escolha os apps que devem ser rotulados como RedZone"
                uiState.isAddictiveUsageVisible ->
                    "Aqui vamos acompanhar o tempo de uso dos apps adquitivos"
                else -> "Arraste para a direita para voltar"
            },
            modifier = Modifier.padding(top = 8.dp),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.size(28.dp))

        if (uiState.isRedZoneSelectionVisible) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Pesquisar apps para a RedZone",
                        color = Color.White.copy(alpha = 0.45f)
                    )
                },
                colors = fieldColors()
            )

            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "${uiState.redZonePackageNames.size} app(s) marcados",
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.size(12.dp))
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
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(
                            items = filteredApps,
                            key = { app -> app.packageName }
                        ) { app ->
                            RedZoneAppRow(
                                label = app.label,
                                isSelected = uiState.redZonePackageNames.contains(app.packageName),
                                onToggle = { onToggleRedZoneApp(app.packageName) }
                            )
                        }
                    }
                }
            }
        } else if (uiState.isAddictiveUsageVisible) {
            SettingsStaticCard(
                title = "Tempo de uso dos apps adquitivos",
                description = "Esta area vai mostrar quanto tempo os apps adquitivos estao sendo usados ao longo do dia."
            )
            SettingsStaticCard(
                title = "Base para a proxima etapa",
                description = if (uiState.redZonePackageNames.isEmpty()) {
                    "Nenhum app foi marcado na RedZone ainda. Quando voce selecionar apps na RedZone, eles poderao ser usados aqui."
                } else {
                    "${uiState.redZonePackageNames.size} app(s) ja estao marcados na RedZone e poderao alimentar esse painel futuramente."
                }
            )
        } else {
            SettingsOptionRow(
                title = "1. RedZone",
                description = "Escolha a lista de apps que o usuario quer parar de usar.",
                trailingText = "${uiState.redZonePackageNames.size} selecionado(s)",
                onClick = onOpenRedZone
            )
            SettingsOptionRow(
                title = "2. Tempo de uso de Apps adquitivos",
                description = "Area reservada para acompanhar o tempo gasto nos apps mais adquitivos.",
                trailingText = "Em breve",
                onClick = onOpenAddictiveUsage
            )
        }
    }
}

@Composable
private fun SettingsOptionRow(
    title: String,
    description: String,
    trailingText: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
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
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 13.sp
            )
        }
        Text(
            text = description,
            modifier = Modifier.padding(top = 6.dp),
            color = Color.White.copy(alpha = 0.68f),
            fontSize = 14.sp
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 14.dp),
            color = Color.White.copy(alpha = 0.08f)
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
            .padding(vertical = 14.dp)
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
            fontSize = 14.sp
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 14.dp),
            color = Color.White.copy(alpha = 0.08f)
        )
    }
}

@Composable
private fun RedZoneAppRow(
    label: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Color.White,
            fontSize = 18.sp
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onToggle() }
        )
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
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
