// SPDX-License-Identifier: MPL-2.0
import java.io.*;
import java.util.zip.*;
import org.objectweb.asm.*;

// Rebuild the JVM stack maps omitted by dex2jar 2.4, before full JVM verification.
class Frames {
    public static void main(String[] args) throws Exception {
        try (var in = new ZipInputStream(new FileInputStream(args[0]));
             var out = new ZipOutputStream(new FileOutputStream(args[1]))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null;) {
                if (!e.getName().endsWith(".class")) continue;
                var writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
                new ClassReader(in.readAllBytes()).accept(writer, ClassReader.SKIP_FRAMES);
                out.putNextEntry(new ZipEntry(e.getName()));
                out.write(writer.toByteArray());
                out.closeEntry();
            }
        }
    }
}
