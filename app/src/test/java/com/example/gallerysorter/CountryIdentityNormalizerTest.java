package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CountryIdentityNormalizerTest {
    @Test
    public void normalizesGeocoderCountryCodeFirst() {
        assertEquals("JP", CountryIdentityNormalizer.countryCode("JP", "Japan"));
        assertEquals("일본", CountryIdentityNormalizer.displayName("JP", "Japan"));
    }

    @Test
    public void normalizesLegacyCountryNames() {
        assertEquals("JP", CountryIdentityNormalizer.countryCode("", "日本"));
        assertEquals("TR", CountryIdentityNormalizer.countryCode("", "Turkey"));
        assertEquals("TR", CountryIdentityNormalizer.countryCode("", "Türkiye"));
        assertEquals("TR", CountryIdentityNormalizer.countryCode("", "튀르키에"));
        assertEquals("CZ", CountryIdentityNormalizer.countryCode("", "Czech Republic"));
    }

    @Test
    public void countryAliasesMatchSearchQueries() {
        assertTrue(CountryIdentityNormalizer.matchesSearchQuery("JP", "", "japan"));
        assertTrue(CountryIdentityNormalizer.matchesSearchQuery("JP", "", "일본"));
        assertTrue(CountryIdentityNormalizer.matchesSearchQuery("TR", "", "turkey"));
        assertTrue(CountryIdentityNormalizer.matchesSearchQuery("TR", "", "튀르키예"));
        assertTrue(CountryIdentityNormalizer.matchesSearchQuery("CZ", "", "czechia"));
    }
}
