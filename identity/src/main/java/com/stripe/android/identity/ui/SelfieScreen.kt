package com.stripe.android.identity.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.camera.framework.image.mirrorHorizontally
import com.stripe.android.camera.scanui.CameraView
import com.stripe.android.identity.R
import com.stripe.android.identity.analytics.IdentityAnalyticsRequestFactory.Companion.SCREEN_NAME_SELFIE
import com.stripe.android.identity.camera.IdentityCameraManager
import com.stripe.android.identity.camera.SelfieCameraManager
import com.stripe.android.identity.navigation.navigateToErrorScreenWithDefaultValues
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.states.FaceDetectorTransitioner
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.utils.startScanning
import com.stripe.android.identity.viewmodel.IdentityScanViewModel
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.text.Html
import com.stripe.android.uicore.text.dimensionResourceSp

internal const val SELFIE_VIEW_FINDER_ASPECT_RATIO = 1f
internal const val SELFIE_SCAN_TITLE_TAG = "SelfieScanTitle"
internal const val SELFIE_SCAN_MESSAGE_TAG = "SelfieScanMessage"
internal const val SELFIE_SCAN_CONTINUE_BUTTON_TAG = "SelfieScanContinue"
internal const val SCAN_VIEW_TAG = "SelfieScanViewTag"
internal const val RESULT_VIEW_TAG = "SelfieResultViewTag"
internal const val CONSENT_CHECKBOX_TAG = "ConsentCheckboxTag"
private const val FLASH_MAX_ALPHA = 0.5f
private const val FLASH_ANIMATION_TIME = 200

@Composable
internal fun SelfieScanScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    identityScanViewModel: IdentityScanViewModel,
) {
    MdcTheme {
        val verificationPageState by identityViewModel.verificationPage.observeAsState(Resource.loading())
        val context = LocalContext.current
        val changedDisplayState by identityScanViewModel.displayStateChangedFlow.collectAsState()
        val newDisplayState by remember {
            derivedStateOf {
                changedDisplayState?.first
            }
        }

        CheckVerificationPageAndCompose(
            verificationPageResource = verificationPageState,
            onError = {
                identityViewModel.errorCause.postValue(it)
                navController.navigateToErrorScreenWithDefaultValues(context)
            }
        ) { verificationPage ->
            val cameraManager = remember {
                SelfieCameraManager(context = context) { cause ->
                    identityViewModel.sendAnalyticsRequest(
                        identityViewModel.identityAnalyticsRequestFactory.cameraError(
                            scanType = IdentityScanState.ScanType.SELFIE,
                            throwable = IllegalStateException(cause)
                        )
                    )
                }
            }

            val successSelfieCapturePage =
                remember {
                    requireNotNull(verificationPage.selfieCapture) {
                        identityViewModel.errorCause.postValue(
                            IllegalStateException("VerificationPage.selfieCapture is null")
                        )
                        navController.navigateToErrorScreenWithDefaultValues(context)
                    }
                }

            val message = when (newDisplayState) {
                is IdentityScanState.Finished ->
                    stringResource(id = R.string.selfie_capture_complete)
                is IdentityScanState.Found ->
                    stringResource(id = R.string.capturing)
                is IdentityScanState.Initial ->
                    stringResource(id = R.string.position_selfie)
                is IdentityScanState.Satisfied ->
                    stringResource(id = R.string.selfie_capture_complete)
                is IdentityScanState.TimeOut -> ""
                is IdentityScanState.Unsatisfied -> ""
                null -> {
                    stringResource(id = R.string.position_selfie)
                }
            }

            var loadingButtonState by remember(newDisplayState) {
                mutableStateOf(
                    if (newDisplayState is IdentityScanState.Finished) {
                        LoadingButtonState.Idle
                    } else {
                        LoadingButtonState.Disabled
                    }
                )
            }

            var allowImageCollection by remember {
                mutableStateOf(false)
            }

            var allowImageCollectionCheckboxEnabled by remember {
                mutableStateOf(true)
            }

            var flashed by remember {
                mutableStateOf(false)
            }

            val imageAlpha: Float by animateFloatAsState(
                targetValue = if (!flashed && newDisplayState is IdentityScanState.Found) FLASH_MAX_ALPHA else 0f,
                animationSpec = tween(
                    durationMillis = FLASH_ANIMATION_TIME,
                    easing = LinearEasing,
                ),
                finishedListener = {
                    flashed = true
                }
            )

            val lifecycleOwner = LocalLifecycleOwner.current

            LaunchedEffect(Unit) {
                identityViewModel.resetSelfieUploadedState()
            }

            CameraScreenLaunchedEffect(
                identityViewModel = identityViewModel,
                identityScanViewModel = identityScanViewModel,
                verificationPage = verificationPage,
                navController = navController,
                cameraManager = cameraManager
            ) {
                startScanning(
                    IdentityScanState.ScanType.SELFIE,
                    identityViewModel = identityViewModel,
                    identityScanViewModel = identityScanViewModel,
                    lifecycleOwner = lifecycleOwner
                )
            }

            ScreenTransitionLaunchedEffect(
                identityViewModel = identityViewModel,
                screenName = SCREEN_NAME_SELFIE,
                scanType = IdentityScanState.ScanType.SELFIE
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        vertical = dimensionResource(id = R.dimen.page_vertical_margin)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(id = R.string.selfie_captures),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = dimensionResource(id = R.dimen.page_horizontal_margin)
                            )
                            .semantics {
                                testTag = SELFIE_SCAN_TITLE_TAG
                            },
                        fontSize = dimensionResourceSp(id = R.dimen.scan_title_text_size),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(
                                top = 20.dp,
                                bottom = dimensionResource(id = R.dimen.item_vertical_margin),
                                start = dimensionResource(id = R.dimen.page_horizontal_margin),
                                end = dimensionResource(id = R.dimen.page_horizontal_margin)
                            )
                            .semantics {
                                testTag = SELFIE_SCAN_MESSAGE_TAG
                            },
                        maxLines = 3
                    )

                    if (newDisplayState is IdentityScanState.Finished) {
                        ResultView(
                            displayState = newDisplayState as IdentityScanState.Finished,
                            allowImageCollectionHtml = successSelfieCapturePage.consentText,
                            allowImageCollectionCheckboxEnabled = allowImageCollectionCheckboxEnabled,
                            allowImageCollection = allowImageCollection,
                        ) {
                            allowImageCollection = it
                        }
                    } else {
                        SelfieCameraViewFinder(imageAlpha, cameraManager)
                    }
                }
                LoadingButton(
                    modifier = Modifier
                        .testTag(SELFIE_SCAN_CONTINUE_BUTTON_TAG)
                        .padding(dimensionResource(id = R.dimen.page_horizontal_margin)),
                    text = stringResource(id = R.string.kontinue).uppercase(),
                    state = loadingButtonState
                ) {
                    loadingButtonState = LoadingButtonState.Loading
                    allowImageCollectionCheckboxEnabled = false

                    identityViewModel.collectDataForSelfieScreen(
                        navController = navController,
                        faceDetectorTransitioner =
                        requireNotNull(
                            newDisplayState?.transitioner as? FaceDetectorTransitioner
                        ) {
                            "Failed to retrieve final result for Selfie"
                        },
                        allowImageCollection = allowImageCollection
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultView(
    displayState: IdentityScanState,
    allowImageCollectionHtml: String,
    allowImageCollectionCheckboxEnabled: Boolean,
    allowImageCollection: Boolean,
    onAllowImageCollectionChanged: (Boolean) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .padding(
                horizontal = 5.dp
            )
            .testTag(RESULT_VIEW_TAG),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            (displayState.transitioner as FaceDetectorTransitioner)
                .filteredFrames.map { it.first.cameraPreviewImage.image.mirrorHorizontally() }
        ) { bitmap ->
            val imageBitmap = remember {
                bitmap.asImageBitmap()
            }
            Image(
                painter = BitmapPainter(imageBitmap),
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(dimensionResource(id = R.dimen.view_finder_corner_radius))),
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(id = R.string.selfie_item_description)
            )
        }
    }

    Row(
        modifier = Modifier.padding(
            start = dimensionResource(id = R.dimen.page_horizontal_margin),
            end = dimensionResource(id = R.dimen.page_horizontal_margin),
            top = 20.dp
        )
    ) {
        Checkbox(
            modifier = Modifier.testTag(CONSENT_CHECKBOX_TAG),
            checked = allowImageCollection,
            onCheckedChange = {
                onAllowImageCollectionChanged(!allowImageCollection)
            },
            enabled = allowImageCollectionCheckboxEnabled
        )

        Html(
            html = allowImageCollectionHtml,
            urlSpanStyle = SpanStyle(
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.colors.secondary
            )
        )
    }
}

@Composable
private fun SelfieCameraViewFinder(
    imageAlpha: Float,
    cameraManager: IdentityCameraManager,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(SELFIE_VIEW_FINDER_ASPECT_RATIO)
            .padding(
                horizontal = dimensionResource(id = R.dimen.page_horizontal_margin)
            )
            .clip(RoundedCornerShape(dimensionResource(id = R.dimen.view_finder_corner_radius)))
            .testTag(SCAN_VIEW_TAG)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                CameraView(
                    it,
                    CameraView.ViewFinderType.Fill
                )
            },
            update = {
                cameraManager.onCameraViewUpdate(it)
            }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(imageAlpha)
                .background(colorResource(id = R.color.flash_mask_color))
        )
    }
}
