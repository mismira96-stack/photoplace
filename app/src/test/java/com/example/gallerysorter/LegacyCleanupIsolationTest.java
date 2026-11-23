package com.example.gallerysorter;

import org.junit.Test;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

/** Architectural safety check: legacy Activity must not retain executable destructive sinks. */
public class LegacyCleanupIsolationTest {
    @Test public void activityCannotIssueTrashOrPermanentDeleteRequests() throws Exception {
        Set<String> symbols = activitySymbols();
        assertFalse("Legacy trash request still executable", symbols.contains("createTrashRequest"));
        assertFalse("Permanent delete request is forbidden", symbols.contains("createDeleteRequest"));
    }

    @Test public void activityCannotDirectlyWriteTrashedColumn() throws Exception {
        // Existing read-only query predicates contain the full expression, not this column literal.
        assertFalse("Direct IS_TRASHED mutation remains", activitySymbols().contains("is_trashed"));
    }

    private static Set<String> activitySymbols() throws Exception {
        InputStream resource = LegacyCleanupIsolationTest.class.getClassLoader()
                .getResourceAsStream("com/example/gallerysorter/MainActivity.class");
        assertNotNull("Read compiled Activity without loading Android framework classes", resource);
        Set<String> symbols = new HashSet<>();
        try (DataInputStream in = new DataInputStream(resource)) {
            assertEquals(0xCAFEBABE, in.readInt());
            in.readUnsignedShort();
            in.readUnsignedShort();
            int count = in.readUnsignedShort();
            for (int index = 1; index < count; index++) {
                int tag = in.readUnsignedByte();
                switch (tag) {
                    case 1: symbols.add(in.readUTF()); break;
                    case 3: case 4: in.readInt(); break;
                    case 5: case 6: in.readLong(); index++; break;
                    case 7: case 8: case 16: case 19: case 20: in.readUnsignedShort(); break;
                    case 9: case 10: case 11: case 12: case 17: case 18: in.readInt(); break;
                    case 15: in.readUnsignedByte(); in.readUnsignedShort(); break;
                    default: throw new IllegalStateException("Unknown constant pool tag " + tag);
                }
            }
        }
        return symbols;
    }
}
