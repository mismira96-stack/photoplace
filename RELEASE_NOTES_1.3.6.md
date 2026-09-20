# PhotoPlace 1.3.6

## Highlights

- Fixed Memory and Collection views omitting newly discovered photos after a location album was created.
- Restored organized-only places to Discovery browsing and search.
- Prevented duplicate display of already organized media using normalized filename and media-kind matching.
- Updated representative thumbnails when newer Discovery media is found.
- Kept completed location albums ahead of newly discovered places on Home while reserving a Discovery slot.

## Safety

- Read-only projection changes only.
- No Gallery album creation, file copy/move, MediaStore mutation, or original trash action is included.
- Existing date notes and stable Memory identities are preserved.
