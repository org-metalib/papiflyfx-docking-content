package org.metalib.papifly.fx.tree.search;

import org.junit.jupiter.api.Test;
import org.metalib.papifly.fx.tree.api.TreeItem;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeSearchEngineTest {

    @Test
    void findAllReturnsDepthFirstPreOrderMatches() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> alpha = new TreeItem<>("target-alpha");
        TreeItem<String> alphaLeaf = new TreeItem<>("target-alpha-leaf");
        TreeItem<String> beta = new TreeItem<>("beta");
        TreeItem<String> betaLeaf = new TreeItem<>("target-beta-leaf");

        root.addChild(alpha);
        root.addChild(beta);
        alpha.addChild(alphaLeaf);
        beta.addChild(betaLeaf);

        TreeSearchEngine<String> engine = new TreeSearchEngine<>();
        List<TreeItem<String>> matches = engine.findAll(root, "target", null, false);

        assertEquals(3, matches.size());
        assertEquals("target-alpha", matches.get(0).getValue());
        assertEquals("target-alpha-leaf", matches.get(1).getValue());
        assertEquals("target-beta-leaf", matches.get(2).getValue());
    }

    @Test
    void findAllUsesCaseInsensitivePartialMatching() {
        TreeItem<String> root = new TreeItem<>("root");
        TreeItem<String> item = new TreeItem<>("AlphaBeta");
        root.addChild(item);

        TreeSearchEngine<String> engine = new TreeSearchEngine<>();
        List<TreeItem<String>> matches = engine.findAll(root, "phab", null, false);

        assertEquals(1, matches.size());
        assertEquals("AlphaBeta", matches.getFirst().getValue());
    }

    @Test
    void findAllReturnsEmptyForNullOrBlankQueries() {
        TreeItem<String> root = new TreeItem<>("root");
        root.addChild(new TreeItem<>("child"));
        TreeSearchEngine<String> engine = new TreeSearchEngine<>();

        assertTrue(engine.findAll(root, null, null, true).isEmpty());
        assertTrue(engine.findAll(root, "", null, true).isEmpty());
        assertTrue(engine.findAll(root, "   ", null, true).isEmpty());
    }

    @Test
    void findAllCanExcludeRoot() {
        TreeItem<String> root = new TreeItem<>("target-root");
        root.addChild(new TreeItem<>("target-child"));

        TreeSearchEngine<String> engine = new TreeSearchEngine<>();
        List<TreeItem<String>> includeRoot = engine.findAll(root, "target", null, true);
        List<TreeItem<String>> excludeRoot = engine.findAll(root, "target", null, false);

        assertEquals(2, includeRoot.size());
        assertEquals(1, excludeRoot.size());
        assertEquals("target-child", excludeRoot.getFirst().getValue());
    }
}
