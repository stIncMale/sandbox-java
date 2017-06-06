These classes were generated with jASN1 (https://www.openmuc.org/asn1/) from the following ASN.1 definition:

-- this definition was taken from https://developer.apple.com/library/content/releasenotes/General/ValidateAppStoreReceipt/Chapters/ValidateLocally.html
-- app-store-receipt.asn
-- begin
ReceiptModule DEFINITIONS ::=
BEGIN

ReceiptAttribute ::= SEQUENCE {
    type    INTEGER,
    version INTEGER,
    value   OCTET STRING
}

Payload ::= SET OF ReceiptAttribute

InAppAttribute ::= SEQUENCE {
    type                   INTEGER,
    version                INTEGER,
    value                  OCTET STRING
}

InAppReceipt ::= SET OF InAppAttribute

END
-- end

In order to generate the classes, the following command was run: jasn1-compiler -f app-store-receipt.asn -p stinc.male.sandbox.examples.parseappstorereceipt.apple.asn1