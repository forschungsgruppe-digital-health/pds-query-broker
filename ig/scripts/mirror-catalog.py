#!/usr/bin/env python3
"""Mirror SUSHI-generated conformance artifacts into catalog/.

FSH under ig/input/fsh/ is the single source of truth for all FHIR
conformance artifacts. This script regenerates the catalog/ mirror from
ig/fsh-generated/resources/ (run `sushi build` first). catalog/*.json
files of the mirrored types must NEVER be edited by hand — CI fails on
any drift between catalog/ and the compiled FSH.

Usage: python3 ig/scripts/mirror-catalog.py   (from the repo root)
"""

import json
import shutil
import sys
from pathlib import Path

MIRRORED_TYPES = [
    "CodeSystem",
    "ValueSet",
    "StructureDefinition",
    "OperationDefinition",
    "MessageDefinition",
    "GraphDefinition",
]

repo = Path(__file__).resolve().parents[2]
src = repo / "ig" / "fsh-generated" / "resources"
catalog = repo / "catalog"

if not src.is_dir():
    sys.exit(f"{src} not found — run `sushi build` in ig/ first.")

for rtype in MIRRORED_TYPES:
    target = catalog / rtype
    if target.is_dir():
        for stale in target.glob("*.json"):
            stale.unlink()

copied = 0
for f in sorted(src.glob("*.json")):
    resource = json.loads(f.read_text())
    rtype = resource.get("resourceType")
    if rtype not in MIRRORED_TYPES:
        continue
    name = resource.get("name")
    if not name:
        sys.exit(f"{f.name}: mirrored resource has no `name` element.")
    target = catalog / rtype
    target.mkdir(parents=True, exist_ok=True)
    shutil.copy(f, target / f"{name}.json")
    copied += 1

print(f"Mirrored {copied} conformance artifacts into {catalog}/.")
