#!/usr/bin/env python3
"""Verify Android runtime assets are an exact mirror of upstream assets/."""
from pathlib import Path
import hashlib, sys
ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "upstream-source" / "assets"
PACKAGED = ROOT / "app" / "src" / "main" / "assets"

def files(base):
    return {p.relative_to(base).as_posix(): p for p in base.rglob("*") if p.is_file()}

src, dst = files(SOURCE), files(PACKAGED)
missing = sorted(set(src) - set(dst))
extra = sorted(set(dst) - set(src))
mismatched = sorted(p for p in set(src) & set(dst) if hashlib.sha256(src[p].read_bytes()).digest() != hashlib.sha256(dst[p].read_bytes()).digest())
if missing or extra or mismatched:
    if missing: print("MISSING:", *missing, sep="\n  ")
    if extra: print("UNEXPECTED:", *extra, sep="\n  ")
    if mismatched: print("MISMATCHED:", *mismatched, sep="\n  ")
    sys.exit(1)
print(f"Verified {len(src)} upstream assets: exact path parity and SHA-256 match.")
