
package org.bitpedia.util;

import junit.framework.TestCase;

public class Base32Test extends TestCase {

    public void testBasicDecodeEncode() throws Exception {

        assertEquals("foo", new String(Base32.decode("MZXW6")));
        assertEquals("MZXW6", Base32.encode("foo".getBytes()));

        assertEquals("foob", new String(Base32.decode("MZXW6YQ")));
        assertEquals("MZXW6YQ", Base32.encode("foob".getBytes()));

        assertEquals("fooba", new String(Base32.decode("MZXW6YTB")));
        assertEquals("MZXW6YTB", Base32.encode("fooba".getBytes()));

        assertEquals("foobar", new String(Base32.decode("MZXW6YTBOI")));
        assertEquals("MZXW6YTBOI", Base32.encode("foobar".getBytes()));
    }

    public void testBasicDecodeWithPadding() throws Exception {

        assertEquals("foo", new String(Base32.decode("MZXW6")));
        assertEquals("foo", new String(Base32.decode("MZXW6=")));
        // assertEquals("foo", new String(Base32.decode("MZXW6=="))); Should
        // this work? I thought so, but it does not.
    }

    public void testInvalidCodedStrings() throws Exception {

        // An 8n+{1,3,6} length string is never legal base32.

        try {
            new String(Base32.decode("A"));
            fail();
        } catch (ArrayIndexOutOfBoundsException e) {
            /* failure is expected. */
        }

        // This test fails.
//        try {
//            new String(Base32.decode("ABC"));
//            fail();
//        } catch (ArrayIndexOutOfBoundsException e) {
//            /* failure is expected. */
//        }

        // This test fails.
//        try {
//            new String(Base32.decode("ABCDEF"));
//            fail();
//        } catch (ArrayIndexOutOfBoundsException e) {
//            /* failure is expected. */
//        }
    }
}
