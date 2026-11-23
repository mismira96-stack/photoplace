package com.example.gallerysorter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Display/selection only. Never changes evidence, verification outcomes or media. */
final class CleanupReviewModel {
    static final class Source {
        final String path, name, volume;
        Source(String path, String name, String volume) {
            this.path = CleanupPairVerifier.path(path);
            this.name = name == null ? "" : name;
            this.volume = volume == null ? "" : volume;
        }
        boolean known() { return !path.isEmpty() && !volume.isEmpty(); }
        String folderLabel() {
            if (!known()) return "원본 위치 확인 필요";
            if (path.equals("DCIM/Camera")) return "카메라";
            if (path.equals("Download") || path.equals("Downloads")) return "다운로드";
            return path.substring(path.lastIndexOf('/') + 1);
        }
        String location() { return known() ? ("external_primary".equals(volume) ? "내장 저장소 / " : "external".equals(volume) ? "저장소 / " : "외장 저장소 / ") + path + "/" : "원본 폴더를 읽지 못했어요"; }
    }
    static final class Group {
        final String key;
        final Source source;
        final String keptPath;
        final List<CleanupCandidate> items = new ArrayList<>();
        final boolean selectable;
        Group(String key, Source source, String keptPath, boolean selectable) {
            this.key = key; this.source = source; this.keptPath = keptPath; this.selectable = selectable;
        }
        String albumName() { return keptPath.substring(keptPath.lastIndexOf('/') + 1); }
    }
    final CleanupReviewAssessment assessment;
    private final Map<String, Source> sources;
    private final List<Group> available = new ArrayList<>(), held = new ArrayList<>();
    private final List<CleanupCandidate> selectable = new ArrayList<>();
    CleanupReviewModel(CleanupReviewAssessment assessment, Map<String, Source> sources) {
        this.assessment = assessment;
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
        Map<String, Group> groups = new LinkedHashMap<>();
        for (CleanupCandidate c : assessment.candidates) {
            Source source = source(c);
            boolean allowed = c.isEligible() && source.known();
            String path = CleanupPairVerifier.path(c.evidence.targetRelativePath);
            String key = source.volume + "\u0000" + source.path + "\u0000" + path + "\u0000" + allowed;
            Group g = groups.get(key);
            if (g == null) { g = new Group(key, source, path, allowed); groups.put(key, g); }
            g.items.add(c);
            if (allowed) selectable.add(c);
        }
        for (Group g : groups.values()) (g.selectable ? available : held).add(g);
    }
    Source source(CleanupCandidate c) {
        Source s = sources.get(c.evidence.sourceUri);
        return s == null ? new Source("", "", "") : s;
    }
    int availableCount() { return selectable.size(); }
    int heldCount() { return assessment.totalCount - availableCount(); }
    List<Group> groups(boolean showHeld) { return Collections.unmodifiableList(showHeld ? held : available); }
    int selectedCount(Group group, Set<String> selected) {
        int count = 0; for (CleanupCandidate c : group.items) if (selected.contains(c.evidence.sourceUri)) count++; return count;
    }
    void selectAll(Set<String> selected, boolean checked) {
        selected.clear();
        if (checked) for (CleanupCandidate c : selectable) selected.add(c.evidence.sourceUri);
    }
    void toggle(Group group, Set<String> selected) {
        if (!group.selectable) return;
        boolean remove = selectedCount(group, selected) == group.items.size();
        for (CleanupCandidate c : group.items) {
            if (remove) selected.remove(c.evidence.sourceUri); else selected.add(c.evidence.sourceUri);
        }
    }
    void toggle(CleanupCandidate candidate, Set<String> selected) {
        if (!candidate.isEligible() || !source(candidate).known()) return;
        if (!selected.remove(candidate.evidence.sourceUri)) selected.add(candidate.evidence.sourceUri);
    }
    List<CleanupCandidate> chosen(Set<String> selected) {
        List<CleanupCandidate> result = new ArrayList<>();
        for (CleanupCandidate c : selectable) if (selected.contains(c.evidence.sourceUri)) result.add(c);
        return result;
    }
    String explanation(CleanupCandidate c) {
        return c.isEligible() && !source(c).known() ? "원본 폴더를 확인하지 못해 보류했어요. 다시 검사해 주세요."
                : assessment.explanationFor(c);
    }
}
