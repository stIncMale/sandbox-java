package stincmale.sandbox.examples.parseappstorereceipt;

import stincmale.sandbox.examples.parseappstorereceipt.apple.asn1.receiptmodule.InAppReceipt;
import stincmale.sandbox.examples.parseappstorereceipt.apple.asn1.receiptmodule.Payload;

final class AppStoreReceiptParserExample {
  /**
   * A Base64-encoded App Store receipt
   * (https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html).
   * I would put here a receipt from our project, but I am not sure if I can do this.
   */
  private static final String RECEIPT_BASE64 = "<put your Base64-encoded receipt data here>";

  public static final void main(final String... args) {
    final Payload payload = AppStoreReceiptUtil.decodeReceiptFromBase64(RECEIPT_BASE64);
    final String productId = "com.company.application.subscription";
    final InAppReceipt inAppReceipt = AppStoreReceiptUtil.getInAppReceiptByProductId(payload, productId).orElseThrow();
    System.out.println(AppStoreReceiptUtil.getPurchaseDate(inAppReceipt));
    System.out.println(AppStoreReceiptUtil.toString(payload, true));
  }
}
