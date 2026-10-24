#!/usr/bin/env python3
"""
Starts a throwaway NeoForge server with the given mod, runs console commands, stops it, and says whether the mod
loaded without errors. Standard library only; the NeoForge counterpart of fabric_smoke.py.

  python3 -I .claude/scripts/neoforge_smoke.py --game 26.3 --mod neoforge/build/libs/redbull-neoforge-dev.jar \
      --expect "Red Bull 0.0.0-dev is on: 26 cans, 23 recipes"

NeoForge comes from maven.neoforged.net (the newest release for the game version, or --neoforge), installed once
into ~/.cache/neoforge-smoke; the other mods come from api.modrinth.com for that game version and NeoForge. The server
runs offline on a free port in a fresh folder (--dir, or a temporary one), so it never touches a real world. Exit
code 0 means clean.
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

CACHE = os.path.expanduser("~/.cache/neoforge-smoke")
AGENT = "MairwunNx/neoforge-smoke (github.com/MairwunNx)"
ANSI = re.compile(r"\x1b\[[0-9;]*m")
MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge"


def fetch(url):
    request = urllib.request.Request(url, headers={"User-Agent": AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


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


def neoforge(game, wanted, java):
    """NeoForge's server, installed once per version; returns its folder. 26.3 runs NeoForge 26.3.0.x, 26.3.1 runs 26.3.1.x."""
    if not wanted:
        prefix = game + (".0." if game.count(".") == 1 else ".")
        versions = re.findall(r"<version>([^<]+)</version>", fetch(f"{MAVEN}/maven-metadata.xml").decode())
        releases = [v for v in versions if v.startswith(prefix) and re.fullmatch(r"[\d.]+", v)]
        if not releases:
            sys.exit(f"NeoForge: no release for {game}")
        wanted = max(releases, key=lambda v: [int(p) for p in v.split(".")])
    print(f"NeoForge {wanted} for {game}")
    home = os.path.join(CACHE, f"server-{wanted}")
    if not os.path.exists(os.path.join(home, "libraries", "net", "neoforged", "neoforge", wanted, "unix_args.txt")):
        installer = download(f"{MAVEN}/{wanted}/neoforge-{wanted}-installer.jar", f"neoforge-{wanted}-installer.jar")
        os.makedirs(home, exist_ok=True)
        result = subprocess.run([java, "-jar", installer, "--install-server", home], capture_output=True, text=True)
        if result.returncode != 0:
            sys.exit(f"NeoForge's installer failed:\n{result.stdout[-2000:]}{result.stderr[-2000:]}")
    return home, wanted


def modrinth(spec, game):
    slug, _, wanted = spec.partition(":")
    query = urllib.parse.urlencode({"loaders": json.dumps(["neoforge"]), "game_versions": json.dumps([game])})
    versions = json.loads(fetch(f"https://api.modrinth.com/v2/project/{slug}/version?{query}"))
    if wanted:
        versions = [v for v in versions if v["version_number"] == wanted]
    if not versions:
        sys.exit(f"Modrinth {slug}: no version{' ' + wanted if wanted else ''} for NeoForge {game}")
    version = versions[0]
    file = next((f for f in version["files"] if f["primary"]), version["files"][0])
    print(f"Modrinth {slug} {version['version_number']}")
    return download(file["url"], file["filename"], sha512=file["hashes"]["sha512"])


def mod_id(jar):
    with zipfile.ZipFile(jar) as archive:
        descriptor = archive.read("META-INF/neoforge.mods.toml").decode()
    return re.search(r'^\s*modId\s*=\s*"([^"]+)"', descriptor, re.M).group(1)


def free_port():
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--game", required=True, help="game version, like 26.3")
    parser.add_argument("--neoforge", help="NeoForge version; the newest release for the game by default")
    parser.add_argument("--mod", action="append", default=[], help="a mod jar to test; repeat for more")
    parser.add_argument("--modrinth", action="append", default=[], help="a dependency from Modrinth, slug or slug:version")
    parser.add_argument("--command", action="append", default=[], help="a console command to run once the server is up")
    parser.add_argument("--expect", action="append", default=[], help="a regex the log must contain by the end")
    parser.add_argument("--ignore", action="append", default=[], help="a regex for error lines that don't count")
    parser.add_argument("--dir", help="the server folder; a temporary one by default")
    parser.add_argument("--java", default="java")
    parser.add_argument("--timeout", type=int, default=300, help="seconds to wait for the server to start")
    parser.add_argument("--keep", action="store_true", help="keep the server folder")
    args = parser.parse_args()

    home, version = neoforge(args.game, args.neoforge, args.java)
    folder = os.path.abspath(args.dir) if args.dir else tempfile.mkdtemp(prefix="neoforge-smoke-")
    os.makedirs(os.path.join(folder, "mods"), exist_ok=True)
    # The installed libraries stay in the cache; the fresh folder only points at them.
    libraries = os.path.join(folder, "libraries")
    if not os.path.exists(libraries):
        os.symlink(os.path.join(home, "libraries"), libraries)
    tested = {mod_id(jar): jar for jar in args.mod}
    for jar in args.mod + [modrinth(spec, args.game) for spec in args.modrinth]:
        shutil.copy(jar, os.path.join(folder, "mods", os.path.basename(jar)))
    with open(os.path.join(folder, "eula.txt"), "w") as file:
        file.write("eula=true\n")
    with open(os.path.join(folder, "server.properties"), "w") as file:
        file.write(f"online-mode=false\nserver-port={free_port()}\nspawn-protection=0\n")

    log_path = os.path.join(folder, "smoke.log")
    arguments = f"@libraries/net/neoforged/neoforge/{version}/{'win' if os.name == 'nt' else 'unix'}_args.txt"
    process = subprocess.Popen([args.java, "-Xmx2G", arguments, "nogui"], cwd=folder,
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
    error_lines = [line for line in dict.fromkeys(error_lines) if not any(re.search(pattern, line) for pattern in args.ignore)]
    if error_lines:
        problems.append(f"{len(error_lines)} error line(s)")
    for pattern in args.expect:
        if not any(re.search(pattern, line) for line in lines):
            problems.append(f"nothing in the log matches {pattern!r}")

    print()
    for line in lines:
        if "ModLauncher" in line and "starting" in line or any(i in line.lower() for i in tested) or "Done (" in line:
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
