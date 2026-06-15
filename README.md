# Cornerlays

**Add highly customizable Clock, Date, Weather, and Home Assistant overlays to your Android TV screen.**

Cornerlays is a simple yet powerful app designed for Android TV and other Android devices that allows you to display persistent, glanceable information right where you need it.


![main](https://github.com/user-attachments/assets/cae434a1-df6d-4188-aa95-bf5780f74cc3)

## 

## ✨ Features

  * **Multiple Core Widgets:**
      * Display the current Time, Date, Weather, and Home Assistant entity states simultaneously.
  * **Home Assistant Integration:**
      * Display the live state of any entity from your Home Assistant instance.
      * Configure up to three independent Home Assistant overlays.
      * **Special Display Modes:** Includes a "Countdown" mode to show the time remaining from a timestamp entity (e.g., `input_datetime` or a calendar event) and a "TimeOnly" mode for time-only helpers (e.g. `12:30:00` formatted to `12:30` hh:mm).
      * **Custom Formatting:** Add custom text or units before and after the entity's state (including customized space padding).
      * **Entity Picker:** Easily browse and select from a list of your available entities directly within the app.
      * Option to hide an overlay if the corresponding entity is unavailable.
  * **Android TV State Broadcasting & Control (MQTT):**
      * Broadcasts active application name, remote control keystrokes, focused UI elements, and media details (title, artist, duration, playback state) to Home Assistant.
      * Allows remote control of screen dimming and execution of custom intent actions via MQTT subscriptions.
  * **Clock Widget:**
      * Switch between 12-hour and 24-hour formats.
  * **Date Widget:**
      * Choose from a variety of common date formats (e.g., `DD.MM.YYYY`, `MM/DD/YY`, `Weekday, Day.Month`).
  * **Weather Widget:**
      * Get current temperature for any city worldwide.
      * Switch between Celsius and Fahrenheit.
  * **Deep Customization (for all widgets):**
      * **Positioning:** Place each widget in one of the four corners of the screen.
      * **Fine-Tuning:** Nudge each widget pixel by pixel for perfect alignment using the D-Pad.
      * **Appearance:** Adjust font size, font color, and outline/shadow color for maximum readability on any background.
  * **Settings Tab ("Einstellungen") & Connection Diagnostics:**
      * A dedicated settings tab containing all connection configurations (HA, MQTT) and tools.
      * Asynchronous connection diagnostics to test Home Assistant REST API and MQTT Broker connections dynamically.
  * **Local Backup & Restore (JSON):**
      * Export and import all SharedPreferences configurations into a `cornerlays_backup.json` file.
      * Completely custom, built-in file and directory picker dialogs optimized for Android TV D-pads (no external file explorer app needed).
  * **Remote Key Dimmer Reset:**
      * Pressing any key on the physical Google TV remote control immediately cancels/resets active screen dimming back to `0`.
  * **Built for TV:** A clean, D-Pad friendly interface for easy configuration.
  * **Autostart:** Enabled overlays automatically start when the device boots up.

## 🎯 Motivation

Many Android TV set-top boxes or custom builds lack a persistent, always-visible clock or status bar. Cornerlays solves this problem by providing simple, stable, and highly configurable information widgets that integrate seamlessly into your screen.

## 🚀 Installation

1.  Go to the [Releases Page](https://github.com/olus85/Cornerlays/releases) on GitHub.
2.  Download the latest `Cornerlays.apk` file.
3.  Sideload the APK file onto your Android TV or device and install it.

## ⚙️ Usage

1.  Open the **Cornerlays** app from your device's app launcher.
2.  Use the tabs at the top to switch between **Uhr/Datum**, **Wetter**, **HomeAssistant**, and **Einstellungen**.
3.  Enable the widgets you want to see and customize them to your liking.
4.  **For Connection Setup (Home Assistant & MQTT):**
      * Navigate to the **Einstellungen** tab.
      * Enter your Home Assistant URL (e.g., `http://192.168.1.100:8123`), a Long-Lived Access Token, and MQTT credentials.
      * Click **"Verbindung testen"** to check if reachability is successful.
5.  **For Home Assistant Overlays:**
      * Navigate to the **HomeAssistant** tab.
      * Click one of the "HA-Overlay" slots to configure it. Use the entity picker to select a sensor and adjust its display settings (like Display Mode, display text, and alignment nudging).
6.  **To fine-tune widget positions:**
      * Navigate to the "Fine-Tune Position" button and press OK.
      * The widget will be highlighted. Use the D-Pad (Up, Down, Left, Right) to move it.
      * Press OK or Back to save the new position.
7.  **Backup & Restore:**
      * Go to the **Einstellungen** tab and click **"Backup exportieren"** to choose a folder on your device and save the settings locally as `cornerlays_backup.json`.
      * Click **"Backup importieren"** to pick a previously saved backup file and restore all app settings instantly.

## 🤝 Contributing

Contributions are welcome\! If you have an idea for a new feature or have found a bug, please feel free to open an issue or submit a pull request.
