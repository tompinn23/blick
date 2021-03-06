package one.tlph.blacksmith.launcher;

import one.tlph.blacksmith.api.launcher.LamdbaExceptionUtils;
import org.apache.logging.log4j.LogManager;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.*;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;
import sun.security.util.ManifestEntryVerifier;

import static one.tlph.blacksmith.launcher.TransformingClassLoader.CLASSLOADING;

public class SecureJarHandler {
    private static final Class<?> JVCLASS = LamdbaExceptionUtils.uncheck(()->Class.forName("java.util.jar.JarVerifier"));
    private static final Method BEGIN_ENTRY = LamdbaExceptionUtils.uncheck(()->JVCLASS.getMethod("beginEntry", JarEntry.class, ManifestEntryVerifier.class));
    private static final Method UPDATE = LamdbaExceptionUtils.uncheck(()->JVCLASS.getMethod("update", int.class, byte[].class, int.class, int.class, ManifestEntryVerifier.class));
    private static final Field JV;
    static {
        Field jv;
        try {
            jv = Manifest.class.getDeclaredField("jv");
            jv.setAccessible(true);
            BEGIN_ENTRY.setAccessible(true);
            UPDATE.setAccessible(true);
        } catch (NoSuchFieldException e) {
            LogManager.getLogger().warn("LEGACY JDK DETECTED, SECURED JAR HANDLING DISABLED");
            jv = null;
        }
        JV = jv;
    }


    @SuppressWarnings("ConstantConditions")
    // Manifest is required to originate from an ensureInitialized JarFile. Otherwise it will not work
    public static CodeSource createCodeSource(final String name, @Nullable final URL url, final byte[] bytes, @Nullable final Manifest manifest) {
        if (JV == null) return null;
        if (manifest == null) return null;
        if (url == null) return null;
        JarEntry je = new JarEntry(name);
        ManifestEntryVerifier mev = new ManifestEntryVerifier(manifest);
        Object obj = LamdbaExceptionUtils.uncheck(()->JV.get(manifest));
        if (obj == null) {
            // we don't have a fully fledged manifest with security info, for some reason (likely loaded by default JAR code, rather than our stuff)
            return null;
        }
        // begin Entry on JarVerifier
        LamdbaExceptionUtils.uncheck(()->BEGIN_ENTRY.invoke(obj, je, mev));
        // Feed the bytes to the underlying MEV
        LamdbaExceptionUtils.uncheck(()->UPDATE.invoke(obj, bytes.length, bytes, 0, bytes.length, mev));
        // Generate the cert check - signers will be loaded into the dummy jar entry
        try {
            UPDATE.invoke(obj, -1, bytes, 0, bytes.length, mev);
        } catch (SecurityException se) {
            // SKIP security exception - we didn't validate the signature for some reason
            LogManager.getLogger().info(CLASSLOADING, "Validation problem during class loading of {}", name, se);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
        CodeSigner[] signers = je.getCodeSigners();
        return new CodeSource(url, signers);
    }

    private static final Map<CodeSource, ProtectionDomain> pdCache = new HashMap<>();
    public static ProtectionDomain createProtectionDomain(CodeSource codeSource, ClassLoader cl) {
        synchronized (pdCache) {
            return pdCache.computeIfAbsent(codeSource, cs->{
                Permissions perms = new Permissions();
                perms.add(new AllPermission());
                return new ProtectionDomain(codeSource, perms, cl, null);
            });
        }
    }

    public static boolean canHandleSecuredJars() {
        return JV != null;
    }
}
