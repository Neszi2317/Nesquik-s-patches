#!/usr/bin/env python3
"""Merge independently versioned patch modules for Morphe."""
import argparse
from pathlib import Path
import re
import subprocess
from tempfile import TemporaryDirectory
from zipfile import ZIP_DEFLATED, ZipFile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--r8", type=Path, required=True)
parser.add_argument("--android-jar", type=Path, required=True)
parser.add_argument("--morphe-jar", type=Path, required=True)
parser.add_argument("--version", required=True)
parser.add_argument("--output", type=Path, required=True)
parser.add_argument("bundles", type=Path, nargs="+")
args = parser.parse_args()
if not re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+(?:[-.][a-zA-Z0-9.]+)?", args.version):
    parser.error("invalid release version")

manifest = [
    "Manifest-Version: 1.0", "Name: Nesquik's patches", f"Version: {args.version}",
    "Description: Personal patches for X, Reddit, YouTube, Firefox and Discord",
    "Patcher-Version: 2.4.0", "Author: Nesquik", "License: See component licenses", "", ""
]

with TemporaryDirectory() as directory:
    work = Path(directory)
    entries = {}
    for path in args.bundles:
        with ZipFile(path) as module:
            if not any(n.endswith(".class") for n in module.namelist()):
                raise ValueError(f"Missing JVM patch classes in {path}")
            for item in module.infolist():
                name = item.filename
                if item.is_dir():
                    continue
                if re.fullmatch(r"META-INF/[^/]+\.(SF|RSA|DSA|EC)", name, re.I):
                    continue
                data = module.read(item)
                if re.fullmatch(r"classes\d*\.dex", name):
                    continue
                if name.startswith("META-INF/") and name.endswith(".kotlin_module"):
                    name = f"META-INF/{path.stem}-{Path(name).name}"
                if name in {"LICENSE", "NOTICE", "META-INF/MANIFEST.MF"}:
                    name = f"META-INF/notices/{path.stem}/{Path(name).name}"
                if name in entries:
                    raise ValueError(f"Duplicate archive entry {name} from {path}")
                entries[name] = data

    # Compile the combined classfiles once so D8 owns one set of synthetic annotations.
    classes = work / "module-classes.jar"
    with ZipFile(classes, "w", ZIP_DEFLATED) as output:
        for name, data in sorted(entries.items()):
            if name.endswith(".class"):
                output.writestr(name, data)
    dex_dir = work / "dex"
    dex_dir.mkdir()
    subprocess.run([
        "java", "-Xmx2g", "-cp", str(args.r8), "com.android.tools.r8.D8",
        "--release", "--min-api", "29", "--lib", str(args.android_jar),
        "--classpath", str(args.morphe_jar), "--output", str(dex_dir), str(classes),
    ], check=True)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with ZipFile(args.output, "w", ZIP_DEFLATED) as output:
        output.writestr("META-INF/MANIFEST.MF", "\r\n".join(manifest))
        for dex in sorted(dex_dir.glob("classes*.dex")):
            output.write(dex, dex.name)
        for name, data in sorted(entries.items()):
            output.writestr(name, data)
    with ZipFile(args.output) as output:
        assert output.testzip() is None
        assert len(output.namelist()) == len(set(output.namelist()))
