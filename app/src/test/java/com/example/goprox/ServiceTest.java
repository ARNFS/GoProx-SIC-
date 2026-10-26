
package com.example.goprox;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class ServiceTest {

    @Test
    public void setRating_shouldNotExceedFive() {
        Service service = new Service();

        service.setRating(7.0f);

        assertEquals(5.0f, service.getRating(), 0.001f);
    }

    @Test
    public void setRating_shouldNotGoBelowZero() {
        Service service = new Service();

        service.setRating(-2.0f);

        assertEquals(0.0f, service.getRating(), 0.001f);
    }

    @Test
    public void setRatingCount_shouldNotBeNegative() {
        Service service = new Service();

        service.setRatingCount(-3);

        assertEquals(0, service.getRatingCount());
    }

    @Test
    public void constructor_shouldUseZeroPriceWhenPriceIsNull() {
        Service service = new Service(
                "service-1",
                "Plumber",
                "Repair",
                "Home repairs",
                null,
                4.5f,
                10,
                null,
                "user-1",
                null
        );

        assertEquals("0", service.getPrice());
    }

    @Test
    public void setTags_shouldCopyInputList() {
        Service service = new Service();

        List<String> originalTags =
                new ArrayList<>(Arrays.asList("repair", "home"));

        service.setTags(originalTags);
        originalTags.add("unexpected");

        assertEquals(
                Arrays.asList("repair", "home"),
                service.getTags()
        );
    }
}
