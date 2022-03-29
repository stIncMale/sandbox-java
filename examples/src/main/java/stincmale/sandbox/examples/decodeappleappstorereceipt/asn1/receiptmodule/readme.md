These classes were generated with the compiler from the [ASN1bean](https://www.beanit.com/asn1/)
library from the ASN.1 definition in the file
[app-store-receipt.asn](https://github.com/stIncMale/sandbox-java/blob/master/examples/src/main/asn/stincmale/sandbox/examples/decodeappleappstorereceipt/app-store-receipt.asn).
In order to generate the classes, the following command was run:

```
asn1bean-compiler -f app-store-receipt.asn -p stincmale.sandbox.examples.decodeappleappstorereceipt.asn1
```

Then I manually modified

- the type of `InAppReceipt.seqOf` from `List<InAppAttribute>` to `ArrayList<InAppAttribute>`,
- the type of `Payload.seqOf` from `List<ReceiptAttribute>` to `ArrayList<ReceiptAttribute>`.

These modifications are required to work around a bug in ASN1bean: `List` is not `Serializable`,
and `InAppReceipt`/`Payload` are `Serializable`. `javac` emitts warnings

> non-transient instance field of a serializable class declared with a non-serializable type

to notify about the problems in the code.
