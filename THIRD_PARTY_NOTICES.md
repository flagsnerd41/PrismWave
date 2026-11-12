# Third-party notices

## ArchiveTune lyrics rendering

PrismWave's `ArchiveLyricsRenderer.kt` adapts portions of the lyrics rendering and text-wrapping implementation from **ArchiveTune**.

- Original project: https://github.com/rukamori/ArchiveTune
- Original author notice: © Rukamori
- Original license: GNU General Public License v3.0 (GPL-3.0)
- Adapted source: `app/src/main/java/com/prismwave/app/ArchiveLyricsRenderer.kt`
- Changes: integrated into PrismWave's existing lyrics stage, adapted to PrismWave's Compose UI and typography, and extended with an optional timed-word rendering interface.

The ArchiveTune-derived file retains its attribution notice. The complete GPL-3.0 license text is provided in the repository's `LICENSE` file.

PrismWave's current LRCLIB integration provides line-level LRC timestamps. The timed-word renderer is available for genuine word-timestamp data, but the app does not fabricate word timestamps from line-level lyrics.
