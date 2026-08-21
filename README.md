# _init_ /files

**A file explorer, rebuilt for people who think in systems.**

![Downloads](https://img.shields.io/github/downloads/rKyzen/_init_-files/total?style=flat-square&label=downloads)

[![Download Latest Release](https://img.shields.io/badge/⬇%20Download-Latest%20Release-white?style=for-the-badge)](https://github.com/rKyzen/_init_-files/releases/latest)

`_init_ /files` is the first app in the `_init_` suite — a set of core system utilities for Android, designed for devices (like Nothing Phone) that ship without a native file manager. Where stock file explorers feel like leftover Android 4.0 UI, `_init_ /files` is built dark-first, monospace-native, and minimal by default.

---

## Why

Most Android OEMs either bundle the ancient Google Files app or skip a file manager entirely. `_init_ /files` exists to fill that gap with something that actually matches modern, minimal hardware design — fast, legible, and free of skeuomorphic clutter.

## Features

- **Browse** — internal storage, SD card, and USB OTG as distinct root entry points, with list/grid views and terminal-style breadcrumb paths
- **Quick Access** — auto-categorized shortcuts (Images, Videos, Audio, Documents, APKs, Archives), recents, and pinned folders
- **File Operations** — copy, move, rename, delete, share, zip/unzip, batch multi-select actions
- **Search** — filename and global search with type/size/date filters
- **Storage Analyzer** — visual breakdown of storage usage, largest files/folders, junk & cache detection
- **File Preview** — inline preview for images, text/code, video, audio, and APK metadata
- **Boot Sequence Splash** — a full-screen startup animation on cold launch, true to the `_init_` brand

## Design System

- **Headfont:** Michroma — used sparingly, in caps, for titles and section headers
- **Main font:** JetBrains Mono — used for everything else, with file metadata aligned like a terminal listing
- **Theme:** Dark-first, near-black background, no red anywhere in the UI — destructiveness is signaled through typography and iconography rather than color
- **Visual language:** Flat, outlined icons, generous whitespace, mechanical/precise motion — a dashboard feel over a "cute consumer app" feel

## Tech Stack

- Kotlin + Jetpack Compose (Material 3, fully re-themed)
- MVVM architecture
- Storage Access Framework (SAF) + MediaStore — fully scoped-storage compliant (Android 11+)
- Hilt for dependency injection
- Kotlin Coroutines + Flow
- WorkManager for background scans
- Room for local caching (folder trees, recents, favorites, search index)
- Media3 ExoPlayer for the startup splash video
- Min SDK 26 (Android 8.0)

## Project Status

🚧 In active development — core browse/search/file-ops build is underway, with storage analyzer, preview, and settings screens following.

## Roadmap

Planned for future releases:
- Duplicate file finder
- Cloud storage integration (Drive, Dropbox, OneDrive)
- App manager / APK backup
- Network access (SMB/FTP/WebDAV)
- Password-protected private vault
- Home-screen widget
- Terminal-style command bar
- Local device-to-device file transfer

## License

MIT

## Part of the `_init_` Suite

`_init_` is a boot-sequence-inspired suite of Android system utilities. `_init_ /files` is the first release, with future apps following the same `_init_ /[name]` naming convention.


