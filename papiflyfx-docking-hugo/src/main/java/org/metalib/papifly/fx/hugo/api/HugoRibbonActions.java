package org.metalib.papifly.fx.hugo.api;

/**
 * Action contract used by ribbon providers to execute Hugo workflows.
 */
public interface HugoRibbonActions {

    boolean isServerRunning();

    boolean canRunHugoCommands();

    void toggleServer();

    void newContent(String relativePath);

    void build();

    void mod(String subCommand);

    void env();

    void frontMatterTemplate();

    void insertShortcode(String shortcodeName);
}
