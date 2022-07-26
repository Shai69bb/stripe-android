package com.stripe.android.paymentsheet.model

import com.google.common.truth.Truth
import com.stripe.android.model.CardBrand
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParamsFixtures
import com.stripe.android.model.PaymentMethodOptionsParams
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.addresselement.AddressDetails
import com.stripe.android.paymentsheet.addresselement.toConfirmPaymentIntentShipping
import org.junit.Test

class ConfirmPaymentIntentParamsFactoryTest {
    private val factory = ConfirmPaymentIntentParamsFactory(
        PaymentIntentClientSecret(CLIENT_SECRET),
        null
    )

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = OffSession`() {
        Truth.assertThat(
            factory.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestReuse
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.OffSession
                )
            )
        )
    }

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = blank`() {
        Truth.assertThat(
            factory.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.RequestNoReuse
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card(
                    setupFutureUsage = ConfirmPaymentIntentParams.SetupFutureUsage.Blank
                )
            )
        )
    }

    @Test
    fun `create() with new card when savePaymentMethod is true should create params with setupFutureUsage = null`() {
        Truth.assertThat(
            factory.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card()
            )
        )
    }

    @Test
    fun `create() with card and shippingDetails sets shipping field`() {
        val shippingDetails = AddressDetails(
            name = "Test",
            address = PaymentSheet.Address(
                line1 = "line1",
                city = "city"
            ),
            phoneNumber = "5555555555"
        )
        val factoryWithConfig = ConfirmPaymentIntentParamsFactory(
            PaymentIntentClientSecret(CLIENT_SECRET),
            PaymentSheet.Configuration(
                merchantDisplayName = "some merchant",
                shippingDetails = shippingDetails
            )
        )

        Truth.assertThat(
            factoryWithConfig.create(
                paymentSelection = PaymentSelection.New.Card(
                    PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                    CardBrand.Visa,
                    customerRequestedSave = PaymentSelection.CustomerRequestedSave.NoRequest
                )
            )
        ).isEqualTo(
            ConfirmPaymentIntentParams.createWithPaymentMethodCreateParams(
                paymentMethodCreateParams = PaymentMethodCreateParamsFixtures.DEFAULT_CARD,
                clientSecret = CLIENT_SECRET,
                setupFutureUsage = null,
                paymentMethodOptions = PaymentMethodOptionsParams.Card(),
                shipping = shippingDetails.toConfirmPaymentIntentShipping()
            )
        )
    }

    private companion object {
        private const val CLIENT_SECRET = com.stripe.android.paymentsheet.PaymentSheetFixtures.CLIENT_SECRET
    }
}
