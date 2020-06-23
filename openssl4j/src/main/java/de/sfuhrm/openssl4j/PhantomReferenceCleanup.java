package de.sfuhrm.openssl4j;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Frees native AbstractNative objects.
 * The ByteBuffer objects are allocated in {@linkplain OpenSSLMessageDigestNative#OpenSSLMessageDigestNative(String)} ()}
 * and are not used any longer.
 * @author Stephan Fuhrmann
 */
class PhantomReferenceCleanup {

    /** The reference queue of unused AbstractNative objects. */
    private static final ReferenceQueue<OpenSSLMessageDigestNative> BYTE_BUFFER_REFERENCE_QUEUE = new ReferenceQueue<>();

    /** Is the thread running? */
    private static boolean running = false;

    private static final Set<NativePhantomReference> nativePhantomReferenceList = Collections.synchronizedSet(new HashSet<>());

    private static class NativePhantomReference extends PhantomReference<OpenSSLMessageDigestNative> {
        private final ByteBuffer byteBuffer;
        NativePhantomReference(OpenSSLMessageDigestNative abstractNative, ByteBuffer context) {
            super(abstractNative, BYTE_BUFFER_REFERENCE_QUEUE);
            this.byteBuffer = context;
        }
        public void free() {
            OpenSSLMessageDigestNative.removeContext(byteBuffer);
        }
    }

    /** Enqueues a AbstractNative for later cleanup. */
    static void enqueueForCleanup(OpenSSLMessageDigestNative ref, ByteBuffer context) {
        NativePhantomReference phantomReference = new NativePhantomReference(
                Objects.requireNonNull(ref),
                Objects.requireNonNull(context));
        nativePhantomReferenceList.add(phantomReference);
        startIfNeeded();
    }

    /** Checks whether the queue thread is already
     * running and starts it if not.
     * */
    static synchronized void startIfNeeded() {
        if (!running) {
            running = true;
            Runnable r = () -> {
                try {
                    while (true) {
                        NativePhantomReference reference = (NativePhantomReference)BYTE_BUFFER_REFERENCE_QUEUE.remove();
                        reference.free();
                        nativePhantomReferenceList.remove(reference);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            };
            Thread t = new Thread(r, "OpenSSL-Cleanup");
            t.setDaemon(true);
            t.start();
        }
    }
}
