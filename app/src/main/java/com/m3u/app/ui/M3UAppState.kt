package com.m3u.app.ui

import android.util.Log
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navOptions
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.m3u.app.navigation.Destination
import com.m3u.app.navigation.TopLevelDestination
import com.m3u.features.favorite.navigation.favouriteNavigationRoute
import com.m3u.features.favorite.navigation.navigateToFavourite
import com.m3u.features.live.navigation.navigateToLive
import com.m3u.features.main.navgation.mainNavigationRoute
import com.m3u.features.main.navgation.navigateToMain
import com.m3u.features.setting.navigation.navigateToSetting
import com.m3u.features.setting.navigation.settingNavigationRoute
import com.m3u.features.subscription.navigation.navigationToSubscription
import com.m3u.ui.model.AppAction
import com.m3u.ui.model.SetActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

@Composable
fun rememberM3UAppState(
    @OptIn(ExperimentalAnimationApi::class)
    navController: NavHostController = rememberAnimatedNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): M3UAppState {
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            Log.d("AppState", "OnDestinationChanged: $destination")
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    return remember(navController, coroutineScope) {
        M3UAppState(navController, coroutineScope)
    }
}

@Stable
class M3UAppState(
    val navController: NavHostController,
    private val coroutineScope: CoroutineScope
) {
    val currentNavDestination: NavDestination?
        @Composable get() = navController
            .currentBackStackEntryAsState().value?.destination

    val currentTopLevelDestination: TopLevelDestination?
        @Composable get() = when (currentNavDestination?.route) {
            mainNavigationRoute -> TopLevelDestination.Main
            settingNavigationRoute -> TopLevelDestination.Setting
            favouriteNavigationRoute -> TopLevelDestination.Favourite
            else -> null
        }

    val topLevelDestinations: List<TopLevelDestination> = TopLevelDestination.values().asList()

    @Throws(IllegalArgumentException::class)
    fun navigateToTopLevelDestination(topLevelDestination: TopLevelDestination) {
        val topLevelNavOptions = navOptions {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = false
            }
            launchSingleTop = true
            restoreState = true
        }
        when (topLevelDestination) {
            TopLevelDestination.Main -> navController.navigateToMain(topLevelNavOptions)
            TopLevelDestination.Favourite -> navController.navigateToFavourite(topLevelNavOptions)
            TopLevelDestination.Setting -> navController.navigateToSetting(topLevelNavOptions)
        }
    }

    private val _labelFlow: MutableSharedFlow<String?> = MutableSharedFlow()
    val labelFlow
        get() = _labelFlow.shareIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000)
        )

    fun navigateToDestination(destination: Destination, label: String? = "") {
        when (destination) {
            is Destination.Subscription -> {
                coroutineScope.launch {
                    _labelFlow.emit(label)
                }
                navController.navigationToSubscription(destination.url)
            }

            is Destination.Live -> {
                navController.navigateToLive(destination.id)
            }
        }
    }

    private val _appActions: MutableState<List<AppAction>> = mutableStateOf(emptyList())
    val appActions: State<List<AppAction>> get() = _appActions

    val setAppActions: SetActions = {
        _appActions.value = it
    }

    fun onBackClick() {
        navController.popBackStack()
    }
}