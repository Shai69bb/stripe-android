package com.stripe.android.paymentsheet

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stripe.android.paymentsheet.ui.LpmSelectorText
import com.stripe.android.ui.core.forms.resources.LpmRepository.SupportedPaymentMethod
import com.stripe.android.ui.core.getBorderStroke
import com.stripe.android.ui.core.paymentsColors
import com.stripe.android.uicore.image.StripeImage
import com.stripe.android.uicore.image.StripeImageLoader

private object Spacing {
    val cardLeadingInnerPadding = 12.dp
    val carouselOuterPadding = 20.dp
    val carouselInnerPadding = 12.dp
    val iconSize = 28.dp
}

@VisibleForTesting
const val TEST_TAG_LIST = "PaymentMethodsUITestTag"

@Composable
internal fun PaymentMethodsUI(
    paymentMethods: List<SupportedPaymentMethod>,
    selectedIndex: Int,
    isEnabled: Boolean,
    onItemSelectedListener: (SupportedPaymentMethod) -> Unit,
    imageLoader: StripeImageLoader,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
) {
    LaunchedEffect(selectedIndex) {
        state.animateScrollToItem(selectedIndex)
    }

    BoxWithConstraints(
        modifier = modifier.testTag(TEST_TAG_LIST + "1")
    ) {
        val viewWidth = rememberViewWidth(
            this.maxWidth,
            paymentMethods.size
        )

        LazyRow(
            state = state,
            contentPadding = PaddingValues(horizontal = Spacing.carouselOuterPadding),
            horizontalArrangement = Arrangement.spacedBy(Spacing.carouselInnerPadding),
            userScrollEnabled = isEnabled,
            modifier = Modifier.testTag(TEST_TAG_LIST)
        ) {
            itemsIndexed(items = paymentMethods) { index, item ->
                val iconUrl = if (isSystemInDarkTheme() && item.darkThemeIconUrl != null) {
                    item.darkThemeIconUrl
                } else {
                    item.lightThemeIconUrl
                }
                PaymentMethodUI(
                    modifier = Modifier.testTag(
                        TEST_TAG_LIST + stringResource(item.displayNameResource)
                    ),
                    minViewWidth = viewWidth,
                    iconRes = item.iconResource,
                    iconUrl = iconUrl,
                    imageLoader = imageLoader,
                    title = stringResource(item.displayNameResource),
                    isSelected = index == selectedIndex,
                    isEnabled = isEnabled,
                    tintOnSelected = item.tintIconOnSelection,
                    itemIndex = index,
                    onItemSelectedListener = {
                        onItemSelectedListener(paymentMethods[it])
                    }
                )
            }
        }
    }
}

@Composable
private fun rememberViewWidth(
    maxWidth: Dp,
    numberOfPaymentMethods: Int
) = remember(maxWidth, numberOfPaymentMethods) {
    calculateViewWidth(maxWidth, numberOfPaymentMethods)
}

internal fun calculateViewWidth(
    maxWidth: Dp,
    numberOfPaymentMethods: Int
): Dp {
    val targetWidth = maxWidth - (Spacing.carouselOuterPadding * 2)
    val minItemWidth = 100.dp

    val minimumCardsWidth = minItemWidth * numberOfPaymentMethods
    val spacingWidth = Spacing.carouselInnerPadding * (numberOfPaymentMethods - 1)
    val minimumContentWidth = minimumCardsWidth + spacingWidth

    val viewWidth = if (minimumContentWidth <= targetWidth) {
        // Stretch cards to fill entire width
        (targetWidth - spacingWidth) / numberOfPaymentMethods
    } else {
        computeItemWidthWhenExceedingMaxWidth(
            availableWidth = targetWidth,
            minItemWidth = minItemWidth,
            spacing = Spacing.carouselInnerPadding,
        )
    }
    return viewWidth
}

private fun computeItemWidthWhenExceedingMaxWidth(
    availableWidth: Dp,
    minItemWidth: Dp,
    spacing: Dp,
): Dp {
    val itemWithSpacing = minItemWidth + spacing

    val remainingWidthAfterAddingFirstCard = availableWidth - minItemWidth
    val numberOfAdditionalCards = (remainingWidthAfterAddingFirstCard / itemWithSpacing).toInt()

    val visibleCards = numberOfAdditionalCards + 1
    val overallSpacing = spacing * numberOfAdditionalCards

    return (availableWidth - overallSpacing) / visibleCards
}

@Composable
internal fun PaymentMethodUI(
    minViewWidth: Dp,
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    title: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    tintOnSelected: Boolean,
    itemIndex: Int,
    modifier: Modifier = Modifier,
    onItemSelectedListener: (Int) -> Unit
) {
    val color = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.paymentsColors.onComponent
    }

    Card(
        modifier = modifier
            .alpha(alpha = if (isEnabled) 1.0F else 0.6F)
            .height(60.dp)
            .widthIn(min = minViewWidth),
        shape = MaterialTheme.shapes.medium,
        backgroundColor = MaterialTheme.paymentsColors.component,
        border = MaterialTheme.getBorderStroke(isSelected),
        elevation = if (isSelected) 1.5.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .selectable(
                    selected = isSelected,
                    enabled = isEnabled,
                    onClick = {
                        onItemSelectedListener(itemIndex)
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .height(Spacing.iconSize)
                    .padding(
                        start = Spacing.cardLeadingInnerPadding,
                        top = Spacing.cardLeadingInnerPadding,
                    )
            ) {
                PaymentMethodIconUi(
                    iconRes = iconRes,
                    iconUrl = iconUrl,
                    imageLoader = imageLoader,
                    color = color,
                    tintOnSelected = tintOnSelected
                )
            }

            LpmSelectorText(
                text = title,
                isEnabled = isEnabled,
                textColor = color,
                modifier = Modifier.padding(
                    top = 6.dp,
                    start = Spacing.cardLeadingInnerPadding,
                    end = Spacing.cardLeadingInnerPadding,
                )
            )
        }
    }
}

@Composable
private fun PaymentMethodIconUi(
    iconRes: Int,
    iconUrl: String?,
    imageLoader: StripeImageLoader,
    tintOnSelected: Boolean,
    color: Color,
) {
    val colorFilter = remember(tintOnSelected, color) {
        if (tintOnSelected) {
            ColorFilter.tint(color)
        } else {
            null
        }
    }

    if (iconUrl != null) {
        StripeImage(
            url = iconUrl,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )
    } else {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            colorFilter = colorFilter,
        )
    }
}
