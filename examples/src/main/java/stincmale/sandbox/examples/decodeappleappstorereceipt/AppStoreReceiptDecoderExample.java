package stincmale.sandbox.examples.decodeappleappstorereceipt;

import stincmale.sandbox.examples.decodeappleappstorereceipt.asn1.receiptmodule.InAppReceipt;
import stincmale.sandbox.examples.decodeappleappstorereceipt.asn1.receiptmodule.Payload;

final class AppStoreReceiptDecoderExample {
    /**
     * A Base64-encoded
     * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html">
     * App Store receipt</a>.
     * I would put here a receipt from our project, but I am not sure if I can do this.
     */
    private static final String RECEIPT_BASE64 = "<put your Base64-encoded receipt data here>";

    public static final void main(final String... args) {
        final Payload payload = AppStoreReceiptUtil.decodeReceiptFromBase64(RECEIPT_BASE64);
        final String productId = "com.company.application.subscription";
        final InAppReceipt inAppReceipt =
                AppStoreReceiptUtil.inAppReceiptByProductId(payload, productId).orElseThrow();
        System.out.println(AppStoreReceiptUtil.purchaseDate(inAppReceipt));
        System.out.println(AppStoreReceiptUtil.toString(payload, true));
    }

    private AppStoreReceiptDecoderExample() {
        throw new AssertionError();
    }
}
