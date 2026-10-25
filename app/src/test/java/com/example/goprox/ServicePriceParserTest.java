
package com.example.goprox;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServicePriceParserTest {

    @Test
    public void nullPrice_shouldReturnZero() {
        assertEquals("0", ServicePriceParser.toPriceString(null));
    }

    @Test
    public void integerPrice_shouldReturnIntegerString() {
        assertEquals("125", ServicePriceParser.toPriceString(125));
    }

    @Test
    public void wholeNumberDouble_shouldNotKeepDecimalPart() {
        assertEquals("125", ServicePriceParser.toPriceString(125.0));
    }

    @Test
    public void decimalPrice_shouldKeepDecimalPart() {
        assertEquals("12.5", ServicePriceParser.toPriceString(12.5));
    }

    @Test
    public void stringPrice_shouldTrimWhitespace() {
        assertEquals("125", ServicePriceParser.toPriceString(" 125 "));
    }

    @Test
    public void emptyString_shouldReturnZero() {
        assertEquals("0", ServicePriceParser.toPriceString("   "));
    }

    @Test
    public void unsupportedType_shouldReturnZero() {
        assertEquals(
                "0",
                ServicePriceParser.toPriceString(Boolean.TRUE)
        );
    }

    @Test
    public void infinitePrice_shouldReturnZero() {
        assertEquals(
                "0",
                ServicePriceParser.toPriceString(Double.POSITIVE_INFINITY)
        );
    }

    @Test
    public void nanPrice_shouldReturnZero() {
        assertEquals(
                "0",
                ServicePriceParser.toPriceString(Double.NaN)
        );
    }
}
