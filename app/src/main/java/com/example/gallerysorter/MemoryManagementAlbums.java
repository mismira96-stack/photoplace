package com.example.gallerysorter;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Reads recorded album associations; does not guess a folder from its display name. */
final class MemoryManagementAlbums {
    static List<StoredAlbumSummary> read(Context c) {
        List<StoredAlbumSummary> albums=new ArrayList<>();Set<String> paths=new HashSet<>();
        JSONArray sessions=new AlbumSummaryHistoryStore(c).readRoot().optJSONArray("sessions");
        if(sessions!=null)for(int i=0;i<sessions.length();i++) {
            JSONObject session=sessions.optJSONObject(i);JSONArray rows=session==null?null:session.optJSONArray("albums");
            if(rows==null)continue;
            for(int j=0;j<rows.length();j++)if(rows.optJSONObject(j)!=null) {
                StoredAlbumSummary a=StoredAlbumSummary.fromJson(rows.optJSONObject(j));
                if(a.relativePath!=null&&!a.relativePath.isEmpty()&&paths.add(a.relativePath))albums.add(a);
            }
        }
        return withLinks(albums,new MemoryOrganizationLinkStore(c).readAll());
    }
    static List<StoredAlbumSummary> withLinks(List<StoredAlbumSummary> albums,List<OrganizationLink> links) {
        List<StoredAlbumSummary> result=new ArrayList<>(albums);Set<String> paths=new HashSet<>();
        for(StoredAlbumSummary a:albums)paths.add(a.relativePath);
        for(OrganizationLink link:links)if(link.isValid()&&link.subjectType==OrganizationLink.SubjectType.MEMORY
                &&link.status!=OrganizationLink.Status.MISSING&&paths.add(link.relativePath))
            result.add(new StoredAlbumSummary(link.albumName,link.relativePath,link.copiedCount,"","","","",
                    link.organizedAtMillis,"","",""));
        return result;
    }
    static Set<String> paths(List<StoredAlbumSummary> albums) {
        Set<String> result=new HashSet<>();for(StoredAlbumSummary a:albums)if(a.relativePath!=null&&!a.relativePath.isEmpty())result.add(a.relativePath);
        return result;
    }
    static List<StoredAlbumSummary> withManual(List<StoredAlbumSummary> albums,ManualPlaceSnapshot manual)throws Exception {
        List<StoredAlbumSummary> result=new ArrayList<>(albums);Set<String> paths=paths(albums);
        JSONObject places=manual.writableCopy().getJSONObject("places");
        for(Iterator<String> keys=places.keys();keys.hasNext();) {
            JSONObject place=places.getJSONObject(keys.next());String path=place.optString("outputPath","");
            if(path.isEmpty()||!paths.add(path))continue;
            String[] parts=path.split("/");
            result.add(new StoredAlbumSummary(parts[parts.length-1],path,0,"","","","",0,
                    place.optString("countryCode",""),place.optString("countryName",""),place.optString("adminArea",""),place.optString("addressLine","")));
        }
        return result;
    }
}
