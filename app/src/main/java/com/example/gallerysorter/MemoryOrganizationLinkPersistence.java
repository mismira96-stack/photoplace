package com.example.gallerysorter;

/** Enforces one active Gallery output path per Memory identity before persisting a link. */
final class MemoryOrganizationLinkPersistence {
    enum Result {
        PERSISTED,
        ALREADY_PERSISTED,
        CONFLICT,
        INVALID,
        FAILED
    }

    private MemoryOrganizationLinkPersistence() {
    }

    static Result persist(MemoryIdentityRegistryStore identities,
                          MemoryOrganizationLinkStore links,
                          OrganizationLink candidate) {
        if (identities == null || links == null) {
            return Result.FAILED;
        }
        if (candidate == null || !candidate.isValid()
                || candidate.subjectType != OrganizationLink.SubjectType.MEMORY) {
            return Result.INVALID;
        }

        String pathAlias = "path:" + candidate.relativePath;
        String aliasOwner = identities.findStableId(pathAlias);
        if (!aliasOwner.isEmpty() && !aliasOwner.equals(candidate.subjectId)) {
            return Result.CONFLICT;
        }

        boolean sameMemoryAlreadyLinked = false;
        for (OrganizationLink existing : links.readAll()) {
            if (existing == null || !isUsable(existing)
                    || !candidate.relativePath.equals(existing.relativePath)) {
                continue;
            }
            if (existing.subjectType != candidate.subjectType
                    || !existing.subjectId.equals(candidate.subjectId)) {
                return Result.CONFLICT;
            }
            sameMemoryAlreadyLinked = true;
        }

        if (aliasOwner.isEmpty() && !identities.registerAlias(candidate.subjectId, pathAlias)) {
            String ownerAfterWrite = identities.findStableId(pathAlias);
            if (!candidate.subjectId.equals(ownerAfterWrite)) {
                return ownerAfterWrite.isEmpty() ? Result.FAILED : Result.CONFLICT;
            }
        }

        if (sameMemoryAlreadyLinked) {
            return Result.ALREADY_PERSISTED;
        }

        MemoryOrganizationLinkStore.CommitResult commit = links.commit(candidate);
        if (commit == MemoryOrganizationLinkStore.CommitResult.ADDED) {
            return Result.PERSISTED;
        }
        if (commit == MemoryOrganizationLinkStore.CommitResult.ALREADY_COMMITTED) {
            return Result.ALREADY_PERSISTED;
        }
        if (commit == MemoryOrganizationLinkStore.CommitResult.CONFLICT) {
            return Result.CONFLICT;
        }
        if (commit == MemoryOrganizationLinkStore.CommitResult.INVALID) {
            return Result.INVALID;
        }
        return Result.FAILED;
    }

    private static boolean isUsable(OrganizationLink link) {
        return link.status == OrganizationLink.Status.SUCCESS
                || link.status == OrganizationLink.Status.PARTIAL;
    }
}
