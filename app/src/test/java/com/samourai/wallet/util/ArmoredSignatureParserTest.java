package com.samourai.wallet.util;

import com.samourai.wallet.util.func.ArmoredSignatureParser;

import org.junit.Assert;
import org.junit.Test;

public class ArmoredSignatureParserTest {

    @Test
    public void should_return_null_for_blank_content() {
        Assert.assertNull(ArmoredSignatureParser.parse(""));
        Assert.assertNull(ArmoredSignatureParser.parse(null));
    }

    @Test
    public void should_return_null_for_any_content_having_no_expected_tags() {
        Assert.assertNull(ArmoredSignatureParser.parse("any content"));
    }

    @Test
    public void should_return_instance_when_the_content_respect_bitcoin_armored_signature_format() {
        final ArmoredSignatureParser armoredSignatureParser = ArmoredSignatureParser.parse(getArmoredSignatureContent_00());
        Assert.assertNotNull(armoredSignatureParser);
        Assert.assertEquals("1KdADcJgD7sS94oNCGGe52pw5dteEFjewR", armoredSignatureParser.getAddress());
        Assert.assertEquals("le contenu du message", armoredSignatureParser.getMessage());
        Assert.assertEquals("HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=", armoredSignatureParser.getSignature());
    }

    @Test
    public void should_return_instance_when_for_format_having_space_for_message() {

        final ArmoredSignatureParser armoredSignatureParser01 = ArmoredSignatureParser.parse(getArmoredSignatureContent_01());
        Assert.assertNotNull(armoredSignatureParser01);
        Assert.assertEquals(" ", armoredSignatureParser01.getMessage());

        final ArmoredSignatureParser armoredSignatureParser02 = ArmoredSignatureParser.parse(getArmoredSignatureContent_02());
        Assert.assertNotNull(armoredSignatureParser02);
        Assert.assertEquals("\n", armoredSignatureParser02.getMessage());
    }

    @Test
    public void should_return_null_when_detect_several_tags_of_begin_message() {
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_00()));
    }

    @Test
    public void should_return_null_when_detect_several_tags_of_end_signature() {
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_01()));
    }

    @Test
    public void should_return_null_when_detect_several_tags_of_begin_signature() {
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_02()));
    }

    @Test
    public void should_return_null_when_detect_several_attributes_of_address() {
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_03()));
    }

    @Test
    public void should_return_null_when_detect_several_attributes_of_version() {
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_04()));
    }

    @Test
    public void should_return_null_when_detect_unknown_attributes_in_signature_part() {
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_05()));
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_06()));
    }

    @Test
    public void should_return_null_when_message_is_empty() {
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_07()));
        Assert.assertNull(ArmoredSignatureParser.parse(getBadArmoredSignatureContent_08()));
    }

    private String getArmoredSignatureContent_00() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getArmoredSignatureContent_01() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                " \n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }



    private String getArmoredSignatureContent_02() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "\n\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_00() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_01() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_02() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_03() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_04() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_05() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Alien1: Bitcoin-qt (1.0)\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_06() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "le contenu du message\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Alien2: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_07() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "\n" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }

    private String getBadArmoredSignatureContent_08() {
        return "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                "" +
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1KdADcJgD7sS94oNCGGe52pw5dteEFjewR\n" +
                "\n" +
                "HBHYS6PK+FSH4Jf+oyADFSaksCz9xl9MFKR/jupPWIHNNnMVcJzhEOFO3/BIwNsnQGXxbigGsOuAbkZcWpRP+9I=\n" +
                "-----END BITCOIN SIGNATURE-----";
    }
}
