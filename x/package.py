import re
import sys
from pathlib import Path
from zipfile import ZIP_DEFLATED, ZipFile

mode, base, work_dir, output = sys.argv[1:]
work = Path(work_dir)
src = Path(__file__).resolve().parent
runtime = work / "runtime"
patch = work / "patch"

def create_jar(target, directory, names):
    with ZipFile(target, "w", ZIP_DEFLATED) as archive:
        for name in sorted(names):
            archive.write(directory / name, name)

def runtime_group(name):
    if name.startswith("app/morphe/extension/newx/postfilter/Advanced") or "/advanced/re2j/" in name:
        return "advanced"
    for package, target in (
        ("anonymous", "anonymous"), ("profileimages", "profile-images"),
        ("hidesensitive", "hide-sensitive"), ("mediablur", "media-blur"),
    ):
        if name.startswith(f"app/morphe/extension/newx/{package}/"):
            return target
    return "stubs"

if mode == "prepare":
    classes = {"advanced": [], "anonymous": [], "profile-images": [],
               "hide-sensitive": [], "media-blur": [], "stubs": []}
    for path in runtime.rglob("*.class"):
        name = path.relative_to(runtime).as_posix()
        classes[runtime_group(name)].append(name)
    for name, entries in classes.items():
        jar = work / ("runtime-stubs.jar" if name == "stubs" else f"extensions/{name}.jar")
        create_jar(jar, runtime, entries)

    for name in classes["advanced"] + classes["stubs"]:
        if re.search(r"/(AdvancedMatcher|PhraseMatcher|AhoMatcher)(\$[^/]+)?\.class$", name) or "/advanced/re2j/" in name:
            (patch / name).parent.mkdir(parents=True, exist_ok=True)
            (patch / name).write_bytes((runtime / name).read_bytes())

    with ZipFile(base) as archive:
        for name in archive.namelist():
            if not name.endswith(".class") or (patch / name).is_file():
                continue
            if (
                name.startswith(("app/crimera/bytecode/", "app/crimera/patches/common/",
                                 "app/crimera/patches/newx/models/", "com/google/gson/"))
                or name in ("app/crimera/patches/newx/utils/InstallAvailabilityKt.class",
                            "app/morphe/util/BytecodeUtilsKt.class",
                            "app/morphe/util/BytecodeUtilsKt$WhenMappings.class")
            ):
                (patch / name).parent.mkdir(parents=True, exist_ok=True)
                (patch / name).write_bytes(archive.read(name))
    create_jar(work / "patch.jar", patch, [p.relative_to(patch).as_posix() for p in patch.rglob("*.class")])
else:
    with ZipFile(output, "w", ZIP_DEFLATED) as bundle:
        manifest = [
            "Manifest-Version: 1.0", "Name: Nesquik's patches - X", "Version: 0.1.0",
            "Description: Five personal patches integrating with Piko for X 12.43.0",
            "Patcher-Version: 2.4.0", "Author: Nesquik", "License: GPL-3.0", "", ""
        ]
        bundle.writestr("META-INF/MANIFEST.MF", "\r\n".join(manifest))
        for dex in sorted((work / "bundle-dex").glob("classes*.dex")):
            bundle.write(dex, dex.name)
        for path in sorted(patch.rglob("*")):
            if path.is_file():
                bundle.write(path, path.relative_to(patch).as_posix())
        for dex in sorted((work / "extensions").glob("*/classes.dex")):
            bundle.write(dex, f"extensions/{dex.parent.name}.mpe")
        for icon in sorted((src / "res").glob("ic_piko_blur_*.xml")):
            bundle.write(icon, f"mediablur/{icon.name}")
        for strings in sorted((src / "res").glob("personal-strings*.xml")):
            bundle.write(strings, f"nesquik-x/{strings.name}")
    with ZipFile(output) as bundle:
        if len(bundle.namelist()) != len(set(bundle.namelist())) or bundle.testzip() is not None:
            raise ValueError("Bundle has duplicate or invalid entries")
    print(output)
