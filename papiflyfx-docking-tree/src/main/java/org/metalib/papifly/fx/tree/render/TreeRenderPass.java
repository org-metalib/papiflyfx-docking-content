package org.metalib.papifly.fx.tree.render;

interface TreeRenderPass<T> {

    default void renderFull(TreeRenderContext<T> context) {}

    default void renderRow(TreeRenderContext<T> context, TreeRenderRow<T> row) {}
}
