"""Upload the mods' jars to Modrinth as versions of the project, each marked to run on the client and the server.

Standard library only: MODRINTH_TOKEN=… python3 -I scripts/modrinth_mods.py --version X.Y.Z --notes release-notes.md
The release workflow runs it after `./gradlew build`. Minotaur can't set a version's `environment`, which Modrinth
asks of every mod version (and refuses a version without while the project is in review), so the mods go up through
Modrinth's v3 API here; Paper's jar still goes through Minotaur. A version already uploaded is skipped.
"""

import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
PROJECT = "minecraft-redbull"
API = "https://api.modrinth.com"
AGENT = "MairwunNx/minecraft-redbull (github.com/MairwunNx)"
MODS = {
    # loader: its name on the version, and the projects it can't run without
    "fabric": ("Fabric", ["fabric-api", "fabric-language-kotlin"]),
    "neoforge": ("NeoForge", []),
}


def request(method, path, token, body=None, headers=None):
    req = urllib.request.Request(f"{API}{path}", data=body, method=method,
                                 headers={"User-Agent": AGENT, "Authorization": token, **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=120) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        sys.exit(f"{method} {path}: {error.code} {error.read().decode(errors='replace')}")


def project_id(slug, token):
    return request("GET", f"/v2/project/{slug}", token)["id"]


def multipart(data, file_name, file_bytes):
    boundary = uuid.uuid4().hex
    parts = [
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"data\"\r\nContent-Type: application/json\r\n\r\n".encode(),
        json.dumps(data).encode(), b"\r\n",
        f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{file_name}\"\r\n"
        f"Content-Type: application/java-archive\r\n\r\n".encode(),
        file_bytes, f"\r\n--{boundary}--\r\n".encode(),
    ]
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--version", required=True)
    parser.add_argument("--notes", required=True, help="a Markdown file with the release notes")
    args = parser.parse_args()
    token = os.environ.get("MODRINTH_TOKEN") or sys.exit("MODRINTH_TOKEN isn't set")
    notes = Path(args.notes).read_text(encoding="utf-8")
    project = project_id(PROJECT, token)
    uploaded = {v["version_number"] for v in request("GET", f"/v2/project/{project}/version", token)}

    for loader, (name, needs) in MODS.items():
        number = f"{args.version}+{loader}"
        if number in uploaded:
            print(f"{number}: already on Modrinth")
            continue
        game = re.search(r'val minecraft = "([^"]+)"', (ROOT / loader / "build.gradle.kts").read_text()).group(1)
        jar = ROOT / loader / "build/libs" / f"redbull-{loader}-{args.version}.jar"
        data = {
            "name": f"Red Bull {args.version} for {name}", "version_number": number, "changelog": notes,
            "game_versions": [game], "loaders": [loader], "version_type": "release", "featured": True,
            "project_id": project, "status": "listed", "environment": "client_and_server",
            "dependencies": [{"project_id": project_id(slug, token), "dependency_type": "required"} for slug in needs],
            "file_parts": ["file"], "primary_file": "file",
        }
        body, content_type = multipart(data, jar.name, jar.read_bytes())
        version = request("POST", "/v3/version", token, body, {"Content-Type": content_type})
        print(f"{number}: {version['id']}, {version.get('environment')}")


if __name__ == "__main__":
    main()
