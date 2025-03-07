package com.stripe.android.identity.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SnackbarDefaults.backgroundColor
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.contentColorFor
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
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.android.material.composethemeadapter.MdcTheme
import com.stripe.android.identity.R
import com.stripe.android.identity.navigation.navigateToFinalErrorScreen
import com.stripe.android.identity.navigation.routeToScreenName
import com.stripe.android.identity.networking.Resource
import com.stripe.android.identity.networking.Status
import com.stripe.android.identity.networking.models.CollectedDataParam
import com.stripe.android.identity.networking.models.Requirement
import com.stripe.android.identity.states.IdentityScanState
import com.stripe.android.identity.viewmodel.IdentityViewModel
import com.stripe.android.uicore.text.dimensionResourceSp

internal const val FRONT_ROW_TAG = "frontRow"
internal const val BACK_ROW_TAG = "backRow"
internal const val UPLOAD_SCREEN_CONTINUE_BUTTON_TAG = "uploadScreenContinueButton"
internal const val SHOULD_SHOW_TAKE_PHOTO_TAG = "shouldShowTakePhoto"
internal const val SHOULD_SHOW_CHOOSE_PHOTO_TAG = "shouldShowChoosePhoto"

internal enum class UploadMethod {
    TAKE_PHOTO, CHOOSE_PHOTO
}

internal data class DocumentUploadSideInfo(
    @StringRes
    val descriptionRes: Int,
    @StringRes
    val checkmarkContentDescriptionRes: Int,
    val scanType: IdentityScanState.ScanType
)

@Composable
internal fun UploadScreen(
    navController: NavController,
    identityViewModel: IdentityViewModel,
    collectedDataParamType: CollectedDataParam.Type,
    route: String,
    @StringRes titleRes: Int,
    @StringRes contextRes: Int,
    frontInfo: DocumentUploadSideInfo,
    backInfo: DocumentUploadSideInfo?,
    shouldShowTakePhoto: Boolean,
    shouldShowChoosePhoto: Boolean
) {
    MdcTheme {
        val localContext = LocalContext.current
        val verificationState by identityViewModel.verificationPage.observeAsState(Resource.loading())
        CheckVerificationPageAndCompose(
            verificationPageResource = verificationState,
            onError = {
                identityViewModel.errorCause.postValue(it)
                navController.navigateToFinalErrorScreen(
                    localContext,
                )
            }
        ) {
            val frontUploadState by identityViewModel.documentFrontUploadedState.collectAsState()
            val backUploadState by identityViewModel.documentBackUploadedState.collectAsState()
            val collectedData by identityViewModel.collectedData.collectAsState()
            val missings by identityViewModel.missingRequirements.collectAsState()
            LaunchedEffect(Unit) {
                identityViewModel.collectDataForDocumentUploadScreen(
                    navController,
                    collectedDataParamType,
                    route
                )
            }

            ScreenTransitionLaunchedEffect(
                identityViewModel = identityViewModel,
                screenName = route.routeToScreenName(),
                scanType = frontInfo.scanType
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = dimensionResource(id = R.dimen.page_horizontal_margin),
                        end = dimensionResource(id = R.dimen.page_horizontal_margin),
                        top = 64.dp,
                        bottom = dimensionResource(id = R.dimen.page_vertical_margin)
                    )
                    .testTag(SCROLLABLE_COLUMN_TAG)
            ) {
                Text(
                    text = stringResource(id = titleRes),
                    fontSize = dimensionResourceSp(id = R.dimen.upload_title_text_size),

                    modifier = Modifier.padding(
                        vertical = dimensionResource(id = R.dimen.item_vertical_margin)
                    )
                )
                Text(
                    text = stringResource(id = contextRes),
                    modifier = Modifier.padding(
                        bottom = 32.dp
                    )
                )

                val frontUploadedUiState by remember {
                    derivedStateOf {
                        collectedData.idDocumentFront?.let {
                            DocumentUploadUIState.Done
                        } ?: run {
                            when (frontUploadState.highResResult.status) {
                                Status.SUCCESS -> DocumentUploadUIState.Loading
                                Status.LOADING -> DocumentUploadUIState.Loading
                                Status.IDLE -> DocumentUploadUIState.Idle
                                // place holder, Error will redirect to ErrorScreen
                                Status.ERROR -> DocumentUploadUIState.Idle
                            }
                        }
                    }
                }

                var shouldShowFrontDialog by remember { mutableStateOf(false) }
                SingleSideUploadRow(
                    modifier = Modifier.testTag(FRONT_ROW_TAG),
                    uploadUiState = frontUploadedUiState,
                    uploadInfo = frontInfo,
                ) { shouldShowFrontDialog = true }

                if (shouldShowFrontDialog) {
                    UploadImageDialog(
                        uploadInfo = frontInfo,
                        shouldShowTakePhoto = shouldShowTakePhoto,
                        shouldShowChoosePhoto = shouldShowChoosePhoto,
                        onPhotoSelected = { uploadMethod ->
                            when (uploadMethod) {
                                UploadMethod.TAKE_PHOTO -> {
                                    identityViewModel.imageHandler.takePhotoFront(localContext)
                                }
                                UploadMethod.CHOOSE_PHOTO -> {
                                    identityViewModel.imageHandler.chooseImageFront()
                                }
                            }
                        },
                        onDismissRequest = { shouldShowFrontDialog = false }
                    ) { shouldShowFrontDialog = false }
                }

                // decide should show back or not
                val shouldShowBack by remember {
                    derivedStateOf {
                        // If front and back are collected, it means the user has already uploaded both sides,
                        // should show both sides done and enable continue button
                        if (collectedData.idDocumentFront != null && collectedData.idDocumentBack != null) {
                            true
                        }
                        // Otherwise show back when all the follows are true
                        //  * backInfo is not null - e.g not a passport scan where backInfo is null
                        //  * collectedData.idDocumentFront not null - front is already scanned
                        //  * missing BACK - front already scanned and server returns missing back
                        else {
                            backInfo != null && collectedData.idDocumentFront != null && missings.contains(
                                Requirement.IDDOCUMENTBACK
                            )
                        }
                    }
                }

                if (shouldShowBack) {
                    var shouldShowBackDialog by remember { mutableStateOf(false) }
                    Divider(modifier = Modifier.padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin)))
                    val backUploadedUiState by remember {
                        derivedStateOf {
                            collectedData.idDocumentBack?.let {
                                DocumentUploadUIState.Done
                            } ?: run {
                                when (backUploadState.highResResult.status) {
                                    Status.SUCCESS -> DocumentUploadUIState.Loading
                                    Status.LOADING -> DocumentUploadUIState.Loading
                                    Status.IDLE -> DocumentUploadUIState.Idle
                                    // place holder, Error will redirect to ErrorScreen
                                    Status.ERROR -> DocumentUploadUIState.Idle
                                }
                            }
                        }
                    }
                    SingleSideUploadRow(
                        modifier = Modifier.testTag(BACK_ROW_TAG),
                        uploadUiState = backUploadedUiState,
                        uploadInfo = requireNotNull(backInfo),
                    ) { shouldShowBackDialog = true }

                    if (shouldShowBackDialog) {
                        UploadImageDialog(
                            uploadInfo = backInfo,
                            shouldShowTakePhoto = shouldShowTakePhoto,
                            shouldShowChoosePhoto = shouldShowChoosePhoto,
                            onPhotoSelected = { uploadMethod ->
                                when (uploadMethod) {
                                    UploadMethod.TAKE_PHOTO -> {
                                        identityViewModel.imageHandler.takePhotoBack(localContext)
                                    }
                                    UploadMethod.CHOOSE_PHOTO -> {
                                        identityViewModel.imageHandler.chooseImageBack()
                                    }
                                }
                            },
                            onDismissRequest = { shouldShowBackDialog = false }
                        ) { shouldShowBackDialog = false }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // enable LoadingButton when collectedData has both front and back
                var continueButtonState by remember(missings) {
                    mutableStateOf(
                        if (missings.contains(Requirement.IDDOCUMENTFRONT) || missings.contains(
                                Requirement.IDDOCUMENTBACK
                            )
                        ) {
                            LoadingButtonState.Disabled
                        } else {
                            LoadingButtonState.Idle
                        }
                    )
                }
                LoadingButton(
                    modifier = Modifier.testTag(UPLOAD_SCREEN_CONTINUE_BUTTON_TAG),
                    text = stringResource(id = R.string.kontinue).uppercase(),
                    state = continueButtonState
                ) {
                    continueButtonState = LoadingButtonState.Loading
                    identityViewModel.navigateToSelfieOrSubmit(
                        navController,
                        route
                    )
                }
            }
        }
    }
}

@Composable
internal fun UploadImageDialog(
    uploadInfo: DocumentUploadSideInfo,
    shouldShowTakePhoto: Boolean,
    shouldShowChoosePhoto: Boolean,
    onPhotoSelected: (UploadMethod) -> Unit,
    onDismissRequest: () -> Unit,
    onUploadMethodSelected: () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface,
            contentColor = contentColorFor(backgroundColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        vertical = dimensionResource(id = R.dimen.item_vertical_margin)
                    )
            ) {
                Text(
                    modifier = Modifier.padding(
                        bottom = dimensionResource(
                            id = R.dimen.item_vertical_margin
                        ),
                        start = 24.dp,
                        end = 24.dp
                    ),
                    text = getTitleFromScanType(scanType = uploadInfo.scanType),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.Bold
                )
                if (shouldShowTakePhoto) {
                    DialogListItem(
                        text = stringResource(id = R.string.take_photo),
                        testTag = SHOULD_SHOW_TAKE_PHOTO_TAG
                    ) {
                        onUploadMethodSelected()
                        onPhotoSelected(UploadMethod.TAKE_PHOTO)
                    }
                }
                if (shouldShowChoosePhoto) {
                    DialogListItem(
                        text = stringResource(id = R.string.choose_file),
                        testTag = SHOULD_SHOW_CHOOSE_PHOTO_TAG
                    ) {
                        onUploadMethodSelected()
                        onPhotoSelected(UploadMethod.CHOOSE_PHOTO)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogListItem(
    text: String,
    testTag: String,
    onSelected: () -> Unit,
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .height(
                dimensionResource(
                    com.google.android.material.R.dimen.abc_list_item_height_small_material
                )
            )
            .clickable { onSelected() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            text = text,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun getTitleFromScanType(scanType: IdentityScanState.ScanType): String {
    return when (scanType) {
        IdentityScanState.ScanType.ID_FRONT -> {
            stringResource(R.string.upload_dialog_title_id_front)
        }
        IdentityScanState.ScanType.ID_BACK -> {
            stringResource(R.string.upload_dialog_title_id_back)
        }
        IdentityScanState.ScanType.DL_FRONT -> {
            stringResource(R.string.upload_dialog_title_dl_front)
        }
        IdentityScanState.ScanType.DL_BACK -> {
            stringResource(R.string.upload_dialog_title_dl_back)
        }
        IdentityScanState.ScanType.PASSPORT -> {
            stringResource(R.string.upload_dialog_title_passport)
        }
        else -> {
            throw java.lang.IllegalArgumentException("invalid scan type: $scanType")
        }
    }
}

private enum class DocumentUploadUIState {
    Idle, Loading, Done
}

@Composable
private fun SingleSideUploadRow(
    modifier: Modifier = Modifier,
    uploadUiState: DocumentUploadUIState,
    uploadInfo: DocumentUploadSideInfo,
    onSelectButtonClicked: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(bottom = dimensionResource(id = R.dimen.item_vertical_margin))
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(id = uploadInfo.descriptionRes),
            modifier = Modifier.align(CenterVertically)
        )
        when (uploadUiState) {
            DocumentUploadUIState.Idle -> {
                TextButton(onClick = {
                    onSelectButtonClicked()
                }) {
                    Text(text = stringResource(id = R.string.select).uppercase())
                }
            }
            DocumentUploadUIState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .align(CenterVertically),
                    strokeWidth = 3.dp
                )
            }
            DocumentUploadUIState.Done -> {
                Image(
                    painter = painterResource(id = R.drawable.check_mark),
                    contentDescription = stringResource(id = uploadInfo.checkmarkContentDescriptionRes),
                    modifier = Modifier
                        .height(18.dp)
                        .width(18.dp)
                        .align(CenterVertically),
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
                )
            }
        }
    }
}
