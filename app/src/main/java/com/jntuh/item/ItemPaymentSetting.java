package com.jntuh.item;

public class ItemPaymentSetting {

    private String currencyCode;
    private String stripePublisherKey;
    private String razorPayKey;
    private String payStackPublicKey;
    private boolean isPayPal = false;
    private boolean isPayPalSandbox = false;
    private boolean isStripe = false;
    private boolean isRazorPay = false;
    private boolean isPayStack = false;
    private boolean isPayUMoney = false;
    private boolean isPayUMoneySandbox = false;
    private String payUMoneyMerchantId;
    private String payUMoneyMerchantKey;
    private boolean isFlutterWave = false;
    private String flutterWavePublicKey;
    private String flutterWaveSecretKey;
    private String flutterWaveEncryptionKey;
    private boolean isCinetPay = false;
    private String cinetPayApiKey;
    private String cinetPaySiteId;
    private boolean isManual = false;
    private String ManualInfo;
    private boolean isSslCommerzSandbox = false;
    private boolean isSslCommerz = false;
    private String sslStoreId;
    private String sslStorePass;
    public boolean isFlutterWave() {
        return isFlutterWave;
    }

    public void setFlutterWave(boolean flutterWave) {
        isFlutterWave = flutterWave;
    }

    public String getFlutterWavePublicKey() {
        return flutterWavePublicKey;
    }

    public void setFlutterWavePublicKey(String flutterWavePublicKey) {
        this.flutterWavePublicKey = flutterWavePublicKey;
    }

    public String getFlutterWaveSecretKey() {
        return flutterWaveSecretKey;
    }

    public void setFlutterWaveSecretKey(String flutterWaveSecretKey) {
        this.flutterWaveSecretKey = flutterWaveSecretKey;
    }

    public String getFlutterWaveEncryptionKey() {
        return flutterWaveEncryptionKey;
    }

    public void setFlutterWaveEncryptionKey(String flutterWaveEncryptionKey) {
        this.flutterWaveEncryptionKey = flutterWaveEncryptionKey;
    }
     public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public boolean isPayPal() {
        return isPayPal;
    }

    public void setPayPal(boolean payPal) {
        isPayPal = payPal;
    }

    public boolean isStripe() {
        return isStripe;
    }

    public void setStripe(boolean stripe) {
        isStripe = stripe;
    }

    public boolean isRazorPay() {
        return isRazorPay;
    }

    public void setRazorPay(boolean razorPay) {
        isRazorPay = razorPay;
    }


    public boolean isPayPalSandbox() {
        return isPayPalSandbox;
    }

    public void setPayPalSandbox(boolean payPalSandbox) {
        isPayPalSandbox = payPalSandbox;
    }

    public String getStripePublisherKey() {
        return stripePublisherKey;
    }

    public void setStripePublisherKey(String stripePublisherKey) {
        this.stripePublisherKey = stripePublisherKey;
    }

    public String getRazorPayKey() {
        return razorPayKey;
    }

    public void setRazorPayKey(String razorPayKey) {
        this.razorPayKey = razorPayKey;
    }

    public String getPayStackPublicKey() {
        return payStackPublicKey;
    }

    public void setPayStackPublicKey(String payStackPublicKey) {
        this.payStackPublicKey = payStackPublicKey;
    }

    public boolean isPayStack() {
        return isPayStack;
    }

    public void setPayStack(boolean payStack) {
        isPayStack = payStack;
    }

    public boolean isPayUMoney() {
        return isPayUMoney;
    }

    public void setPayUMoney(boolean payUMoney) {
        isPayUMoney = payUMoney;
    }

    public boolean isPayUMoneySandbox() {
        return isPayUMoneySandbox;
    }

    public void setPayUMoneySandbox(boolean payUMoneySandbox) {
        isPayUMoneySandbox = payUMoneySandbox;
    }

    public String getPayUMoneyMerchantId() {
        return payUMoneyMerchantId;
    }

    public void setPayUMoneyMerchantId(String payUMoneyMerchantId) {
        this.payUMoneyMerchantId = payUMoneyMerchantId;
    }

    public String getPayUMoneyMerchantKey() {
        return payUMoneyMerchantKey;
    }

    public void setPayUMoneyMerchantKey(String payUMoneyMerchantKey) {
        this.payUMoneyMerchantKey = payUMoneyMerchantKey;
    }
    public boolean isCinetPay() {
        return isCinetPay;
    }

    public void setCinetPay(boolean cinetPay) {
        isCinetPay = cinetPay;
    }

    public String getCinetPayApiKey() {
        return cinetPayApiKey;
    }

    public void setCinetPayApiKey(String cinetPayApiKey) {
        this.cinetPayApiKey = cinetPayApiKey;
    }

    public String getCinetPaySiteId() {
        return cinetPaySiteId;
    }

    public void setCinetPaySiteId(String cinetPaySiteId) {
        this.cinetPaySiteId = cinetPaySiteId;
    }

    public boolean isManual() {
        return isManual;
    }

    public void setManual(boolean manual) {
        isManual = manual;
    }

    public String getManualInfo() {
        return ManualInfo;
    }

    public void setManualInfo(String manualInfo) {
        ManualInfo = manualInfo;
    }

    public boolean isSslCommerzSandbox() {
        return isSslCommerzSandbox;
    }

    public void setSslCommerzSandbox(boolean sslCommerzSandbox) {
        isSslCommerzSandbox = sslCommerzSandbox;
    }

    public boolean isSslCommerz() {
        return isSslCommerz;
    }

    public void setSslCommerz(boolean sslCommerz) {
        isSslCommerz = sslCommerz;
    }

    public String getSslStoreId() {
        return sslStoreId;
    }

    public void setSslStoreId(String sslStoreId) {
        this.sslStoreId = sslStoreId;
    }

    public String getSslStorePass() {
        return sslStorePass;
    }

    public void setSslStorePass(String sslStorePass) {
        this.sslStorePass = sslStorePass;
    }
}
