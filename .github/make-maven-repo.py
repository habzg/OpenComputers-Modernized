#!/usr/bin/env python3
import hashlib
import json
import os
import re
import shutil
import sys
import urllib.request
from datetime import datetime, timezone
from html import escape

REPO_OWNER = "habzg"
REPO_NAME = "OpenComputers-Modernized"
REPO_URL = f"https://github.com/{REPO_OWNER}/{REPO_NAME}"
REPO_GIT_URL = f"git@github.com:{REPO_OWNER}/{REPO_NAME}.git"
PAGES_URL = f"https://{REPO_OWNER}.github.io/{REPO_NAME}/"
API_URL = f"https://api.github.com/repos/{REPO_OWNER}/{REPO_NAME}"

DEFAULT_GROUP = "li.cil.oc"

JAR_RE = re.compile(r"^opencomputers-(.+)-(\d+\.\d+\.\d+(?:[-+.][0-9A-Za-z]+)*)\.jar$")

AUTHORS = [
    ("Florian 'Sangar' Nuecke", "Original author"),
    ("Johannes 'Lord Joda' Lohrer", "Original author"),
    ("habzg", "Port author"),
    ("Everyone who contributed to the mod on Github", "Contributor (thank you!)"),
]

VARIANT_NAMES = {
    "api": "OpenComputers: Modernized (API)",
    "fabric": "OpenComputers: Modernized (Fabric)",
    "fabric-api": "OpenComputers: Modernized (Fabric API)",
    "neoforge": "OpenComputers: Modernized (NeoForge)",
    "neoforge-api": "OpenComputers: Modernized (NeoForge API)",
}

README_DESCRIPTION = (
    "OpenComputers: Modernized (OC:M) is a fork of the original "
    "OpenComputers mod for modern versions of Minecraft."
)

API_DESCRIPTION = (
    "The API for OpenComputers: Modernized (OC:M), a fork of the original "
    "OpenComputers mod for modern versions of Minecraft."
)

OUT_DIR = "maven"


def log(msg):
    print(msg, flush=True)


def version_key(version):
    parts = re.split(r"[-+.]", version)
    key = []
    for part in parts:
        try:
            key.append(("n", int(part)))
        except ValueError:
            key.append(("s", part))
    return tuple(key)


def write_checksums(path):
    with open(path, "rb") as fh:
        data = fh.read()
    for digest, ext in (
        (hashlib.md5, "md5"),
        (hashlib.sha1, "sha1"),
        (hashlib.sha256, "sha256"),
        (hashlib.sha512, "sha512"),
    ):
        with open(path + "." + ext, "w", encoding="ascii") as fh:
            fh.write(digest(data).hexdigest() + "\n")


def pom(artifact_id, version, variant):
    name = VARIANT_NAMES.get(variant, f"OpenComputers: Modernized ({variant})")
    description = API_DESCRIPTION if variant in ("api", "fabric-api", "neoforge-api") else README_DESCRIPTION
    developer_entries = "\n".join(
        "    <developer>\n"
        f"      <name>{dname}</name>\n"
        "      <roles>\n"
        f"        <role>{drole}</role>\n"
        "      </roles>\n"
        "    </developer>"
        for dname, drole in AUTHORS
    )
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>{DEFAULT_GROUP}</groupId>
  <artifactId>{artifact_id}</artifactId>
  <version>{version}</version>
  <packaging>jar</packaging>
  <name>{name}</name>
  <description>{description}</description>
  <url>{REPO_URL}</url>
  <licenses>
    <license>
      <name>MIT License</name>
      <url>{REPO_URL}/blob/main/LICENSE</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <developers>
{developer_entries}
  </developers>
  <issueManagement>
    <system>GitHub Issues</system>
    <url>{REPO_URL}/issues</url>
  </issueManagement>
  <scm>
    <connection>scm:git:{REPO_GIT_URL}</connection>
    <developerConnection>scm:git:{REPO_GIT_URL}</developerConnection>
    <url>{REPO_URL}</url>
    <tag>{version}</tag>
  </scm>
  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <maven.compiler.release>21</maven.compiler.release>
  </properties>
  <distributionManagement>
    <repository>
      <id>habzg</id>
      <name>OpenComputers: Modernized Maven Repository</name>
      <url>{PAGES_URL}</url>
    </repository>
  </distributionManagement>
</project>
"""


def metadata_marker(artifact_id, versions, last_updated):
    latest = versions[-1]
    version_entries = "\n".join(f"      <version>{v}</version>" for v in versions)
    return f"""<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>{DEFAULT_GROUP}</groupId>
  <artifactId>{artifact_id}</artifactId>
  <versioning>
    <latest>{latest}</latest>
    <release>{latest}</release>
    <versions>
{version_entries}
    </versions>
    <lastUpdated>{last_updated}</lastUpdated>
  </versioning>
</metadata>
"""


def github_api_get(path):
    url = f"{API_URL}/{path}" if not path.startswith("http") else path
    req = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)


def download_all_releases(dl_dir):
    os.makedirs(dl_dir, exist_ok=True)
    page = 1
    total_assets = 0
    while True:
        log(f"fetching page {page}")
        releases = github_api_get(f"releases?page={page}&per_page=100")
        if not releases:
            break
        for release in releases:
            tag = release.get("tag_name", "unknown")
            assets = {}
            for asset in release.get("assets", []):
                name = asset["name"]
                if name.startswith("opencomputers-") and name.endswith(".jar"):
                    assets[name] = asset["browser_download_url"]
            if not assets:
                continue
            log(f"  {tag}: {len(assets)} jar(s)")
            for name, url in sorted(assets.items()):
                dest = os.path.join(dl_dir, name)
                req = urllib.request.Request(url)
                with urllib.request.urlopen(req, timeout=120) as resp, open(dest, "wb") as out:
                    shutil.copyfileobj(resp, out)
                total_assets += 1
        page += 1
    return collect_jars(dl_dir)


def collect_jars(dl_dir):
    jars = {}
    for name in sorted(os.listdir(dl_dir)):
        if not name.endswith(".jar"):
            continue
        m = JAR_RE.match(name)
        if not m:
            continue
        variant, version = m.group(1), m.group(2)
        jars.setdefault(variant, {})[version] = os.path.join(dl_dir, name)
    return jars


def generate(out_dir, jars):
    os.makedirs(out_dir, exist_ok=True)
    group_dir = os.path.join(out_dir, DEFAULT_GROUP.replace(".", os.sep))
    if os.path.isdir(group_dir):
        shutil.rmtree(group_dir)

    last_updated = datetime.now(timezone.utc).strftime("%Y%m%d%H%M%S")
    generated = 0

    for variant in sorted(jars):
        artifact = "opencomputers-" + variant
        versions = sorted(jars[variant], key=version_key)
        for version in versions:
            jar_path = jars[variant][version]
            dir_version = os.path.join(out_dir, DEFAULT_GROUP.replace(".", os.sep), artifact, version)
            os.makedirs(dir_version, exist_ok=True)

            jar_name = f"{artifact}-{version}.jar"
            pom_name = f"{artifact}-{version}.pom"

            shutil.copy2(jar_path, os.path.join(dir_version, jar_name))
            write_checksums(os.path.join(dir_version, jar_name))

            pom_path = os.path.join(dir_version, pom_name)
            with open(pom_path, "w", encoding="utf-8", newline="\n") as fh:
                fh.write(pom(artifact, version, variant))
            write_checksums(pom_path)

            generated += 1
            log(f"generated {DEFAULT_GROUP}:{artifact}:{version}")

        meta_name = "maven-metadata.xml"
        meta_path = os.path.join(out_dir, DEFAULT_GROUP.replace(".", os.sep), artifact, meta_name)
        with open(meta_path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(metadata_marker(artifact, versions, last_updated))
        write_checksums(meta_path)
    return generated


MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]


def apache_size(size):
    value = float(size)
    units = ["K", "M", "G", "T"]
    unit = -1
    while value >= 1024 and unit < len(units) - 1:
        value /= 1024
        unit += 1
    if unit == -1:
        return str(int(value))
    return f"{value:.1f}{units[unit]}" if value < 10 else f"{round(value)}{units[unit]}"


def apache_time(timestamp):
    dt = datetime.fromtimestamp(timestamp, tz=timezone.utc)
    return f"{dt.day:02d}-{MONTHS[dt.month - 1]}-{dt.year} {dt.hour:02d}:{dt.minute:02d}"


def index_page(rel_path, entries):
    is_root = not rel_path or rel_path == "."
    title = "Index of /" if is_root else f"Index of /{rel_path}/"
    labels = [escape(name) + ("/" if is_dir else "") for name, _m, _s, is_dir in entries]
    name_width = max([len(label) for label in labels] or [0]) + 5
    lines = ['<pre><a href="../">../</a>']
    for label, (name, mtime, size, is_dir) in zip(labels, entries):
        size_col = "-" if is_dir else apache_size(size)
        line = f'<a href="{label}">{label}</a>'
        line += " " * (name_width - len(label))
        line += f"{apache_time(mtime)}  {size_col:>8}"
        lines.append(line)
    body = "\n".join(lines)
    return f"""<html>
<head><title>{escape(title)}</title></head>
<body bgcolor="white">
<h1>{escape(title)}</h1><hr>{body}
</pre><hr></body>
</html>
"""


def generate_indexes(out_dir):
    for root, dirs, files in os.walk(out_dir):
        dirs.sort()
        entries = []
        for name in dirs:
            path = os.path.join(root, name)
            entries.append((name, os.path.getmtime(path), 0, True))
        for name in sorted(files):
            if name.startswith(".") or name == "index.html":
                continue
            path = os.path.join(root, name)
            entries.append((name, os.path.getmtime(path), os.path.getsize(path), False))
        rel_path = os.path.relpath(root, out_dir)
        rel_path = "" if rel_path == "." else rel_path.replace(os.sep, "/")
        with open(os.path.join(root, "index.html"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(index_page(rel_path, entries))
    with open(os.path.join(out_dir, ".nojekyll"), "w", encoding="ascii") as fh:
        fh.write("")


def main():
    dl_dir = "maven-dl"
    out_dir = OUT_DIR

    jars = download_all_releases(dl_dir)
    if not jars:
        log("No opencomputers-*.jar found.")
        sys.exit(1)

    generate(out_dir, jars)
    generate_indexes(out_dir)

    log("\nDone.")


if __name__ == "__main__":
    main()
