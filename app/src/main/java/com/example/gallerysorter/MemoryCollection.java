package com.example.gallerysorter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/** User-authored, app-only grouping of existing Memory identities. */
final class MemoryCollection {
    static final int MIN_MEMBER_COUNT = 2;

    final String collectionId;
    final String title;
    final List<Member> members;
    final long createdAtMillis;
    final long updatedAtMillis;

    MemoryCollection(String collectionId,
                     String title,
                     List<Member> members,
                     long createdAtMillis,
                     long updatedAtMillis) {
        this.collectionId = clean(collectionId);
        this.title = clean(title);
        this.members = immutableDistinctMembers(members);
        this.createdAtMillis = Math.max(0L, createdAtMillis);
        this.updatedAtMillis = Math.max(this.createdAtMillis, updatedAtMillis);
    }

    boolean isValid() {
        return collectionId.startsWith("group_")
                && !title.isEmpty()
                && members.size() >= MIN_MEMBER_COUNT;
    }

    MemoryCollection withTitle(String title, long updatedAtMillis) {
        return new MemoryCollection(
                collectionId,
                title,
                members,
                createdAtMillis,
                Math.max(this.updatedAtMillis, updatedAtMillis));
    }

    JSONObject toJson() throws Exception {
        JSONObject json = new JSONObject();
        json.put("collectionId", collectionId);
        json.put("title", title);
        JSONArray memberArray = new JSONArray();
        for (Member member : members) {
            memberArray.put(member.toJson());
        }
        json.put("members", memberArray);
        json.put("createdAtMillis", createdAtMillis);
        json.put("updatedAtMillis", updatedAtMillis);
        return json;
    }

    static MemoryCollection fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        JSONArray memberArray = json.optJSONArray("members");
        ArrayList<Member> members = new ArrayList<>();
        if (memberArray != null) {
            for (int index = 0; index < memberArray.length(); index++) {
                Member member = Member.fromJson(memberArray.optJSONObject(index));
                if (member != null) {
                    members.add(member);
                }
            }
        }
        MemoryCollection collection = new MemoryCollection(
                json.optString("collectionId", ""),
                json.optString("title", ""),
                members,
                json.optLong("createdAtMillis", 0L),
                json.optLong("updatedAtMillis", 0L));
        return collection.isValid() ? collection : null;
    }

    private static List<Member> immutableDistinctMembers(List<Member> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashMap<String, Member> distinct = new LinkedHashMap<>();
        for (Member member : values) {
            if (member != null && member.isValid() && !distinct.containsKey(member.stableMemoryId)) {
                distinct.put(member.stableMemoryId, member);
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(distinct.values()));
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    static final class Member {
        final String stableMemoryId;
        final String lastKnownAlias;

        Member(String stableMemoryId, String lastKnownAlias) {
            this.stableMemoryId = clean(stableMemoryId);
            this.lastKnownAlias = clean(lastKnownAlias);
        }

        boolean isValid() {
            return stableMemoryId.startsWith("mem_");
        }

        JSONObject toJson() throws Exception {
            JSONObject json = new JSONObject();
            json.put("stableMemoryId", stableMemoryId);
            json.put("lastKnownAlias", lastKnownAlias);
            return json;
        }

        static Member fromJson(JSONObject json) {
            if (json == null) {
                return null;
            }
            Member member = new Member(
                    json.optString("stableMemoryId", ""),
                    json.optString("lastKnownAlias", ""));
            return member.isValid() ? member : null;
        }
    }
}
