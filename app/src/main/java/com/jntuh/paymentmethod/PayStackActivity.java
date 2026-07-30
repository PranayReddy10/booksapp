package com.jntuh.paymentmethod;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.jntuh.books.R;
import com.jntuh.books.databinding.ActivityPayStackBinding;
import com.jntuh.util.Method;
import com.tenbis.library.consts.CardType;
import com.tenbis.library.listeners.OnCreditCardStateChanged;
import com.tenbis.library.models.CreditCard;
import com.tenbis.library.views.CompactCreditCardInput;

import org.jetbrains.annotations.NotNull;

import co.paystack.android.Paystack;
import co.paystack.android.PaystackSdk;
import co.paystack.android.Transaction;
import co.paystack.android.model.Card;
import co.paystack.android.model.Charge;


public class PayStackActivity extends AppCompatActivity implements OnCreditCardStateChanged {

    String planId, planPrice, planCurrency, planGateway, planGateWayText, payStackPublicKey;
    Method method;
    ProgressDialog pDialog;
    CompactCreditCardInput creditCardInput;
    CreditCard creditCard;
    boolean isCardValid = false;
    ActivityPayStackBinding viewPayStackBinding;
    boolean isRent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        viewPayStackBinding = ActivityPayStackBinding.inflate(getLayoutInflater());
        setContentView(viewPayStackBinding.getRoot());
        PaystackSdk.initialize(this);

        method = new Method(PayStackActivity.this);

        viewPayStackBinding.toolbarMain.tvToolbarTitle.setText(getString(R.string.payment_paystack));
         viewPayStackBinding.toolbarMain.imageArrowBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        pDialog = new ProgressDialog(this,R.style.MyAlertDialogStyle);

        Intent intent = getIntent();
        planId = intent.getStringExtra("planId");
        planPrice = intent.getStringExtra("planPrice");
        planCurrency = intent.getStringExtra("planCurrency");
        planGateway = intent.getStringExtra("planGateway");
        planGateWayText = intent.getStringExtra("planGatewayText");
        payStackPublicKey = intent.getStringExtra("payStackPublicKey");
        if (intent.hasExtra("isRent")){
            isRent=intent.getBooleanExtra("isRent",false);
        }

        PaystackSdk.setPublicKey(payStackPublicKey);

        creditCardInput = findViewById(R.id.compact_credit_card_input);
        creditCardInput.addOnCreditCardStateChangedListener(this);

        viewPayStackBinding.btnPay.setOnClickListener(view -> {
            if (method.isNetworkAvailable()) {
                if (isCardValid && creditCard != null) {
                    performCharge(method.getUserEmail());
                }
            } else {
                Toast.makeText(PayStackActivity.this, getString(R.string.internet_connection), Toast.LENGTH_SHORT).show();
            }
        });


    }

    public void performCharge(String email) {
        showProgressDialog();
        Charge charge = new Charge();
        charge.setCard(loadCardFromForm());
        charge.setEmail(email);
        double amount = Double.parseDouble(planPrice);
        charge.setAmount((int) amount * 100);
        PaystackSdk.chargeCard(PayStackActivity.this, charge, new Paystack.TransactionCallback() {
            @Override
            public void onSuccess(Transaction transaction) {
                dismissProgressDialog();
                if (method.isNetworkAvailable()) {
                    new com.jntuh.paymentmethod.Transaction(PayStackActivity.this)
                            .purchasedItem(planId,method.getUserId(), transaction.getReference(), planGateway,isRent);
                } else {
                    showError(getString(R.string.internet_connection));
                }
            }

            @Override
            public void beforeValidate(Transaction transaction) {

            }

            @Override
            public void showLoading(Boolean isProcessing) {

            }

            @Override
            public void onError(Throwable error, Transaction transaction) {
                dismissProgressDialog();
                showError(error.getMessage());
            }
        });
    }


    public void showProgressDialog() {
        pDialog.setMessage(getString(R.string.loading));
        pDialog.setIndeterminate(false);
        pDialog.setCancelable(true);
        pDialog.show();
    }

    public void dismissProgressDialog() {
        if (pDialog != null && pDialog.isShowing())
            pDialog.dismiss();
    }

    private void showError(String Title) {
        new AlertDialog.Builder(PayStackActivity.this)
                .setTitle(getString(R.string.pay_stack_error_1))
                .setMessage(Title)
                .setIcon(R.mipmap.ic_launcher_round)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {

                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> {
                    // do nothing
                })
                .show();
    }

    private Card loadCardFromForm() {
        return new Card.Builder(creditCard.getCardNumber(), creditCard.getExpiryMonth(), creditCard.getExpiryYear(), creditCard.getCvv()).build();
    }

    @Override
    public void onCreditCardCvvValid(@NotNull String s) {

    }

    @Override
    public void onCreditCardExpirationDateValid(int i, int i1) {

    }

    @Override
    public void onCreditCardNumberValid(@NotNull String s) {

    }

    @Override
    public void onCreditCardTypeFound(@NotNull CardType cardType) {

    }

    @Override
    public void onCreditCardValid(@NotNull CreditCard creditCard) {
        isCardValid = true;
        this.creditCard = creditCard;
    }

    @Override
    public void onInvalidCardTyped() {
        isCardValid = false;
    }
}
