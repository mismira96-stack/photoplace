package com.example.gallerysorter;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Persists confirmed Gallery outputs without making them the identity of a Memory. */
final class MemoryOrganizationLinkStore {
    private static final String FILE_NAME = "memory_organization_links.json";
    private static final int SCHEMA_VERSION = 1;

    enum CommitResult {
        ADDED,
        ALREADY_COMMITTED,
        CONFLICT,
        INVALID,
        FAILED
    }

    private final File filesDir;

    MemoryOrganizationLinkStore(Context context) {
        this(context.getApplicationContext().getFilesDir());
    }

    MemoryOrganizationLinkStore(File filesDir) {
        this.filesDir = filesDir;
    }

    synchronized List<OrganizationLink> readAll() {
        return Collections.unmodifiableList(linksFrom(readRoot()));
    }

    synchronized OrganizationLink findByRequestId(String requestId) {
        String id = clean(requestId);
        if (id.isEmpty()) {
            return null;
        }
        for (OrganizationLink link : linksFrom(readRoot())) {
            if (id.equals(link.requestId)) {
                return link;
            }
        }
        return null;
    }

    synchronized List<OrganizationLink> linksForSubject(OrganizationLink.SubjectType subjectType,
                                                          String subjectId) {
        String id = clean(subjectId);
        if (subjectType == null || id.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<OrganizationLink> result = new ArrayList<>();
        for (OrganizationLink link : linksFrom(readRoot())) {
            if (subjectType == link.subjectType && id.equals(link.subjectId)) {
                result.add(link);
            }
        }
        return Collections.unmodifiableList(result);
    }

    synchronized boolean hasUsableOutput(OrganizationLink.SubjectType subjectType, String subjectId) {
        return findLatestUsableLink(subjectType, subjectId) != null;
    }

    synchronized OrganizationLink findLatestUsableLink(OrganizationLink.SubjectType subjectType,
                                                        String subjectId) {
        OrganizationLink latest = null;
        for (OrganizationLink link : linksForSubject(subjectType, subjectId)) {
            if ((link.status == OrganizationLink.Status.SUCCESS
                    || link.status == OrganizationLink.Status.PARTIAL)
                    && (latest == null || link.organizedAtMillis >= latest.organizedAtMillis)) {
                latest = link;
            }
        }
        return latest;
    }

    synchronized CommitResult commit(OrganizationLink candidate) {
        if (candidate == null || !candidate.isValid()) {
            return CommitResult.INVALID;
        }
        JSONObject root = readWritableRoot();
        if (root == null) {
            return CommitResult.FAILED;
        }
        List<OrganizationLink> links = linksFrom(root);
        for (OrganizationLink existing : links) {
            if (existing.requestId.equals(candidate.requestId)) {
                if (sameRequest(existing, candidate)
                        && (existing.status == candidate.status
                        || existing.status == OrganizationLink.Status.MISSING)) {
                    return CommitResult.ALREADY_COMMITTED;
                }
                return CommitResult.CONFLICT;
            }
            if (existing.linkId.equals(candidate.linkId)) {
                return CommitResult.CONFLICT;
            }
        }
        links.add(candidate);
        try {
            writeRoot(rootWithLinks(links));
            return CommitResult.ADDED;
        } catch (Exception ignored) {
            return CommitResult.FAILED;
        }
    }

    synchronized boolean markMissing(String linkId) {
        String id = clean(linkId);
        if (id.isEmpty()) {
            return false;
        }
        JSONObject root = readWritableRoot();
        if (root == null) {
            return false;
        }
        List<OrganizationLink> links = linksFrom(root);
        for (int index = 0; index < links.size(); index++) {
            OrganizationLink current = links.get(index);
            if (!id.equals(current.linkId)) {
                continue;
            }
            if (current.status == OrganizationLink.Status.MISSING) {
                return true;
            }
            links.set(index, withStatus(current, OrganizationLink.Status.MISSING));
            try {
                writeRoot(rootWithLinks(links));
                return true;
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    private JSONObject readRoot() {
        JSONObject root = readRootFile(file());
        if (root != null) {
            return root;
        }
        JSONObject backup = readRootFile(backupFile());
        if (backup != null) {
            restoreBackup();
            return backup;
        }
        return new JSONObject();
    }

    private JSONObject readWritableRoot() {
        if (!file().exists() && !backupFile().exists()) {
            return new JSONObject();
        }
        JSONObject root = readRootFile(file());
        if (root != null) {
            return root;
        }
        JSONObject backup = readRootFile(backupFile());
        if (backup == null) {
            return null;
        }
        restoreBackup();
        return readRootFile(file());
    }

    private JSONObject readRootFile(File candidate) {
        if (candidate == null || !candidate.exists()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        try (FileInputStream input = new FileInputStream(candidate);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line);
            }
            JSONObject root = new JSONObject(text.toString());
            JSONArray values = root.optJSONArray("links");
            if (root.optInt("schemaVersion", -1) != SCHEMA_VERSION || values == null) {
                return null;
            }
            Set<String> linkIds = new HashSet<>();
            Set<String> requestIds = new HashSet<>();
            for (int index = 0; index < values.length(); index++) {
                OrganizationLink link = OrganizationLink.fromJson(values.optJSONObject(index));
                if (link == null || !linkIds.add(link.linkId) || !requestIds.add(link.requestId)) {
                    return null;
                }
            }
            return root;
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<OrganizationLink> linksFrom(JSONObject root) {
        JSONArray values = root == null ? null : root.optJSONArray("links");
        if (values == null || values.length() == 0) {
            return new ArrayList<>();
        }
        ArrayList<OrganizationLink> links = new ArrayList<>();
        for (int index = 0; index < values.length(); index++) {
            OrganizationLink link = OrganizationLink.fromJson(values.optJSONObject(index));
            if (link != null) {
                links.add(link);
            }
        }
        return links;
    }

    private JSONObject rootWithLinks(List<OrganizationLink> links) throws Exception {
        JSONObject root = new JSONObject();
        root.put("schemaVersion", SCHEMA_VERSION);
        JSONArray values = new JSONArray();
        if (links != null) {
            for (OrganizationLink link : links) {
                values.put(link.toJson());
            }
        }
        root.put("links", values);
        return root;
    }

    private void writeRoot(JSONObject root) throws Exception {
        if (!filesDir.exists() && !filesDir.mkdirs()) {
            throw new IllegalStateException("Could not create organization link directory");
        }
        File target = file();
        File temp = tempFile();
        File backup = backupFile();
        try (FileOutputStream output = new FileOutputStream(temp)) {
            output.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            output.getFD().sync();
        }
        if (backup.exists() && !backup.delete()) {
            throw new IllegalStateException("Could not clear organization link backup");
        }
        boolean hadTarget = target.exists();
        if (hadTarget && !target.renameTo(backup)) {
            throw new IllegalStateException("Could not back up organization links");
        }
        if (!temp.renameTo(target)) {
            if (hadTarget) {
                backup.renameTo(target);
            }
            throw new IllegalStateException("Could not write organization links");
        }
        if (backup.exists()) {
            backup.delete();
        }
    }

    private void restoreBackup() {
        File backup = backupFile();
        if (!backup.exists()) {
            return;
        }
        File target = file();
        File corrupt = new File(filesDir, FILE_NAME + ".corrupt");
        if (corrupt.exists()) {
            corrupt.delete();
        }
        if (target.exists() && !target.renameTo(corrupt)) {
            return;
        }
        if (!backup.renameTo(target) && corrupt.exists()) {
            corrupt.renameTo(target);
        }
    }

    private static boolean sameRequest(OrganizationLink first, OrganizationLink second) {
        return first.subjectType == second.subjectType
                && first.subjectId.equals(second.subjectId)
                && first.requestId.equals(second.requestId)
                && first.albumName.equals(second.albumName)
                && first.relativePath.equals(second.relativePath)
                && first.copiedCount == second.copiedCount
                && first.skippedCount == second.skippedCount
                && first.failedCount == second.failedCount;
    }

    private static OrganizationLink withStatus(OrganizationLink link, OrganizationLink.Status status) {
        return new OrganizationLink(
                link.linkId,
                link.subjectType,
                link.subjectId,
                link.requestId,
                link.albumName,
                link.relativePath,
                link.organizedAtMillis,
                status,
                link.copiedCount,
                link.skippedCount,
                link.failedCount);
    }

    private File file() { return new File(filesDir, FILE_NAME); }
    private File tempFile() { return new File(filesDir, FILE_NAME + ".tmp"); }
    private File backupFile() { return new File(filesDir, FILE_NAME + ".bak"); }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
