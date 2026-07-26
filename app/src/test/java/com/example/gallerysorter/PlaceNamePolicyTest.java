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
        assertNull(PlaceNamePolicy.cleanSeoulDetailName("앤앤앤에스프레소"));
        assertNull(PlaceNamePolicy.cleanSeoulDetailName("석촌호수"));
        assertNull(PlaceNamePolicy.cleanSeoulDetailName("차호"));
        assertEquals("송파구", PlaceNamePolicy.firstDistrictName("서울특별시 송파구 장지동"));
    }

    @Test
    public void strongPoiNamesAreKept() {
        assertEquals("에버랜드", PlaceNamePolicy.cleanPoiLocationName("Everland Resort"));
        assertEquals("롯데월드", PlaceNamePolicy.cleanPoiLocationName("서울특별시 송파구 롯데월드"));
        assertEquals("롯데월드", PlaceNamePolicy.cleanPoiLocationName("Lotte World Adventure"));
        assertEquals("예술의전당", PlaceNamePolicy.cleanPoiLocationName("Seoul Arts Center"));
        assertEquals("분당서울대병원", PlaceNamePolicy.cleanPoiLocationName("분당서울대학교병원"));
        assertEquals("인천국제공항", PlaceNamePolicy.cleanPoiLocationName("Incheon International Airport"));
        assertEquals("인천국제공항", PlaceNamePolicy.cleanPoiLocationName("인천공항 제1여객터미널"));
        assertEquals("인천국제공항", PlaceNamePolicy.cleanPoiLocationName("인천국제공항 제2여객터미널"));
    }

    @Test
    public void nonAllowlistedPoiNamesFallBackToAdministrativeLocation() {
        assertNull(PlaceNamePolicy.cleanPoiLocationName("광교중앙역"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("광교고등학교"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("앤앤앤 에스프레스"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("부산 해운대 동백로"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("일반 병원"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("서울대학교병원"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("서울대병원"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("김포공항"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("인천공항철도"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("인천공항고속도로"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("롯데월드몰"));
        assertNull(PlaceNamePolicy.cleanPoiLocationName("롯데월드타워"));
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
