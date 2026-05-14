package org.metalib.papifly.fx.hugo.web;

import java.net.URI;

public final class UrlPolicy {

    private UrlPolicy() {
    }

    public static boolean isSameOrigin(URI base, URI target) {
        if (base == null || target == null) {
            return false;
        }
        if (base.getHost() == null || target.getHost() == null) {
            return false;
        }
        return base.getScheme().equalsIgnoreCase(target.getScheme())
            && base.getHost().equalsIgnoreCase(target.getHost())
            && base.getPort() == target.getPort();
    }

    public static URI safeCreate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return URI.create(value);
        } catch (Exception ex) {
            return null;
        }
    }
}
