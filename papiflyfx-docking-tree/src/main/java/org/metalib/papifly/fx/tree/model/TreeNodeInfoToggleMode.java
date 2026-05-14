package org.metalib.papifly.fx.tree.model;

public enum TreeNodeInfoToggleMode {
    DISABLED,
    KEYBOARD_ONLY,
    MOUSE_ONLY,
    KEYBOARD_AND_MOUSE;

    public boolean allowsKeyboard() {
        return this == KEYBOARD_ONLY || this == KEYBOARD_AND_MOUSE;
    }

    public boolean allowsMouse() {
        return this == MOUSE_ONLY || this == KEYBOARD_AND_MOUSE;
    }
}
