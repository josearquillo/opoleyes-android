package com.opoleyes.ui.screens

import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.opoleyes.R
import com.opoleyes.data.local.PreferencesManager
import com.opoleyes.ui.components.GameButton
import com.opoleyes.ui.navigation.Routes
import com.opoleyes.ui.theme.*
import kotlinx.coroutines.delay

data class LogoOption(
    val id: String,
    val nameRes: Int,
    val iconRes: Int,
    val aliasSuffix: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoPickerScreen(navController: NavController, isFirstLaunch: Boolean = false) {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    var selectedLogo by remember { mutableStateOf(prefs.getLogoPref()) }
    var applying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val logoOptions = listOf(
        LogoOption("ol_v1", R.string.logo_ol_v1, R.drawable.ic_logo_ol_v1, "OlV1Alias"),
        LogoOption("ol_v2", R.string.logo_ol_v2, R.drawable.ic_logo_ol_v2, "OlV2Alias"),
        LogoOption("ol_v3", R.string.logo_ol_v3, R.drawable.ic_logo_ol_v3, "OlV3Alias")
    )

    // Staggered appearance
    var visibleItems by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..5) {
            delay(80)
            visibleItems = i
        }
    }

    Scaffold(
        topBar = {
            if (!isFirstLaunch) {
                TopAppBar(
                    title = { Text(stringResource(R.string.choose_logo), color = Accent, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = TextLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BgDark,
                        titleContentColor = Accent
                    )
                )
            }
        },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isFirstLaunch) {
                Spacer(Modifier.height(48.dp))
            }

            // Title
            StaggeredAppearanceLogo(visibleItems, 0) {
                Text(
                    stringResource(R.string.choose_logo),
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(12.dp))

            StaggeredAppearanceLogo(visibleItems, 1) {
                Text(
                    stringResource(R.string.choose_logo_subtitle),
                    color = TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(32.dp))

            // Logo cards
            logoOptions.forEachIndexed { index, option ->
                StaggeredAppearanceLogo(visibleItems, index + 1) {
                    LogoCard(
                        option = option,
                        isSelected = selectedLogo == option.id,
                        onClick = { selectedLogo = option.id }
                    )
                    Spacer(Modifier.height(14.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            // Apply button
            StaggeredAppearanceLogo(visibleItems, 4) {
                if (applying) {
                    Text(
                        stringResource(R.string.logo_change_notice),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
                GameButton(
                    text = stringResource(R.string.logo_apply),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    color1 = Success,
                    color2 = SuccessDark,
                    textFontSize = 20,
                    enabled = !applying
                ) {
                    applying = true
                    prefs.setLogoPref(selectedLogo)

                    // Enable the selected alias, disable others
                    val pm = context.packageManager
                    val pkg = context.packageName
                    logoOptions.forEach { opt ->
                        val component = ComponentName(pkg, "$pkg.${opt.aliasSuffix}")
                        val state = if (opt.id == selectedLogo)
                            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                        else
                            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
                    }

                    // Navigate after a short delay to let the system process the change
                    scope.launch {
                        delay(800)
                        if (isFirstLaunch) {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGO_PICKER) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LogoCard(option: LogoOption, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor = if (isSelected) Accent else SurfaceVariant
    val borderWidth = if (isSelected) 3.dp else 1.dp
    val scale = if (isSelected) 1f else 0.95f
    val scaleAnim by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(BgCard, BgCardDark)))
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Logo preview in a rounded square
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BgDark)
                .border(1.dp, SurfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(option.iconRes),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                stringResource(option.nameRes),
                color = TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            if (isSelected) {
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.logo_selected),
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Selection indicator
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Brush.horizontalGradient(listOf(Primary, PurpleDark))),
                contentAlignment = Alignment.Center
            ) {
                Text("✓", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StaggeredAppearanceLogo(visibleCount: Int, index: Int, content: @Composable () -> Unit) {
    val visible = visibleCount > index
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(250),
        label = "stagger$index"
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale$index"
    )
    Box(modifier = Modifier.alpha(alpha).scale(scale)) {
        content()
    }
}
