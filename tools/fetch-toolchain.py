#!/usr/bin/env python3
"""Fetch the public build dependencies and check their exact SHA-256."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import subprocess
import sys
from tempfile import NamedTemporaryFile

target = Path(sys.argv[1] if len(sys.argv) > 1 else ".toolchain")
target.mkdir(parents=True, exist_ok=True)
sources = json.loads(Path(__file__).with_name("toolchain.json").read_text())

def sha256(path):
    with path.open("rb") as content:
        return hashlib.file_digest(content, "sha256").hexdigest()

def fetch(entry):
    name, spec = entry
    path = target / name
    if path.is_file() and sha256(path) == spec["sha256"]:
        return
    with NamedTemporaryFile(dir=target, delete=False) as temporary:
        staged = Path(temporary.name)
    try:
        subprocess.run(["curl", "-fLsS", "--retry", "2", "--max-time", "180",
                        spec["url"], "-o", str(staged)], check=True)
        if sha256(staged) != spec["sha256"]:
            raise ValueError(f"Unexpected SHA-256 for {name}")
        staged.replace(path)
    finally:
        staged.unlink(missing_ok=True)

with ThreadPoolExecutor(max_workers=4) as executor:
    list(executor.map(fetch, sources.items()))
