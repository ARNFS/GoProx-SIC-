package com.example.goprox;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ServiceSearchFilterTest {
    @Test
    public void changingSearchUsesTheFullSourceListAndCanBroadenResults() {
        Service plumber = service("plumber", "Plumbing");
        Service painter = service("painter", "Painting");
        List<Service> all = Arrays.asList(plumber, painter);

        assertEquals(Arrays.asList(plumber), ServiceSearchFilter.filter(all, "plumb"));
        assertEquals(Arrays.asList(painter), ServiceSearchFilter.filter(all, "paint"));
        assertEquals(all, ServiceSearchFilter.filter(all, ""));
    }

    private static Service service(String name, String profession) {
        Service service = new Service();
        service.setName(name);
        service.setProfession(profession);
        return service;
    }
}
