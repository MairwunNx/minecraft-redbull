#!/usr/bin/env python3
"""
Starts a throwaway Paper server with the given plugins, runs console commands, stops it, and says whether
every plugin loaded and enabled without errors. Standard library only.

  python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin build/libs/x.jar \
      --modrinth placeholderapi --command "papi list" --expect "allay"

Paper comes from fill.papermc.io (the newest beta or stable build, or --build), Modrinth plugins from api.modrinth.com
for that game version. Downloads are cached in ~/.cache/paper-smoke. The server runs offline on a free port in
a fresh folder (--dir, or a temporary one), so it never touches a real world. Exit code 0 means clean.
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

CACHE = os.path.expanduser("~/.cache/paper-smoke")
AGENT = "MairwunNx/paper-smoke (github.com/MairwunNx)"
ANSI = re.compile(r"\x1b\[[0-9;]*m")


def fetch_json(url):
    request = urllib.request.Request(url, headers={"User-Agent": AGENT})
    with urllib.request.urlopen(request, timeout=60) as response:
        return json.load(response)


def download(url, name, sha256=None, sha512=None):
    """Into the cache, checked against the hash the API gave; a cached file with the right hash is reused."""
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, name)
    algorithm, expected = ("sha256", sha256) if sha256 else ("sha512", sha512)

    def matches():
        if not expected or not os.path.exists(path):
            return os.path.exists(path) and not expected
        digest = hashlib.new(algorithm)
        with open(path, "rb") as file:
            for chunk in iter(lambda: file.read(1 << 20), b""):
                digest.update(chunk)
        return digest.hexdigest() == expected

    if not matches():
        request = urllib.request.Request(url, headers={"User-Agent": AGENT})
        with urllib.request.urlopen(request, timeout=300) as response, open(path + ".part", "wb") as file:
            shutil.copyfileobj(response, file)
        os.replace(path + ".part", path)
        if not matches():
            sys.exit(f"{name}: hash mismatch after download")
    return path


def paper(game, build):
    base = f"https://fill.papermc.io/v3/projects/paper/versions/{game}/builds"
    if build:
        info = fetch_json(f"{base}/{build}")
    else:
        # Newest first; an alpha build isn't ready to test against.
        info = next((b for b in fetch_json(base) if b.get("channel") in ("BETA", "STABLE")), None)
        if not info:
            sys.exit(f"Paper {game} has no beta or stable build yet")
    file = info["downloads"]["server:default"]
    print(f"Paper {game} build {info['id']} ({info.get('channel', '?')})")
    return download(file["url"], file["name"], sha256=file["checksums"]["sha256"])


def modrinth(spec, game):
    slug, _, wanted = spec.partition(":")
    query = urllib.parse.urlencode({"loaders": json.dumps(["paper", "spigot", "bukkit"]), "game_versions": json.dumps([game])})
    versions = fetch_json(f"https://api.modrinth.com/v2/project/{slug}/version?{query}")
    if wanted:
        versions = [v for v in versions if v["version_number"] == wanted]
    if not versions:
        sys.exit(f"Modrinth {slug}: no version{' ' + wanted if wanted else ''} for Paper {game}")
    version = versions[0]
    file = next((f for f in version["files"] if f["primary"]), version["files"][0])
    print(f"Modrinth {slug} {version['version_number']}")
    return download(file["url"], file["filename"], sha512=file["hashes"]["sha512"])


def plugin_name(jar):
    """The name in paper-plugin.yml or plugin.yml, as the server logs it."""
    with zipfile.ZipFile(jar) as archive:
        for entry in ("paper-plugin.yml", "plugin.yml"):
            if entry in archive.namelist():
                for line in archive.read(entry).decode("utf-8", "replace").splitlines():
                    match = re.match(r"name:\s*['\"]?([^'\"#]+?)['\"]?\s*$", line)
                    if match:
                        return match.group(1)
    sys.exit(f"{jar}: no plugin.yml or paper-plugin.yml with a name")


def free_port():
    with socket.socket() as probe:
        probe.bind(("127.0.0.1", 0))
        return probe.getsockname()[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--game", required=True, help="game version, like 26.3")
    parser.add_argument("--build", help="Paper build number; the newest beta or stable one by default")
    parser.add_argument("--plugin", action="append", default=[], help="a plugin jar to test, a path or an https URL; repeat for more")
    parser.add_argument("--modrinth", action="append", default=[], help="a dependency from Modrinth, slug or slug:version")
    parser.add_argument("--jar", action="append", default=[], help="another jar to install without checking it: a path or an https URL")
    parser.add_argument("--command", action="append", default=[], help="a console command to run once the server is up")
    parser.add_argument("--expect", action="append", default=[], help="a regex the log must contain by the end")
    parser.add_argument("--dir", help="the server folder; a temporary one by default")
    parser.add_argument("--java", default="java")
    parser.add_argument("--timeout", type=int, default=300, help="seconds to wait for the server to start")
    parser.add_argument("--ignore", action="append", default=[], help="a regex for error lines a fresh server always logs, like a plugin asking to accept a EULA")
    parser.add_argument("--all", action="store_true", help="check every installed plugin, not only --plugin ones")
    parser.add_argument("--keep", action="store_true", help="keep the server folder")
    args = parser.parse_args()

    folder = os.path.abspath(args.dir) if args.dir else tempfile.mkdtemp(prefix="paper-smoke-")
    os.makedirs(os.path.join(folder, "plugins"), exist_ok=True)
    server_jar = paper(args.game, args.build)
    local = lambda jar: download(jar, os.path.basename(urllib.parse.urlparse(jar).path)) if jar.startswith("https://") else jar
    plugins = [local(jar) for jar in args.plugin]
    tested = {plugin_name(jar): jar for jar in plugins}
    others = [local(jar) for jar in args.jar] + [modrinth(spec, args.game) for spec in args.modrinth]
    if args.all:
        tested.update({plugin_name(jar): jar for jar in others})
    for jar in plugins + others:
        shutil.copy(jar, os.path.join(folder, "plugins", os.path.basename(jar)))
    with open(os.path.join(folder, "eula.txt"), "w") as file:
        file.write("eula=true\n")
    with open(os.path.join(folder, "server.properties"), "w") as file:
        file.write(f"online-mode=false\nserver-port={free_port()}\nlevel-type=minecraft\\:flat\ngenerate-structures=false\nspawn-protection=0\n")
    # Plugins with a web server of their own get a free port, so runs side by side don't fight over one.
    for plugin, config in {"Plan": f"Webserver:\n  Port: {free_port()}\n"}.items():
        os.makedirs(os.path.join(folder, "plugins", plugin), exist_ok=True)
        with open(os.path.join(folder, "plugins", plugin, "config.yml"), "w") as file:
            file.write(config)

    log_path = os.path.join(folder, "smoke.log")
    process = subprocess.Popen([args.java, "-Xmx2G", "-jar", server_jar, "--nogui"], cwd=folder,
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
    for name in tested:
        if not any(re.search(rf"\[{re.escape(name)}\] Enabling {re.escape(name)}", line) for line in lines):
            problems.append(f"{name} never enabled")
    names = "|".join(re.escape(n) for n in tested) or "(?!)"
    failing = re.compile(rf"(ERROR\]|Exception|Error:|Could not load|NoClassDefFound|NoSuchMethod|NoSuchField).*?({names})|({names}).*?(ERROR\]|Exception|NoClassDefFound|NoSuchMethod)", re.I)
    trace_lines = [line for line in lines if failing.search(line)]
    # A stack trace names the plugin's package on an "at" line rather than its name.
    packages = set()
    for jar in tested.values():
        with zipfile.ZipFile(jar) as archive:
            for entry in archive.namelist():
                if entry.endswith(".class") and "/libs/" not in entry and entry.count("/") >= 2:
                    packages.add(".".join(entry.split("/")[:2]))
    trace_lines += [line for line in lines if line.strip().startswith("at ") and any(p in line for p in packages) and "/libs/" not in line]
    # Plan logs a failing extension method without the plugin's name, and writes the trace to its own logs folder,
    # which stays empty on a clean run.
    trace_lines += [line for line in lines if "DataExtensionMethodCallException" in line]
    plan_logs = os.path.join(folder, "plugins", "Plan", "logs")
    if os.path.isdir(plan_logs):
        trace_lines += [f"Plan wrote an error log: plugins/Plan/logs/{name}" for name in sorted(os.listdir(plan_logs))]
    trace_lines = [line for line in trace_lines if not any(re.search(pattern, line) for pattern in args.ignore)]
    if trace_lines:
        problems.append(f"{len(trace_lines)} error line(s) about the plugins")
    for pattern in args.expect:
        if not any(re.search(pattern, line) for line in lines):
            problems.append(f"nothing in the log matches {pattern!r}")

    print()
    for line in lines:
        if any(f"[{n}]" in line for n in tested) or "Paper version" in line or line.startswith("This server is running"):
            print("  " + line)
    if trace_lines:
        print("\nError lines:")
        for line in trace_lines[:40]:
            print("  " + line)
    # A failed run keeps its folder for a look at the full log.
    if problems or args.keep or args.dir:
        print(f"\nLog: {log_path}")
    else:
        shutil.rmtree(folder, ignore_errors=True)
    print(("FAILED: " + "; ".join(problems)) if problems else ("OK: " + ", ".join(tested) + " loaded and enabled cleanly"))
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
