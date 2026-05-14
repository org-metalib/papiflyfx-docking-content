package org.metalib.papifly.fx.hugo.process;

public record HugoServerOptions(
    String hugoBinary,
    int preferredPort,
    String bindAddress,
    boolean disableFastRender,
    boolean renderToMemory,
    boolean buildDrafts
) {

    private static final int DEFAULT_PORT = 1313;

    public HugoServerOptions {
        hugoBinary = hugoBinary == null || hugoBinary.isBlank() ? "hugo" : hugoBinary;
        preferredPort = preferredPort > 0 ? preferredPort : DEFAULT_PORT;
        bindAddress = bindAddress == null || bindAddress.isBlank() ? "127.0.0.1" : bindAddress;
    }

    public static HugoServerOptions defaults(int preferredPort) {
        return new HugoServerOptions("hugo", preferredPort, "127.0.0.1", true, false, true);
    }
}
