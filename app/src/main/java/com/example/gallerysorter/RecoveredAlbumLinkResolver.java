package com.example.gallerysorter;

import java.util.*;

/** Read-only recovery of generated Korean albums whose country/link metadata was lost on reinstall.
 * Never infers identity from a name alone and never grants cleanup/delete eligibility. */
final class RecoveredAlbumLinkResolver {
    interface GeoReader { Point read(String uri); }
    static final class Point {
        final double latitude, longitude;
        Point(double latitude,double longitude){this.latitude=latitude;this.longitude=longitude;}
        boolean valid(){return Double.isFinite(latitude)&&Double.isFinite(longitude)&&Math.abs(latitude)<=90&&Math.abs(longitude)<=180&&(latitude!=0||longitude!=0);}
    }
    static final class Match {
        final String albumKey, discoveryKey, primaryKey;
        Match(String album,String discovery,String primary){albumKey=album;discoveryKey=discovery;primaryKey=primary;}
    }
    static Map<String,Match> resolve(List<MemoryRecord> albums,List<MemoryRecord> discoveries,
                                    Map<String,String> ids,Set<String> reservedPaths,GeoReader reader) {
        if(reader==null)return Collections.emptyMap();
        Map<String,List<MemoryRecord>> sources=new LinkedHashMap<>(),targets=new LinkedHashMap<>();
        for(MemoryRecord source:discoveries) {
            if(source.discoveryGroup==null||source.placeKey.startsWith("manual-place:")||source.memoryKey.startsWith("manual:"))continue;
            sources.computeIfAbsent(name(source),key->new ArrayList<>()).add(source);
        }
        for(MemoryRecord album:albums) {
            if(album.organizedAlbum==null||!album.countryCode.isEmpty())continue;
            String path=path(album.organizedAlbum.relativePath);
            if(!path.equals("Pictures/"+album.title)||!album.title.endsWith("에서")||reservedPaths.contains(path))continue;
            targets.computeIfAbsent(name(album),key->new ArrayList<>()).add(album);
        }
        Map<String,Point> geo=new HashMap<>();Set<String> read=new HashSet<>();Map<String,Match> matches=new LinkedHashMap<>();
        for(Map.Entry<String,List<MemoryRecord>> entry:targets.entrySet()) {
            List<MemoryRecord> candidates=sources.get(entry.getKey());
            if(entry.getKey().isEmpty()||entry.getValue().size()!=1||candidates==null||candidates.size()!=1)continue;
            MemoryRecord album=entry.getValue().get(0),source=candidates.get(0);
            // Unknown-country overseas homonyms stay separate; ordinary country-aware matching is unchanged.
            if(!"KR".equals(source.countryCode))continue;
            String albumId=clean(ids.get(album.memoryKey)),sourceId=clean(ids.get(source.memoryKey));
            if(!albumId.isEmpty()&&!sourceId.isEmpty()&&!albumId.equals(sourceId))continue;
            Point target=point(album.coverUri,reader,geo,read);if(target==null)continue;
            boolean nearby=false;int attempts=0;
            for(DiscoveryPhotoRef photo:source.discoveryGroup.photoRefs) {
                if(photo.stale||photo.manualAssigned)continue;
                Point location=point(photo.sourceUri,reader,geo,read);
                if(location!=null&&meters(target,location)<=25000d){nearby=true;break;}
                if(++attempts>=3)break;
            }
            if(!nearby)continue;
            String primary=!albumId.isEmpty()?album.memoryKey:source.memoryKey;
            matches.put(source.memoryKey,new Match(album.memoryKey,source.memoryKey,primary));
        }
        return Collections.unmodifiableMap(matches);
    }
    private static Point point(String uri,GeoReader reader,Map<String,Point> cache,Set<String> read) {
        if(uri==null||uri.isEmpty())return null;
        if(read.add(uri)){try{Point p=reader.read(uri);if(p!=null&&p.valid())cache.put(uri,p);}catch(RuntimeException ignored){ }}
        return cache.get(uri);
    }
    private static double meters(Point a,Point b) {
        double lat=Math.toRadians(b.latitude-a.latitude),lon=Math.toRadians(b.longitude-a.longitude);
        double h=Math.sin(lat/2)*Math.sin(lat/2)+Math.cos(Math.toRadians(a.latitude))*Math.cos(Math.toRadians(b.latitude))*Math.sin(lon/2)*Math.sin(lon/2);
        return 6371000d*2*Math.asin(Math.min(1d,Math.sqrt(h)));
    }
    private static String name(MemoryRecord record){return PlaceNamePolicy.normalizeForMatch(record.canonicalPlaceName);}
    private static String path(String value){String p=clean(value).replace('\\','/');while(p.endsWith("/"))p=p.substring(0,p.length()-1);return p;}
    private static String clean(String value){return value==null?"":value.trim();}
}
