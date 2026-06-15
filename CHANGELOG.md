# Changelog

All notable changes to this project will be documented in this file.

## [1.3] - 2026-06-15

### Added
- **Settings Tab (Einstellungen):** Introduced a 4th tab containing connection configurations, diagnostics, and backup tools.
- **Connection Diagnostics:** Added a "Verbindung testen" (Test Connection) button to dynamically test API connectivity for both Home Assistant and MQTT Broker asynchronously.
- **Local JSON Backup & Restore:**
  - Export all configuration parameters to a `cornerlays_backup.json` file.
  - Custom built-in folder picker optimized for Android TV D-pads to select the export location.
  - Custom built-in file picker to browse and import JSON configuration files directly, automatically reloading widgets and restarting services.
- **Remote Key Event Dimmer Reset:** Accessibility service intercepts physical remote control keystrokes to instantly dismiss active screen dimming overlays and reset opacity back to 0.
- **Decimal Localization:** Numeric states (e.g. temperatures) now match the device's language and region formatting (e.g. displays `21,5` instead of `21.5` on German systems).

### Fixed
- Fixed app crashes when opening Home Assistant overlay slots.
- Restored missing `DimmerService` accessibility service declaration and XML capability configurations.
- Fixed tab navigation order and D-pad focus flow inside the Settings screen.
- Removed hardcoded padding spaces around prefixes and suffixes to allow fully customized user spacing.
