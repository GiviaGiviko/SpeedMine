package meowtils.extension;

import wtf.tatp.meowtils.extension.Extension;

/**
 * This is your main class for your Extension.
 */
public class Main {

    /**
     * This is your entrypoint, it will run when the Extension loads. This should be used
     * for registering new modules, commands, and so on.
     *
     * docs.tatp.wtf/extensions/entrypoint
     */
    public static void init() {
        Extension.registerModule(new SpeedMine()); // Register our example module
    }
}
