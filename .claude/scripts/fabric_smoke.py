#!/usr/bin/env python3
"""
Starts a throwaway Fabric server with the given mod, runs console commands, stops it, and says whether the mod
loaded without errors. Standard library only.

  python3 -I .claude/scripts/fabric_smoke.py --game 26.3 --mod fabric/build/libs/redbull-fabric-dev.jar \
      --modrinth fabric-api --modrinth fabric-language-kotlin --expect "Red Bull dev is on"

The server launcher comes from meta.fabricmc.net (the newest stable loader, or --loader), the other mods from
api.modrinth.com for that game version. Downloads are cached in ~/.cache/fabric-smoke. The server runs offline on a
free port in a fresh folder (--dir, or a temporary one), so it never touches a real world. Exit code 0 means clean.
"""
import argparse
import hashlib
import json
import os
import re
import shutil
import socket
import subprocess
import sys
import tempfile
import threading
import time
import urllib.parse
import urllib.request
import zipfile

CACHE = os.path.expanduser("~/.cache/fabric-smoke")
AGENT = "MairwunNx/fabric-smoke (github.com/MairwunNx)"
ANSI = re.compile(r"\x1b\[[0-9;]*m")


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def download(url, name, sha512=None):
    """Into the cache, checked against the hash the API gave; a cached file with the right hash is reused."""
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)

    def matches():
        if not os.path.exists(path):
            return False
        if not sha512:
            return True
        digest = hashlib.sha512()
        with open(path, "rb") as file:
            for chunk in iter(lambda: file.read(1 << 20), b""):
                digest.update(chunk)
        return digest.hexdigest() == sha512

    if not matches():
        request = urllib.request.Request(url, headers={"User-Agent": AGENT})
        with urllib.request.urlopen(request, timeout=300) as response, open(path + ".part", "wb") as file:
            shutil.copyfileobj(response, file)
        os.replace(path + ".part", path)
        if not matches():
            sys.exit(f"{name}: hash mismatch after download")
    return path


def server(game, loader):
    if not loader:
        loader = next(v["version"] for v in fetch_json("https://meta.fabricmc.net/v2/versions/loader") if v["stable"])
    installer = next(v["version"] for v in fetch_json("https://meta.fabricmc.net/v2/versions/installer") if v["stable"])
    print(f"Fabric Loader {loader} for {game}, installer {installer}")
    url = f"https://meta.fabricmc.net/v2/versions/loader/{game}/{loader}/{installer}/server/jar"
    return download(url, f"fabric-server-{game}-{loader}-{installer}.jar")


def modrinth(spec, game):
    slug, _, wanted = spec.partition(":")
    query = urllib.parse.urlencode({"loaders": json.dumps(["fabric"]), "game_versions": json.dumps([game])})
    versions = fetch_json(f"https://api.modrinth.com/v2/project/{slug}/version?{query}")
    if wanted:
        versions = [v for v in versions if v["version_number"] == wanted]
    if not versions:
        sys.exit(f"Modrinth {slug}: no version{' ' + wanted if wanted else ''} for Fabric {game}")
    version = versions[0]
    file = next((f for f in version["files"] if f["primary"]), version["files"][0])
    print(f"Modrinth {slug} {version['version_number']}")
    return download(file["url"], file["filename"], sha512=file["hashes"]["sha512"])


def mod_id(jar):
    with zipfile.ZipFile(jar) as archive:
        return json.loads(archive.read("fabric.mod.json"))["id"]


def free_port():
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--game", required=True, help="game version, like 26.3")
    parser.add_argument("--loader", help="Fabric Loader version; the newest stable one by default")
    parser.add_argument("--mod", action="append", default=[], help="a mod jar to test; repeat for more")
    parser.add_argument("--modrinth", action="append", default=[], help="a dependency from Modrinth, slug or slug:version")
    parser.add_argument("--command", action="append", default=[], help="a console command to run once the server is up")
    parser.add_argument("--expect", action="append", default=[], help="a regex the log must contain by the end")
    parser.add_argument("--dir", help="the server folder; a temporary one by default")
    parser.add_argument("--java", default="java")
    parser.add_argument("--timeout", type=int, default=300, help="seconds to wait for the server to start")
    parser.add_argument("--keep", action="store_true", help="keep the server folder")
    args = parser.parse_args()

    folder = os.path.abspath(args.dir) if args.dir else tempfile.mkdtemp(prefix="fabric-smoke-")
    os.makedirs(os.path.join(folder, "mods"), exist_ok=True)
    launcher = server(args.game, args.loader)
    tested = {mod_id(jar): jar for jar in args.mod}
    for jar in args.mod + [modrinth(spec, args.game) for spec in args.modrinth]:
        shutil.copy(jar, os.path.join(folder, "mods", os.path.basename(jar)))
    with open(os.path.join(folder, "eula.txt"), "w") as file:
        file.write("eula=true\n")
    with open(os.path.join(folder, "server.properties"), "w") as file:
        file.write(f"online-mode=false\nserver-port={free_port()}\nspawn-protection=0\n")

    log_path = os.path.join(folder, "smoke.log")
    process = subprocess.Popen([args.java, "-Xmx2G", "-jar", launcher, "nogui"], cwd=folder,
                               stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, errors="replace")
    lines = []
    started = threading.Event()

    def read():
        with open(log_path, "w") as log:
            for raw in process.stdout:
                line = ANSI.sub("", raw.rstrip("\n"))
                lines.append(line)
                log.write(line + "\n")
                if re.search(r"Done \([\d.,]+s\)!", line):
                    started.set()

    reader = threading.Thread(target=read, daemon=True)
    reader.start()

    def console(command):
        process.stdin.write(command + "\n")
        process.stdin.flush()

    up = started.wait(args.timeout)
    if up:
        time.sleep(3)
        for command in args.command:
            console(command)
            time.sleep(2)
        console("stop")
    else:
        print(f"The server didn't start within {args.timeout} s")
    try:
        process.wait(120)
    except subprocess.TimeoutExpired:
        process.kill()
    reader.join(10)

    problems = [] if up else ["the server didn't start"]
    # A stack trace names the mod's package on an "at" line; the loader and the game name a broken mod by its id.
    packages = set()
    for jar in tested.values():
        with zipfile.ZipFile(jar) as archive:
            for entry in archive.namelist():
                if entry.endswith(".class") and entry.count("/") >= 2:
                    packages.add(".".join(entry.split("/")[:3]))
    ids = "|".join(re.escape(i) for i in tested) or "(?!)"
    failing = re.compile(rf"(ERROR\]|/ERROR|Exception|Could not|Failed|Incompatible).*\b({ids})\b", re.I)
    error_lines = [line for line in lines if failing.search(line)]
    error_lines += [line for line in lines if line.strip().startswith("at ") and any(p in line for p in packages)]
    error_lines += [line for line in lines if re.search(r"/ERROR\]|\bERROR\]", line)]
    error_lines = list(dict.fromkeys(error_lines))
    if error_lines:
        problems.append(f"{len(error_lines)} error line(s)")
    for pattern in args.expect:
        if not any(re.search(pattern, line) for line in lines):
            problems.append(f"nothing in the log matches {pattern!r}")

    print()
    for line in lines:
        if "Loading Minecraft" in line or "Loading " in line and " mods:" in line or any(f"[{i}" in line.lower() for i in tested) \
                or "Red Bull" in line or "Done (" in line:
            print("  " + line)
    if error_lines:
        print("\nError lines:")
        for line in error_lines[:40]:
            print("  " + line)
    if problems or args.keep or args.dir:
        print(f"\nLog: {log_path}")
    else:
        shutil.rmtree(folder, ignore_errors=True)
    print(("FAILED: " + "; ".join(problems)) if problems else ("OK: " + ", ".join(tested) + " loaded cleanly"))
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
