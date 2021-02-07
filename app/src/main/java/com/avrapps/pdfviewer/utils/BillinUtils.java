package com.avrapps.pdfviewer.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.avrapps.pdfviewer.constants.AppConstants;

import java.util.ArrayList;

import static com.android.billingclient.api.BillingClient.BillingResponseCode.OK;
import static com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED;
import static com.android.billingclient.api.Purchase.PurchaseState.PURCHASED;

public class BillinUtils {


    private static String TAG = "Purchase";
    final Activity activity;
    BillingClient billingClient;

    public BillinUtils(Activity activity) {
        this.activity = activity;
    }

    public static boolean isApplicationBought(Context activity) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        return sp.getBoolean(AppConstants.IS_BOUGHT, false);
    }

    public void buyApplication() {
        setBillingClient();
        startPurchaseProcess();
       /* if(billingClient.isReady()) {
            startPurchaseProcess();
        } else{
            MessagingUtility.showDialogWithTitleMessageDismiss(activity,"Service Unavailable","InApp Purchase not Available. If your device support play services, please check your internet connection and retry purchase");
        }*/
    }

    public void setBillingClient() {
        billingClient = BillingClient
                .newBuilder(activity)
                .enablePendingPurchases()
                .setListener((billingResult, purchases) -> {
                    Log.i(TAG, "Billing client Listener" + billingResult + purchases);
                    if (billingResult.getResponseCode() == OK && purchases != null) {
                        for (Purchase purchase : purchases) {
                            handlePurchase(purchase);
                        }
                    } else if (billingResult.getResponseCode() == USER_CANCELED) {
                        Log.i(TAG, "User cancelled purchase flow.");
                    } else {
                        Log.i(TAG, "onPurchaseUpdated error:" + billingResult.getResponseCode());
                    }
                })
                .build();
    }

    private void startPurchaseProcess() {
        Log.i(TAG, "Billing client initiated");
        billingClient.startConnection(new BillingClientStateListener() {

            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == OK) {
                    Log.i(TAG, "Billing client successfully set up");
                    queryPurchases();
                } else {
                    String message = "Inapp Billing Not supported at this time. Reason code:" + billingResult.getResponseCode();
                    Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.i(TAG, "Billing service disconnected");
            }
        });
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == PURCHASED) {
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken()).build();
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> updatePreferencePurchased(purchase));
            }
        }
    }

    private void updatePreferencePurchased(Purchase purchase) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean(AppConstants.IS_BOUGHT, true);
        editor.putString("purchase", purchase.toString());
        editor.apply();
        editor.commit();
    }

    private void queryPurchases() {
        if (!billingClient.isReady()) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready");
            return;
        }
        // Query for existing in app products that have been purchased. This does NOT include subscriptions.
        Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (result.getPurchasesList() == null) {
            Log.i(TAG, "No existing in app purchases found.");
        } else {
            Log.i(TAG, "Existing purchases: " + result.getPurchasesList());
            for (Purchase purchase : result.getPurchasesList())
                if (purchase.getSku().equals("pdf_viewer_lite") && purchase.getPurchaseState() == PURCHASED && purchase.isAcknowledged()) {
                    MessagingUtility.showDialogWithPositiveOption(activity, "Already Purchased", "You have already purchased the app in the past. We have restored your purchase");
                    updatePreferencePurchased(purchase);
                    return;
                }
        }
        queryOneTimeProducts();
    }

    private void queryOneTimeProducts() {
        ArrayList<String> skuListToQuery = new ArrayList<>();

        skuListToQuery.add("pdf_viewer_lite");
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder()
                .setSkusList(skuListToQuery)
                .setType(BillingClient.SkuType.INAPP);
        // SkuType.INAPP refers to 'managed products' or one time purchases.

        billingClient.querySkuDetailsAsync(params.build(), (billingResult, skuDetails) -> {
            Log.i(TAG, "onSkuDetailsResponse " + billingResult.getResponseCode() + skuDetails);
            if (skuDetails != null && skuDetails.size() != 0) {
                for (SkuDetails skuDetail : skuDetails) {
                    Log.i(TAG, skuDetail.toString());
                    launchPurchaseFlow(skuDetail);
                    //skuDetail.getPrice() + " "+ skuDetail.getPriceCurrencyCode();
                    break;
                }
            } else {
                Log.i(TAG, "No skus found from query");
            }
        });
    }

    private void launchPurchaseFlow(SkuDetails skuDetails) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(skuDetails)
                .build();
        BillingResult responseCode = billingClient.launchBillingFlow(activity, flowParams);
        Log.i(TAG, "launchPurchaseFlow result " + responseCode);
    }

}
