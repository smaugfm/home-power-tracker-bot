package com.github.smaugfm.power.tracker

import org.h2.tools.DeleteDbFiles
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

class DeleteDbFilesExtension : BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    companion object {
        private var started = false;
    }

    override fun beforeAll(context: ExtensionContext?) {
        if (!started) {
            started = true;
            DeleteDbFiles.main();
        }
    }

    override fun close() {
        DeleteDbFiles.main();
    }
}
