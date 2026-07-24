package com.example.gallerysorter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaceNamePolicyTest {
    @Test
    public void romanizedSeoulDistrictsCollapseToKoreanGu() {
        assertEquals("송파구", PlaceNamePolicy.extractDistrictName("Songpa-gu Seoul"));
        assertEquals("마포구", PlaceNamePolicy.extractDistrictName("Mapo-gu, Seoul"));
    }

    @Test
    public void seoulAdministrativeDongDoesNotBecomeDetailPlace() {
        assertNull(PlaceNamePolicy.cleanSeoulDetailName("송파동"));
        assertNull(PlaceNamePolicy.cleanSeoulDetailName("장지동"));
        assertEquals("송파구", PlaceNamePolicy.firstDistrictName("서울특별시 송파구 장지동"));
    }

    @Test
    public void strongPoiNamesAreKept() {
        assertEquals("에버랜드", PlaceNamePolicy.cleanPoiLocationName("Everland Resort"));
        assertEquals("롯데월드", PlaceNamePolicy.cleanPoiLocationName("서울특별시 송파구 롯데월드"));
    }

    @Test
    public void accessPointNoiseBlocksPoiPromotion() {
        assertNull(PlaceNamePolicy.knownPlaceName(PlaceNamePolicy.normalizeForMatch("봉은사 코엑스 북문")));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("봉은사 코엑스 북문"));
        assertTrue(PlaceNamePolicy.hasAccessPointNoise(PlaceNamePolicy.normalizeForMatch("COEX North Gate")));
    }

    @Test
    public void noisyCommercialBuildingNamesAreNotPoi() {
        assertFalse(PlaceNamePolicy.looksLikePoiName("밀리토피아시티"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("Militopia City 지하"));
    }

    @Test
    public void genericAdministrativeSuffixesAreStrippedSafely() {
        assertEquals("성남", PlaceNamePolicy.normalizeLocationKey("성남시"));
        assertEquals("강남구", PlaceNamePolicy.normalizeLocationKey("강남구"));
    }
}
