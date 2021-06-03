package com.stripe.android.googlepaysheet

import com.google.android.gms.wallet.WalletConstants

internal enum class GooglePaySheetEnvironment(
    internal val value: Int
) {
    Production(WalletConstants.ENVIRONMENT_PRODUCTION),
    Test(WalletConstants.ENVIRONMENT_TEST)
}
