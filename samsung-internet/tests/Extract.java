// SPDX-License-Identifier: MPL-2.0
import com.android.tools.smali.dexlib2.Opcodes;
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile;
import com.android.tools.smali.dexlib2.iface.ClassDef;
import com.android.tools.smali.dexlib2.immutable.ImmutableDexFile;
import com.android.tools.smali.dexlib2.writer.pool.DexPool;
import java.io.*;
import java.util.*;
import java.util.zip.*;

class Extract {
    public static void main(String[] args) throws Exception {
        Map<String, ClassDef> classes = new HashMap<>();
        Opcodes ops = Opcodes.forApi(29);
        try (ZipFile apk = new ZipFile(args[0])) {
            for (ZipEntry entry : Collections.list(apk.entries())) {
                if (!entry.getName().matches("classes[0-9]*\\.dex")) continue;
                try (InputStream in = new BufferedInputStream(apk.getInputStream(entry))) {
                    for (ClassDef c : DexBackedDexFile.fromInputStream(ops, in).getClasses()) {
                        String name = c.getType();
                        if (name.startsWith("Lfr/nesquik/sbrowser/blocker/") ||
                                name.startsWith("Lcom/sec/terrace/browser/net/TerraceUrlFilter") ||
                                name.startsWith("Lcom/sec/terrace/browser/browsing_data/TinUrlFilterBridge")) {
                            if (classes.put(name, c) != null) throw new AssertionError("Duplicate " + name);
                        }
                    }
                }
            }
        }
        DexPool.writeTo(args[1], new ImmutableDexFile(ops, classes.values()));
    }
}
