"""Compare the input and *rebuilt* APK, including the original protection components.
Usage: python tests/audit.py original.apk patched.apk toolchain-directory
Requires JDK and androguard 4.1.4; reuses the repository's DEX disassembler.
"""
import hashlib
import sys
import subprocess
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile
from lxml import etree
from loguru import logger
from androguard.core.apk import APK
from androguard.core.axml import AXMLPrinter

logger.remove()
original, patched, toolchain = map(Path, sys.argv[1:])
root = Path(__file__).resolve().parents[2]
ns = "{http://schemas.android.com/apk/res/android}"
before, after = APK(str(original)), APK(str(patched))
assert before.get_package() == after.get_package() == "com.urbandroid.ddc"
assert before.get_androidversion_code() == after.get_androidversion_code() == "592"
assert set(before.get_permissions()) == set(after.get_permissions())
m1, m2 = before.get_android_manifest_xml(), after.get_android_manifest_xml()
app = m2.find("application")
new = {n.get(ns+"name"): n for n in app if (n.get(ns+"name") or "").startswith("fr.nesquik.detox.")}
assert set(new) == {"fr.nesquik.detox.Devices","fr.nesquik.detox.Push","fr.nesquik.detox.Delivery"}
assert new["fr.nesquik.detox.Devices"].get(ns+"exported") == "false"
assert new["fr.nesquik.detox.Delivery"].get(ns+"permission") == "android.permission.BIND_JOB_SERVICE"
for node in new.values():
    app.remove(node)
visibility = m2.xpath("queries/package[@android:name='io.heckel.ntfy']", namespaces={"android":ns[1:-1]})
assert len(visibility) == 1
visibility[0].getparent().remove(visibility[0])
def structure(node):
    return (node.tag,tuple(sorted(node.attrib.items())),tuple(structure(n) for n in node))
assert structure(m1) == structure(m2), "Existing manifest changed"
r=after.get_android_resources()
res_id=r.get_res_id_by_key(after.get_package(),"xml","settings_main")
title=r.get_res_id_by_key(after.get_package(),"string","nesquik_detox_sync_title")
translations={value for config,value in r.get_resolved_res_configs(title)}
assert {"Device synchronization","Synchronisation des appareils"} <= translations
for _, path in r.get_resolved_res_configs(res_id):
    prefs=AXMLPrinter(after.get_file(path)).get_xml_obj()
    intent=prefs.find(".//*[@"+ns+"key='nesquik_detox_sync']/intent")
    assert intent.get(ns+"targetClass")=="fr.nesquik.detox.Devices"
with ZipFile(original) as a, ZipFile(patched) as b:
    libs = {n for n in a.namelist() if n.startswith("lib/") and not n.endswith("/")}
    assert libs == {n for n in b.namelist() if n.startswith("lib/") and not n.endswith("/")}
    for name in libs:
        assert hashlib.sha256(a.read(name)).digest()==hashlib.sha256(b.read(name)).digest()
print("PASS existing manifest, native libraries, bilingual settings routing")
with TemporaryDirectory() as tmp:
    jar=str(toolchain/"morphe.jar")
    subprocess.run(["javac","-cp",jar,"-d",tmp,str(root/"x/Dump.java")],check=True)
    def dump(apk,mode,*selectors):
        return subprocess.check_output(["java","-cp",tmp+":"+jar,"Dump",str(apk),mode,*selectors])
    protected=["Lcom/urbandroid/ddc/service/","Lcom/urbandroid/ddc/receiver/",
               "Lcom/urbandroid/ddc/context/Settings;","Lcom/urbandroid/ddc/model/Challenge;"]
    old=dump(original,"dump",*protected)
    assert old==dump(patched,"dump",*protected)
    print("PASS",old.count(b"METHOD "),"native service, receiver, challenge and settings method listings identical")
    ui=dump(patched,"xref","Lfr/nesquik/detox/Ui;")
    host=[m for m in ui.split(b"METHOD ") if m.startswith(b"Lcom/urbandroid/ddc/")]
    assert len(host)==3
    all_sites=b"".join(host)
    for name,count in [(b"launchOption(",1),(b"begin(",1),(b"confirmed(",2),(b"resume(",1)]:
        assert all_sites.count(b"Ui;->"+name)==count
    print("PASS five injected calls confined to three original UI methods")
