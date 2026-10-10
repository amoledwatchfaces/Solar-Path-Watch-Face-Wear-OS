package com.amoledwatchfaces.solarpath.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.activity.viewModels
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.platform.LocalContext
import com.amoledwatchfaces.solarpath.presentation.ui.InitialLocationDialog
import com.amoledwatchfaces.solarpath.presentation.ui.SolarPathAppTheme
import com.amoledwatchfaces.solarpath.utils.areLocationPermissionsGranted
import com.amoledwatchfaces.solarpath.utils.updateComplications
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            SolarPathMainApp(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
        updateComplications()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SolarPathMainApp(
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val transformationSpec = rememberTransformationSpec()
    val focusRequester = remember { FocusRequester() }
    val navController = rememberSwipeDismissableNavController()
    val listState = rememberTransformingLazyColumnState()
    val locationListState = rememberTransformingLazyColumnState()
    val faqListState = rememberTransformingLazyColumnState()

    val preferences by viewModel.preferences.collectAsState()
    val initialLocationDialogState by viewModel.initialLocationDialogState.collectAsState()

    val permissionState = rememberPermissionState(
        permission = android.Manifest.permission.ACCESS_COARSE_LOCATION,
        onPermissionResult = { isGranted ->
            if (isGranted) {
                viewModel.requestLocation()
            }
        }
    )

    SolarPathAppTheme {
        AppScaffold {
            SwipeDismissableNavHost(
                navController = navController,
                startDestination = "main"
            ) {
                composable("main") {
                    if (!context.areLocationPermissionsGranted() && !preferences.isLocationPromptDismissed) {
                        LocationDisabledScreen(
                            transformationSpec = transformationSpec,
                            onEnableLocation = { permissionState.launchPermissionRequest() },
                            onUseWithoutLocation = { viewModel.dismissLocationPrompt() }
                        )
                    } else {
                        MainScreen(
                            navController = navController,
                            viewModel = viewModel,
                            transformationSpec = transformationSpec,
                            focusRequester = focusRequester,
                            listState = listState,
                            onEnableLocation = { permissionState.launchPermissionRequest() }
                        )
                    }
                }

                composable("location_choose") {
                    LocationChooseScreen(
                        navController = navController,
                        viewModel = viewModel,
                        permissionState = permissionState,
                        transformationSpec = transformationSpec,
                        focusRequester = focusRequester,
                        listState = locationListState
                    )
                }

                composable("faq") {
                    FaqScreen(
                        navController = navController,
                        transformationSpec = transformationSpec,
                        focusRequester = focusRequester,
                        listState = faqListState
                    )
                }
            }

            if (initialLocationDialogState) {
                InitialLocationDialog(
                    onConfirm = {
                        viewModel.setInitialLocationDialogState(false)
                        permissionState.launchPermissionRequest()
                    },
                    onDismiss = {
                        viewModel.setInitialLocationDialogState(false)
                    }
                )
            }
        }
    }
}
