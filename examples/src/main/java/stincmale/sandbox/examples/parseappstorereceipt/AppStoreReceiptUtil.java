package stincmale.sandbox.examples.parseappstorereceipt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.security.Security;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openmuc.jasn1.ber.types.BerInteger;
import org.openmuc.jasn1.ber.types.BerOctetString;
import org.openmuc.jasn1.ber.types.string.BerIA5String;
import org.openmuc.jasn1.ber.types.string.BerUTF8String;
import stincmale.sandbox.examples.parseappstorereceipt.apple.asn1.receiptmodule.InAppAttribute;
import stincmale.sandbox.examples.parseappstorereceipt.apple.asn1.receiptmodule.InAppReceipt;
import stincmale.sandbox.examples.parseappstorereceipt.apple.asn1.receiptmodule.Payload;
import stincmale.sandbox.examples.parseappstorereceipt.apple.asn1.receiptmodule.ReceiptAttribute;
import static com.google.common.base.Preconditions.checkNotNull;
import static stincmale.sandbox.examples.parseappstorereceipt.AppStoreReceiptUtil.InAppReceiptAttributeType.PURCHASE_DATE;
import static stincmale.sandbox.examples.parseappstorereceipt.AppStoreReceiptUtil.ReceiptAttributeType.BUNDLE_IDENTIFIER;
import static stincmale.sandbox.examples.parseappstorereceipt.AppStoreReceiptUtil.ReceiptAttributeType.IN_APP_PURCHASE_RECEIPT;
import static stincmale.sandbox.examples.parseappstorereceipt.AppStoreReceiptUtil.InAppReceiptAttributeType.ORIGINAL_TRANSACTION_IDENTIFIER;
import static stincmale.sandbox.examples.parseappstorereceipt.AppStoreReceiptUtil.InAppReceiptAttributeType.PRODUCT_IDENTIFIER;
import static stincmale.sandbox.examples.parseappstorereceipt.AppStoreReceiptUtil.InAppReceiptAttributeType.SUBSCRIPTION_EXPIRATION_DATE;
import static stincmale.sandbox.examples.parseappstorereceipt.AppStoreReceiptUtil.InAppReceiptAttributeType.TRANSACTION_IDENTIFIER;

/**
 * Contains utility methods to work with
 * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html">
 * receipts issued by App Store</a>.
 * <p>
 * Note that upon initialization this class {@linkplain Security#addProvider(Provider) adds} a new {@link BouncyCastleProvider}.
 */
public final class AppStoreReceiptUtil {
  static {
    Security.addProvider(new BouncyCastleProvider());
  }

  /**
   * See {@link #decodeReceipt(byte[])}.
   *
   * @param receiptBase64 A receipt (see {@link #decodeReceipt(byte[])}) encoded with Base64 encoding.
   */
  public static final Payload decodeReceiptFromBase64(final String receiptBase64) {
    checkNotNull(receiptBase64, "The argument %s must not be null", receiptBase64);
    return decodeReceipt(Base64.getDecoder()
        .decode(receiptBase64.getBytes(StandardCharsets.UTF_8)));
  }

  /**
   * See {@link #decodeReceipt(byte[])}.
   *
   * @param receiptBase64 A receipt (see {@link #decodeReceipt(byte[])}) encoded with Base64 encoding.
   */
  public static final Payload decodeReceiptFromBase64(final byte[] receiptBase64) {
    checkNotNull(receiptBase64, "The argument %s must not be null", receiptBase64);
    return decodeReceipt(Base64.getDecoder()
        .decode(receiptBase64));
  }

  /**
   * Decodes receipt and extracts {@link Payload} from it.
   *
   * @param receipt A receipt that is described by Apple as a
   * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html">
   * "PKCS #7 container, as defined by RFC 2315, with its payload encoded using ASN.1 (Abstract Syntax Notation One), as defined by ITU-T X.690"</a>.
   *
   * @return Extracted {@link Payload} which
   * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html">
   * "is composed of a set of receipt attributes"</a>.
   */
  public static final Payload decodeReceipt(final byte[] receipt) {
    checkNotNull(receipt, "The argument %s must not be null", "receipt");
    final Payload payload;
    try {
      final CMSSignedData signedData = new CMSSignedData(receipt);
      final CMSTypedData signedContent = signedData.getSignedContent();
      final ByteArrayOutputStream signedDataStream = new ByteArrayOutputStream();
      signedContent.write(signedDataStream);
      final byte[] signedDataBytes = signedDataStream.toByteArray();
      payload = new Payload();
      payload.decode(new ByteArrayInputStream(signedDataBytes));
    } catch (final CMSException | IOException e) {
      throw new RuntimeException(e);
    }
    return payload;
  }

  /**
   * Finds an {@link InAppReceipt} with the specified transaction identifier.
   *
   * @param payload See {@link #decodeReceipt(byte[])}.
   * @param transactionId <a href=
   * "https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * The transaction identifier of the item that was purchased</a>.
   *
   * @return <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * The in-app purchase receipt</a>.
   */
  public static final Optional<InAppReceipt> getInAppReceiptByTransactionId(final Payload payload, final String transactionId) {
    checkNotNull(payload, "The argument %s must not be null", "payload");
    checkNotNull(transactionId, "The argument %s must not be null", "transactionId");
    return getReceiptAttributes(payload, IN_APP_PURCHASE_RECEIPT)
        .stream()
        .map(receiptAttribute -> receiptAttribute.getValue().value)
        .map(bytes -> {
          final InAppReceipt result = new InAppReceipt();
          try {
            result.decode(new ByteArrayInputStream(bytes));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return result;
        })
        .filter(inAppReceipt -> transactionId.equals(getTransactionId(inAppReceipt)))
        .findAny();
  }

  /**
   * Finds the {@linkplain #getPurchaseDate(InAppReceipt) newest} {@link InAppReceipt} with the specified product id.
   *
   * @param payload See {@link #decodeReceipt(byte[])}.
   * @param productId <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * The product identifier of the item that was purchased</a>.
   *
   * @return <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * The in-app purchase receipt</a>.
   */
  public static final Optional<InAppReceipt> getInAppReceiptByProductId(final Payload payload, final String productId) {
    checkNotNull(payload, "The argument %s must not be null", "payload");
    checkNotNull(productId, "The argument %s must not be null", "productId");
    return getReceiptAttributes(payload, IN_APP_PURCHASE_RECEIPT)
        .stream()
        .map(receiptAttribute -> receiptAttribute.getValue().value)
        .map(bytes -> {
          final InAppReceipt result = new InAppReceipt();
          try {
            result.decode(new ByteArrayInputStream(bytes));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return result;
        })
        .filter(inAppReceipt -> productId.equals(getProductId(inAppReceipt)))
        .sorted(Comparator.comparing(AppStoreReceiptUtil::getPurchaseDate)
            .reversed())//find the newest one
        .findFirst();
  }

  /**
   * Finds the {@linkplain #getSubscriptionExpirationDate(InAppReceipt) newest} {@link InAppReceipt} with the specified product id,
   * which belongs to a subscription.
   *
   * @param payload See {@link #decodeReceipt(byte[])}.
   * @param productId <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * The product identifier of the item that was purchased</a>.
   *
   * @return <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * The in-app purchase receipt</a>.
   */
  public static final Optional<InAppReceipt> getInAppReceiptBySubscriptionProductId(final Payload payload, final String productId) {
    checkNotNull(payload, "The argument %s must not be null", "payload");
    checkNotNull(productId, "The argument %s must not be null", "productId");
    return getReceiptAttributes(payload, IN_APP_PURCHASE_RECEIPT)
        .stream()
        .map(receiptAttribute -> receiptAttribute.getValue().value)
        .map(bytes -> {
          final InAppReceipt result = new InAppReceipt();
          try {
            result.decode(new ByteArrayInputStream(bytes));
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
          return result;
        })
        .filter(inAppReceipt -> productId.equals(getProductId(inAppReceipt)))
        .sorted(Comparator.comparing(AppStoreReceiptUtil::getSubscriptionExpirationDate)
            .reversed())//find the newest one
        .findFirst();
  }

  /**
   * Acts as {@link #getReceiptAttribute(Payload, ReceiptAttributeType)} for the attribute of type {@link ReceiptAttributeType#BUNDLE_IDENTIFIER}.
   */
  public static final String getBundleId(final Payload payload) {
    checkNotNull(payload, "The argument %s must not be null", "payload");
    return getReceiptAttribute(payload, BUNDLE_IDENTIFIER)
        .map(inAppAttribute -> decodeAsn1UTF8String(inAppAttribute.getValue()))
        .get();
  }

  /**
   * Acts as {@link #getInAppAttribute(InAppReceipt, InAppReceiptAttributeType)}
   * for the attribute of type {@link InAppReceiptAttributeType#TRANSACTION_IDENTIFIER}.
   */
  public static final String getTransactionId(final InAppReceipt inAppReceipt) {
    checkNotNull(inAppReceipt, "The argument %s must not be null", "inAppReceipt");
    return getInAppAttribute(inAppReceipt, TRANSACTION_IDENTIFIER)
        .map(inAppAttribute -> decodeAsn1UTF8String(inAppAttribute.getValue()))
        .get();
  }

  /**
   * Acts as {@link #getInAppAttribute(InAppReceipt, InAppReceiptAttributeType)}
   * for the attribute of type {@link InAppReceiptAttributeType#ORIGINAL_TRANSACTION_IDENTIFIER}.
   */
  public static final String getOriginalTransactionId(final InAppReceipt inAppReceipt) {
    checkNotNull(inAppReceipt, "The argument %s must not be null", "inAppReceipt");
    return getInAppAttribute(inAppReceipt, ORIGINAL_TRANSACTION_IDENTIFIER)
        .map(inAppAttribute -> decodeAsn1UTF8String(inAppAttribute.getValue()))
        .get();
  }

  /**
   * Acts as {@link #getInAppAttribute(InAppReceipt, InAppReceiptAttributeType)}
   * for the attribute of type {@link InAppReceiptAttributeType#PRODUCT_IDENTIFIER}.
   */
  public static final String getProductId(final InAppReceipt inAppReceipt) {
    checkNotNull(inAppReceipt, "The argument %s must not be null", "inAppReceipt");
    return getInAppAttribute(inAppReceipt, PRODUCT_IDENTIFIER)
        .map(inAppAttribute -> decodeAsn1UTF8String(inAppAttribute.getValue()))
        .get();
  }

  /**
   * Acts as {@link #getInAppAttribute(InAppReceipt, InAppReceiptAttributeType)}
   * for the attribute of type {@link InAppReceiptAttributeType#PURCHASE_DATE}.
   */
  public static final Instant getPurchaseDate(final InAppReceipt inAppReceipt) {
    checkNotNull(inAppReceipt, "The argument %s must not be null", "inAppReceipt");
    return getInAppAttribute(inAppReceipt, PURCHASE_DATE)
        .map(inAppAttribute -> decodeInstant(inAppAttribute.getValue()))
        .get();
  }

  /**
   * Acts as {@link #getInAppAttribute(InAppReceipt, InAppReceiptAttributeType)}
   * for the attribute of type {@link InAppReceiptAttributeType#SUBSCRIPTION_EXPIRATION_DATE}.
   */
  public static final Instant getSubscriptionExpirationDate(final InAppReceipt inAppReceipt) {
    checkNotNull(inAppReceipt, "The argument %s must not be null", "inAppReceipt");
    return getInAppAttribute(inAppReceipt, SUBSCRIPTION_EXPIRATION_DATE)
        .map(inAppAttribute -> {
          @Nullable final Instant result = decodeInstant(inAppAttribute.getValue());
          return result == null//one-off subscriptions do not have SUBSCRIPTION_EXPIRATION_DATE value
              ? Instant.EPOCH
              : result;
        })
        .get();
  }

  /**
   * Finds an {@link InAppAttribute} with the specified type.
   *
   * @param attributeType <a href=
   * "https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html">
   * The type of the attribute of the in-app purchase receipt</a>.
   * The correspondence between attributes and types is specified
   * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">here</a>.
   *
   * @return The <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * attribute of the in-app purchase receipt</a>.
   */
  public static final Optional<InAppAttribute> getInAppAttribute(final InAppReceipt inAppReceipt, final InAppReceiptAttributeType attributeType) {
    checkNotNull(inAppReceipt, "The argument %s must not be null", "inAppReceipt");
    checkNotNull(attributeType, "The argument %s must not be null", "attributeType");
    return inAppReceipt.getInAppAttribute()
        .stream()
        .filter(inAppAttribute -> inAppAttribute.getType()
            .intValue() == attributeType.getValue())
        .findAny();
  }

  /**
   * Finds an {@link ReceiptAttribute} with the specified type.
   *
   * @param payload See {@link #decodeReceipt(byte[])}.
   * @param attributeType <a href=
   * "https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html">
   * The type of the attribute of the receipt ({@code payload})</a>.
   * The correspondence between attributes and types is specified
   * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">here</a>.
   *
   * @return The <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * attribute of the the receipt</a>.
   */
  public static final Optional<ReceiptAttribute> getReceiptAttribute(final Payload payload, final ReceiptAttributeType attributeType) {
    checkNotNull(payload, "The argument %s must not be null", "payload");
    checkNotNull(attributeType, "The argument %s must not be null", "attributeType");
    return payload.getReceiptAttribute()
        .stream()
        .filter(receiptAttribute -> receiptAttribute.getType()
            .intValue() == attributeType.getValue())
        .findAny();
  }

  /**
   * Finds all {@link ReceiptAttribute}s with the specified type.
   *
   * @param payload See {@link #decodeReceipt(byte[])}.
   * @param attributeType <a href="
   * https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html">
   * The type of the attribute of the receipt ({@code payload})</a>.
   * The correspondence between attributes and types is specified
   * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">here</a>.
   *
   * @return All matching
   * <a href="https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html">
   * attributes of the receipt</a>.
   */
  public static final Collection<ReceiptAttribute> getReceiptAttributes(final Payload payload, final ReceiptAttributeType attributeType) {
    checkNotNull(payload, "The argument %s must not be null", "payload");
    checkNotNull(attributeType, "The argument %s must not be null", "attributeType");
    return payload.getReceiptAttribute()
        .stream()
        .filter(receiptAttribute -> receiptAttribute.getType()
            .intValue() == attributeType.getValue())
        .collect(Collectors.toCollection(ArrayList::new));
  }

  public static final String toString(final Payload payload, final boolean omitUnsupportedAttributes) {
    checkNotNull(payload, "The argument %s must not be null", "payload");
    return Payload.class.getSimpleName()
        + "(\n"
        + payload.getReceiptAttribute()
        .stream()
        .filter(receiptAttribute -> !omitUnsupportedAttributes
            || ReceiptAttributeType.of(receiptAttribute.getType()
            .intValue()) != ReceiptAttributeType.UNSUPPORTED)
        .map(receiptAttribute -> AppStoreReceiptUtil.toString(receiptAttribute, omitUnsupportedAttributes))
        .map(s -> "\t" + s)
        .collect(Collectors.joining("\n"))
        + "\n)";
  }

  public static final String toString(final InAppReceipt inAppReceipt, final boolean omitUnsupportedAttributes) {
    checkNotNull(inAppReceipt, "The argument %s must not be null", "inAppReceipt");
    return InAppReceipt.class.getSimpleName()
        + "(\n"
        + inAppReceipt.getInAppAttribute()
        .stream()
        .filter(receiptAttribute -> !omitUnsupportedAttributes
            || InAppReceiptAttributeType.of(receiptAttribute.getType()
            .intValue()) != InAppReceiptAttributeType.UNSUPPORTED)
        .map(AppStoreReceiptUtil::toString)
        .map(s -> "\t\t" + s)
        .collect(Collectors.joining("\n"))
        + "\n\t)";
  }

  public static final String toString(final ReceiptAttribute receiptAttribute, final boolean omitUnsupportedAttributes) {
    checkNotNull(receiptAttribute, "The argument %s must not be null", "receiptAttribute");
    final ReceiptAttributeType type = ReceiptAttributeType.of(receiptAttribute.getType()
        .intValue());
    return ReceiptAttribute.class.getSimpleName()
        + "(type=" + type.getName()
        + ", version=" + receiptAttribute.getVersion().value.toString()
        + ", value=" + type.asString(receiptAttribute.getValue(), omitUnsupportedAttributes)
        + ')';
  }

  public static final String toString(final InAppAttribute inAppAttribute) {
    checkNotNull(inAppAttribute, "The argument %s must not be null", "inAppAttribute");
    final InAppReceiptAttributeType type = InAppReceiptAttributeType.of(inAppAttribute.getType()
        .intValue());
    return InAppAttribute.class.getSimpleName()
        + "(type=" + type.getName()
        + ", version=" + inAppAttribute.getVersion().value.toString()
        + ", value=" + type.asString(inAppAttribute.getValue())
        + ')';
  }

  private static final String decodeAsn1UTF8String(final BerOctetString v) {
    final BerUTF8String result = new BerUTF8String();
    try {
      result.decode(new ByteArrayInputStream(v.value));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return result.toString();
  }

  private static final String decodeAsn1IA5String(final BerOctetString v) {
    final BerIA5String result = new BerIA5String();
    try {
      result.decode(new ByteArrayInputStream(v.value));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return result.toString();
  }

  private static final BigInteger decodeAsn1Integer(final BerOctetString v) {
    final BerInteger result = new BerInteger();
    try {
      result.decode(new ByteArrayInputStream(v.value));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return result.value;
  }

  @Nullable
  private static final Instant decodeInstant(final BerOctetString v) {
    final String stringV = decodeAsn1IA5String(v);
    return stringV.isEmpty()//one-off subscriptions do not have SUBSCRIPTION_EXPIRATION_DATE value
        ? null
        : Instant.parse(decodeAsn1IA5String(v));
  }

  private static final InAppReceipt decodeInAppReceipt(final BerOctetString v) {
    final InAppReceipt result = new InAppReceipt();
    try {
      result.decode(new ByteArrayInputStream(v.value));
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private AppStoreReceiptUtil() {
    throw new UnsupportedOperationException("This class is not designed to be instantiated");
  }

  /**
   * See https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html
   */
  public enum ReceiptAttributeType {
    BUNDLE_IDENTIFIER(2, "bundle_id"),
    APP_VERSION(3, "application_version"),
    OPAQUE_VALUE(4, "4"),
    SHA1_HASH(5, "5"),
    IN_APP_PURCHASE_RECEIPT(17, "in_app"),
    ORIGINAL_APPLICATION_VERSION(19, "original_application_version"),
    RECEIPT_CREATION_DATE(12, "creation_date"),
    RECEIPT_EXPIRATION_DATE(21, "expiration_date"),
    UNSUPPORTED(Integer.MIN_VALUE, "unsupported");

    private final int value;
    private final String name;

    ReceiptAttributeType(final int value, final String name) {
      this.value = value;
      this.name = name;
    }

    public final int getValue() {
      return value;
    }

    public final String getName() {
      return name;
    }

    public static final ReceiptAttributeType of(final int type) {
      final ReceiptAttributeType result;
      switch (type) {
        case 2: {
          result = BUNDLE_IDENTIFIER;
          break;
        }
        case 3: {
          result = APP_VERSION;
          break;
        }
        case 4: {
          result = OPAQUE_VALUE;
          break;
        }
        case 5: {
          result = SHA1_HASH;
          break;
        }
        case 17: {
          result = IN_APP_PURCHASE_RECEIPT;
          break;
        }
        case 19: {
          result = ORIGINAL_APPLICATION_VERSION;
          break;
        }
        case 12: {
          result = RECEIPT_CREATION_DATE;
          break;
        }
        case 21: {
          result = RECEIPT_EXPIRATION_DATE;
          break;
        }
        default: {
          result = UNSUPPORTED;
          break;
        }
      }
      return result;
    }

    private final String asString(final BerOctetString v, final boolean omitUnsupportedAttributes) {
      final String result;
      switch (this) {
        case IN_APP_PURCHASE_RECEIPT: {
          result = AppStoreReceiptUtil.toString(decodeInAppReceipt(v), omitUnsupportedAttributes);
          break;
        }
        case BUNDLE_IDENTIFIER:
        case APP_VERSION:
        case ORIGINAL_APPLICATION_VERSION: {
          result = decodeAsn1UTF8String(v);
          break;
        }
        case RECEIPT_CREATION_DATE:
        case RECEIPT_EXPIRATION_DATE: {
          result = decodeAsn1IA5String(v);
          break;
        }
        default: {
          result = v.toString();
        }
      }
      return result;
    }
  }

  /**
   * See https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ReceiptFields.html
   */
  public enum InAppReceiptAttributeType {
    QUANTITY(1701, "quantity"),
    PRODUCT_IDENTIFIER(1702, "product_id"),
    TRANSACTION_IDENTIFIER(1703, "transaction_id"),
    ORIGINAL_TRANSACTION_IDENTIFIER(1705, "original_transaction_id"),
    PURCHASE_DATE(1704, "purchase_date"),
    ORIGINAL_PURCHASE_DATE(1706, "original_purchase_date"),
    SUBSCRIPTION_EXPIRATION_DATE(1708, "expires_date"),
    CANCELLATION_DATE(1712, "cancellation_date"),
    WEB_ORDER_LINE_ITEM_ID(1711, "web_order_line_item_id"),
    UNSUPPORTED(Integer.MIN_VALUE, "unsupported");

    private final int value;
    private final String name;

    InAppReceiptAttributeType(final int value, final String name) {
      this.value = value;
      this.name = name;
    }

    public final int getValue() {
      return value;
    }

    public final String getName() {
      return name;
    }

    public static final InAppReceiptAttributeType of(final int type) {
      final InAppReceiptAttributeType result;
      switch (type) {
        case 1701: {
          result = QUANTITY;
          break;
        }
        case 1702: {
          result = PRODUCT_IDENTIFIER;
          break;
        }
        case 1703: {
          result = TRANSACTION_IDENTIFIER;
          break;
        }
        case 1705: {
          result = ORIGINAL_TRANSACTION_IDENTIFIER;
          break;
        }
        case 1704: {
          result = PURCHASE_DATE;
          break;
        }
        case 1706: {
          result = ORIGINAL_PURCHASE_DATE;
          break;
        }
        case 1708: {
          result = SUBSCRIPTION_EXPIRATION_DATE;
          break;
        }
        case 1712: {
          result = CANCELLATION_DATE;
          break;
        }
        case 1711: {
          result = WEB_ORDER_LINE_ITEM_ID;
          break;
        }
        default: {
          result = UNSUPPORTED;
          break;
        }
      }
      return result;
    }

    private final String asString(final BerOctetString v) {
      final String result;
      switch (this) {
        case QUANTITY:
        case WEB_ORDER_LINE_ITEM_ID: {
          result = decodeAsn1Integer(v).toString();
          break;
        }
        case PRODUCT_IDENTIFIER:
        case TRANSACTION_IDENTIFIER:
        case ORIGINAL_TRANSACTION_IDENTIFIER: {
          result = decodeAsn1UTF8String(v);
          break;
        }
        case PURCHASE_DATE:
        case ORIGINAL_PURCHASE_DATE:
        case SUBSCRIPTION_EXPIRATION_DATE:
        case CANCELLATION_DATE: {
          result = decodeAsn1IA5String(v);
          break;
        }
        default: {
          result = v.toString();
        }
      }
      return result;
    }
  }
}