"""Upload a jar to CurseForge as a release of a project: a Paper plugin to dev.bukkit.org, a mod to minecraft.curseforge.com.

Standard library only:
    CURSEFORGE_TOKEN=… python3 -I scripts/curseforge.py --project 1766137 --kind plugin --game 26.3 \
        --name "Mo’Bosses 2.11.0" --notes release-notes.md paper/build/libs/mobosses-paper-2.11.0.jar
    … --kind mod --loader fabric --requires fabric-api fabric/build/libs/mobosses-fabric-2.11.0.jar
The release workflow runs it after `./gradlew build` (from tools/ in Allay). CurseForge has no API to create a
project; one is made on authors.curseforge.com, and a project holds one class, so plugins and mods live in separate
projects. Game versions are looked up by name, so a new one needs no change here. The file is the same in every
project of the family.
"""

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

HOSTS = {"plugin": "https://dev.bukkit.org", "mod": "https://minecraft.curseforge.com"}
AGENT = "MairwunNx/minecraft (github.com/MairwunNx)"
LOADERS = {"fabric": "Fabric", "neoforge": "NeoForge"}


def request(url, token, body=None, content_type=None):
    headers = {"User-Agent": AGENT, "X-Api-Token": token}
    if content_type:
        headers["Content-Type"] = content_type
    try:
        with urllib.request.urlopen(urllib.request.Request(url, data=body, headers=headers), timeout=300) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        sys.exit(f"{url}: {error.code} {error.read().decode(errors='replace')}")


def version_ids(host, token, kind, game, loader, java):
    """CurseForge's ids for the game version, and for a mod also its loader, sides, and Java."""
    versions = request(f"{host}/api/game/versions", token)
    types = {t["id"]: t["slug"] for t in request(f"{host}/api/game/version-types", token)}

    def find(name, type_ok):
        found = [v["id"] for v in versions if v["name"] == name and type_ok(types.get(v["gameVersionTypeID"], ""))]
        return found[0] if found else sys.exit(f"CurseForge doesn't know “{name}” yet")

    if kind == "plugin":
        return [find(game, lambda slug: slug == "bukkit")]
    return [find(game, lambda slug: slug.startswith("minecraft-")), find(LOADERS[loader], lambda slug: slug == "modloader"),
            find("Server", lambda slug: slug == "environment"), find("Client", lambda slug: slug == "environment"),
            find(f"Java {java}", lambda slug: slug == "java")]


def multipart(metadata, jar):
    boundary = uuid.uuid4().hex
    body = b"".join([
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"metadata\"\r\n\r\n".encode(),
        json.dumps(metadata).encode(), b"\r\n",
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{jar.name}\"\r\n"
        f"Content-Type: application/java-archive\r\n\r\n".encode(),
        jar.read_bytes(), f"\r\n--{boundary}--\r\n".encode(),
    ])
    return body, f"multipart/form-data; boundary={boundary}"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("jar", type=Path)
    parser.add_argument("--project", required=True, help="the project's id on CurseForge")
    parser.add_argument("--kind", required=True, choices=HOSTS)
    parser.add_argument("--game", required=True, help="the Minecraft version, like 26.3")
    parser.add_argument("--name", required=True, help="the file's display name")
    parser.add_argument("--notes", required=True, help="a Markdown file with the release notes")
    parser.add_argument("--loader", choices=LOADERS, help="a mod's loader")
    parser.add_argument("--java", default="25")
    parser.add_argument("--requires", action="append", default=[], help="a CurseForge project a mod needs, by slug; repeat it")
    args = parser.parse_args()
    if (args.kind == "mod") != bool(args.loader):
        sys.exit("--loader goes with --kind mod, and only there")
    token = os.environ.get("CURSEFORGE_TOKEN") or sys.exit("CURSEFORGE_TOKEN isn't set")
    host = HOSTS[args.kind]

    metadata = {
        "changelog": Path(args.notes).read_text(encoding="utf-8"), "changelogType": "markdown",
        "displayName": args.name, "releaseType": "release",
        "gameVersions": version_ids(host, token, args.kind, args.game, args.loader, args.java),
    }
    if args.requires:
        metadata["relations"] = {"projects": [{"slug": slug, "type": "requiredDependency"} for slug in args.requires]}
    body, content_type = multipart(metadata, args.jar)
    uploaded = request(f"{host}/api/projects/{args.project}/upload-file", token, body, content_type)
    print(f"{args.jar.name}: file {uploaded['id']} in project {args.project}")


if __name__ == "__main__":
    main()
