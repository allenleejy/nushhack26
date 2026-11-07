package com.nushhack26.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nushhack26.app.AppContainer
import com.nushhack26.app.data.repo.Identity
import com.nushhack26.app.data.repo.IdentityState
import com.nushhack26.app.sync.SyncStatus
import com.nushhack26.app.ui.components.FloodHost
import com.nushhack26.app.ui.components.SyncChip
import com.nushhack26.app.ui.components.SyncStatusBar
import com.nushhack26.app.ui.components.TenetMark
import com.nushhack26.app.ui.components.chipState
import com.nushhack26.app.ui.enrol.EnrolScreen
import com.nushhack26.app.ui.incoming.IncomingOfferBanner
import com.nushhack26.app.ui.lock.LockScreen
import com.nushhack26.app.ui.navigation.AppNavHost
import com.nushhack26.app.ui.navigation.DailyRecordRoute
import com.nushhack26.app.ui.navigation.DebugRoute
import com.nushhack26.app.ui.navigation.DevicesRoute
import com.nushhack26.app.ui.navigation.ForkRoute
import com.nushhack26.app.ui.navigation.GiveRoute
import com.nushhack26.app.ui.navigation.HomeRoute
import com.nushhack26.app.ui.navigation.MeRoute
import com.nushhack26.app.ui.navigation.OutboxRoute
import com.nushhack26.app.ui.navigation.ReceiveRelayedRoute
import com.nushhack26.app.ui.navigation.ReceiveRoute
import com.nushhack26.app.ui.navigation.ReportMissingRoute
import com.nushhack26.app.ui.navigation.TabActions
import com.nushhack26.app.ui.navigation.TabPage
import com.nushhack26.app.ui.navigation.TimelineRoute
import com.nushhack26.app.ui.navigation.RecordRoute
import com.nushhack26.app.ui.navigation.TopLevelTab
import com.nushhack26.app.ui.theme.LocalReduceMotion
import com.nushhack26.app.ui.theme.Tenet
import com.nushhack26.app.ui.theme.TenetDimens
import com.nushhack26.app.ui.theme.TenetMotion
import com.nushhack26.app.ui.theme.TenetType
import com.nushhack26.app.ui.theme.hazardStripes
import com.nushhack26.app.ui.theme.rememberReduceMotion
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

const val APP_NAME = "Tenet"

/**
 * The app frame: Enrol until the phone has an identity, then the role's tabs, with the Lock
 * screen drawn over them while the app is locked (docs/sync.md A.7). The tabs stay composed
 * under the lock, so unlocking returns to where the person was. The tap flood (FloodHost)
 * covers all of it.
 */
@Composable
fun AppScaffold(container: AppContainer) {
    val identityState by container.identity.state.collectAsStateWithLifecycle(initialValue = IdentityState.Loading)
    val locked by container.appLock.locked.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalReduceMotion provides rememberReduceMotion()) {
        FloodHost {
            when (val state = identityState) {
                IdentityState.Loading -> Box(Modifier.fillMaxSize().background(Tenet.colors.bg000))
                IdentityState.NotEnrolled -> EnrolFrame(container)
                is IdentityState.Enrolled -> Box(Modifier.fillMaxSize()) {
                    key(state.identity.role) {
                        RoleScaffold(container, state.identity)
                    }
                    if (locked) LockScreen(container, state.identity)
                }
            }
        }
    }
}

@Composable
private fun EnrolFrame(container: AppContainer) {
    val syncStatus by container.syncStatus.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = Tenet.colors.bg000,
        topBar = {
            TenetTopBar(
                APP_NAME,
                syncStatus,
                navigationIcon = { Box(Modifier.size(TenetDimens.targetMin), contentAlignment = Alignment.Center) { TenetMark() } },
                onChipClick = { container.syncScheduler.syncNow() },
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) { EnrolScreen(container) }
    }
}

/**
 * Top app bar, offline banner, then either the role's three tabs (a pager you can swipe, with
 * the bottom bar) or a pushed screen. The RECEIVE tab keeps the bar while it waits for a tap;
 * once an offer arrives it takes the whole frame, with the hazard band, until it ends.
 */
@Composable
private fun RoleScaffold(container: AppContainer, identity: Identity) {
    val tabs = TopLevelTab.forRole(identity.role)
    val startPage = tabs.indexOf(TopLevelTab.startFor(identity.role)).coerceAtLeast(0)
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val syncStatus by container.syncStatus.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(initialPage = startPage) { tabs.size }
    val scope = rememberCoroutineScope()
    val reduce = LocalReduceMotion.current
    var receiveInFlow by remember { mutableStateOf(false) }
    var receiveSession by rememberSaveable { mutableIntStateOf(0) }

    val onHome = destination?.hasRoute(HomeRoute::class) == true
    val currentTab = tabs[pagerState.currentPage]
    val receiving = onHome && receiveInFlow
    val inHandover = receiving || destination?.let {
        it.hasRoute(GiveRoute::class) || it.hasRoute(ReceiveRoute::class) || it.hasRoute(ReceiveRelayedRoute::class)
    } == true

    fun selectPage(page: Int) {
        if (!onHome) navController.popBackStack(HomeRoute, inclusive = false)
        scope.launch {
            if (reduce) {
                pagerState.scrollToPage(page)
            } else {
                pagerState.animateScrollToPage(page, animationSpec = tween(TenetMotion.TAB_MS, easing = TenetMotion.Emphasized))
            }
        }
    }
    fun endReceive() {
        receiveSession++
        receiveInFlow = false
        selectPage(startPage)
    }
    val actions = TabActions(
        openItem = { navController.navigate(TimelineRoute(it)) },
        openRecord = { navController.navigate(RecordRoute(it)) },
        handOver = { navController.navigate(GiveRoute(it)) },
        openFork = { navController.navigate(ForkRoute(it)) },
        receive = {
            val page = tabs.indexOf(TopLevelTab.Receive)
            if (page >= 0) selectPage(page) else navController.navigate(ReceiveRoute)
        },
        endReceive = ::endReceive,
        receiveFlowChanged = { receiveInFlow = it },
    )
    // Back from another tab goes to the first tab; back mid-receive ends the receive.
    BackHandler(enabled = onHome && (receiveInFlow || pagerState.currentPage != startPage)) {
        if (receiveInFlow) endReceive() else selectPage(startPage)
    }

    Scaffold(
        containerColor = Tenet.colors.bg000,
        topBar = {
            TenetTopBar(
                title = when {
                    receiving -> "Receive"
                    onHome -> currentTab.label
                    else -> titleFor(destination)
                },
                status = syncStatus,
                inHandover = inHandover,
                navigationIcon = {
                    when {
                        receiving -> IconButton(onClick = ::endReceive) { Icon(Icons.Sharp.Close, contentDescription = "Close") }
                        onHome -> Box(Modifier.size(TenetDimens.targetMin), contentAlignment = Alignment.Center) { TenetMark() }
                        destination == null -> Unit
                        inHandover -> IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Sharp.Close, contentDescription = "Close")
                        }
                        else -> IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Sharp.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                onChipClick = {
                    container.syncScheduler.syncNow()
                    val outboxPage = tabs.indexOf(TopLevelTab.Outbox)
                    when {
                        inHandover -> Unit
                        outboxPage >= 0 -> selectPage(outboxPage)
                        destination?.hasRoute(OutboxRoute::class) != true -> navController.navigate(OutboxRoute)
                    }
                },
                profile = if (onHome && !receiving) {
                    { ProfileButton(identity.name, onClick = { navController.navigate(MeRoute) }) }
                } else {
                    null
                },
            )
        },
        bottomBar = {
            if (onHome && !receiving) TenetBottomNav(tabs, pagerState, onSelect = ::selectPage)
        },
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            // An offer sent to me through the server (docs/sync.md A.10). Hidden mid-handover.
            if (!inHandover) {
                IncomingOfferBanner(container, onOpen = { navController.navigate(ReceiveRelayedRoute(it)) })
            }
            AppNavHost(navController, container, modifier = Modifier.weight(1f)) {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = !receiveInFlow,
                    key = { tabs[it].name },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    TabPage(
                        tabs[page],
                        container,
                        actions,
                        active = onHome && pagerState.settledPage == page && !pagerState.isScrollInProgress,
                        receiveSession = receiveSession,
                    )
                }
            }
        }
    }
}

/**
 * design/components/TopAppBar: bg-100, the title in uppercase mono, the phone's SyncChip and an
 * optional [profile] on the right, a 2dp rule below, or a hazard band inside the handover flow.
 * Then the offline banner.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenetTopBar(
    title: String,
    status: SyncStatus,
    navigationIcon: @Composable () -> Unit,
    onChipClick: () -> Unit,
    inHandover: Boolean = false,
    profile: (@Composable () -> Unit)? = null,
) {
    val colors = Tenet.colors
    Column {
        TopAppBar(
            title = { Text(title.uppercase(), style = TenetType.title) },
            navigationIcon = navigationIcon,
            actions = {
                SyncChip(
                    state = status.chipState(),
                    count = status.pendingCount,
                    modifier = Modifier
                        .padding(end = TenetDimens.space2)
                        .clickable(onClickLabel = "Sync now and open the outbox", onClick = onChipClick),
                )
                profile?.invoke()
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = colors.bg100,
                titleContentColor = colors.ink,
                navigationIconContentColor = colors.ink,
                actionIconContentColor = colors.ink,
            ),
        )
        if (inHandover) {
            Box(Modifier.fillMaxWidth().height(6.dp).hazardStripes(colors.onSignal, colors.signal))
        } else {
            HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.lineStrong)
        }
        SyncStatusBar(status)
    }
}

/** Me: a square with the person's initials, so each mirrored phone says whose it is. */
@Composable
private fun ProfileButton(name: String, onClick: () -> Unit) {
    val colors = Tenet.colors
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = "Me, $name" }) {
        Box(
            Modifier.size(36.dp).background(colors.bg200).border(TenetDimens.strokeControl, colors.lineStrong),
            contentAlignment = Alignment.Center,
        ) {
            Text(initials(name), style = TenetType.label.copy(fontWeight = FontWeight.Bold), color = colors.ink)
        }
    }
}

/** "Ravi Kumar" → RK, "Tan Wei Ming" → TM, "Dr Tan Mei Ling" → TL. */
private fun initials(name: String): String {
    val words = name.split(' ').filter { it.isNotEmpty() && it != "Dr" }
    return listOfNotNull(words.firstOrNull(), words.drop(1).lastOrNull()).joinToString("") { it.take(1).uppercase() }
}

/**
 * design/components/BottomNav: three square cells divided by hairlines. The selected fill and its
 * 6dp signal bar slide with the pager, so they follow a swipe under your finger.
 */
@Composable
private fun TenetBottomNav(tabs: List<TopLevelTab>, pagerState: PagerState, onSelect: (Int) -> Unit) {
    val colors = Tenet.colors
    Column(Modifier.background(colors.bg100)) {
        HorizontalDivider(thickness = TenetDimens.strokeControl, color = colors.lineStrong)
        BoxWithConstraints(Modifier.fillMaxWidth().height(TenetDimens.navBar)) {
            val cell = maxWidth / tabs.size
            Box(
                Modifier
                    .width(cell)
                    .fillMaxHeight()
                    .offset { IntOffset(((pagerState.currentPage + pagerState.currentPageOffsetFraction) * cell.toPx()).roundToInt(), 0) }
                    .background(colors.signalBg)
                    .drawBehind { drawRect(colors.signal, size = Size(size.width, 6.dp.toPx())) },
            )
            Row(
                Modifier.fillMaxSize().drawBehind {
                    for (i in 1 until tabs.size) {
                        val x = cell.toPx() * i
                        drawLine(colors.line, Offset(x, 0f), Offset(x, size.height), TenetDimens.strokeHairline.toPx())
                    }
                },
            ) {
                tabs.forEachIndexed { index, tab ->
                    val selected = pagerState.currentPage == index
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(TenetDimens.space1, Alignment.CenterVertically),
                    ) {
                        Icon(tab.icon, contentDescription = null, tint = if (selected) colors.signalText else colors.inkMuted)
                        Text(
                            tab.label.uppercase(),
                            style = TenetType.recordLabel.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp),
                            color = if (selected) colors.ink else colors.inkMuted,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
    }
}

private fun titleFor(destination: NavDestination?): String = when {
    destination == null -> APP_NAME
    destination.hasRoute(GiveRoute::class) -> "Hand over"
    destination.hasRoute(ReceiveRoute::class) -> "Receive"
    destination.hasRoute(ReceiveRelayedRoute::class) -> "Receive"
    destination.hasRoute(TimelineRoute::class) -> "Custody chain"
    destination.hasRoute(ForkRoute::class) -> "Fork"
    destination.hasRoute(OutboxRoute::class) -> "Outbox"
    destination.hasRoute(DailyRecordRoute::class) -> "Records"
    destination.hasRoute(DevicesRoute::class) -> "Devices"
    destination.hasRoute(ReportMissingRoute::class) -> "Report missing"
    destination.hasRoute(RecordRoute::class) -> "Receipt"
    destination.hasRoute(MeRoute::class) -> "Me"
    destination.hasRoute(DebugRoute::class) -> "Debug"
    else -> APP_NAME
}
