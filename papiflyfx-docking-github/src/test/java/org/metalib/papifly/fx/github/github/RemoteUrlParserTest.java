package org.metalib.papifly.fx.github.github;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RemoteUrlParserTest {

    @Test
    void parsesHttpsRemote() {
        RemoteUrlParser.RemoteCoordinates coordinates = RemoteUrlParser.parse("https://github.com/org-metalib/papiflyfx-docking");

        assertEquals("org-metalib", coordinates.owner());
        assertEquals("papiflyfx-docking", coordinates.repo());
    }

    @Test
    void parsesHttpsRemoteWithGitSuffix() {
        RemoteUrlParser.RemoteCoordinates coordinates = RemoteUrlParser.parse("https://github.com/org-metalib/papiflyfx-docking.git");

        assertEquals("org-metalib", coordinates.owner());
        assertEquals("papiflyfx-docking", coordinates.repo());
    }

    @Test
    void parsesScpRemote() {
        RemoteUrlParser.RemoteCoordinates coordinates = RemoteUrlParser.parse("git@github.com:org-metalib/papiflyfx-docking.git");

        assertEquals("org-metalib", coordinates.owner());
        assertEquals("papiflyfx-docking", coordinates.repo());
    }

    @Test
    void parsesSshRemote() {
        RemoteUrlParser.RemoteCoordinates coordinates = RemoteUrlParser.parse("ssh://git@github.com/org-metalib/papiflyfx-docking.git");

        assertEquals("org-metalib", coordinates.owner());
        assertEquals("papiflyfx-docking", coordinates.repo());
    }

    @Test
    void rejectsUnsupportedRemote() {
        assertThrows(IllegalArgumentException.class,
            () -> RemoteUrlParser.parse("https://gitlab.com/org-metalib/papiflyfx-docking"));
    }
}
