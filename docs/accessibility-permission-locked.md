# Accessibility permission turns itself off / won't turn on

Whispry needs its Accessibility Service on to detect your keyboard and paste transcripts into
whatever app you're typing in (see [Privacy](../README.md#-privacy) for exactly what it reads).
Two separate things commonly block this, and neither is a Whispry bug.

## 1. "Restricted setting" (Android 13+, every OEM)

Since Android 13, apps installed from outside an app store (GitHub Releases, a downloaded APK) have
their sensitive permissions, including Accessibility, blocked by default. If the Accessibility
toggle for Whispry is greyed out or tapping it does nothing:

1. Open **Settings → Apps → Whispry**.
2. Tap the **⋮ (three-dot) menu** in the top right.
3. Tap **Allow restricted settings**.
4. Now go back to **Settings → Accessibility → Whispry** and turn it on.

This has to be repeated if you ever reinstall the app (uninstall + fresh install counts; an in-app
update does not).

## 2. OEM battery/background management killing the service

Several manufacturers run their own background-app killer on top of Android's, and it can silently
disable accessibility services (usually after a reboot, or after the phone's been locked a while)
to save battery. If Whispry's toggle keeps turning itself back off, check your phone's brand below.

### Xiaomi / Redmi / POCO (MIUI, HyperOS)

- **Settings → Apps → Manage apps → Whispry → Autostart** — enable it.
- **Settings → Battery & performance → App battery saver → Whispry** — set to **No restrictions**.
- Open Whispry in Recents, tap its icon at the top, and **lock it** (padlock icon) so MIUI doesn't
  sweep it from memory.
- Security app → Privacy → Special permissions → sometimes shows a second, separate Accessibility
  toggle list. Check there too if the system Settings one won't stick.

### Samsung (One UI)

- **Settings → Apps → Whispry → Battery → Unrestricted**.
- **Settings → Battery and device care → Battery → Background usage limits** — make sure Whispry
  isn't in the **Sleeping apps** or **Deep sleeping apps** list; remove it if it is.
- **Settings → Apps → ⋮ → Special access → (or Device care) → Auto-disable unused apps** — turn
  this off, or Samsung will silently revoke permissions (including Accessibility) from apps it
  decides you haven't used recently.

### Oppo / Realme / OnePlus (ColorOS / OxygenOS)

- **Settings → Battery → App Battery Management → Whispry** — allow background activity, disable
  any "sleep standby optimization" for the app.
- **Settings → App management → App list → Whispry → Allow auto launch** (autostart).
- Lock Whispry in the Recents/multitasking view (swipe down on its card or tap the lock icon,
  varies by version).

### Vivo / iQOO (Funtouch OS, OriginOS)

- **i Manager → App manager → Autostart manager → Whispry** — enable.
- **Settings → Battery → Background power consumption management → Whispry** — allow high
  background power usage.

### Huawei / Honor (EMUI, MagicOS)

- **Settings → Battery → App launch → Whispry** — switch to **Manage manually** and enable
  **Auto-launch**, **Secondary launch**, and **Run in background**.

## Still not working?

Open Whispry's **About → Share Crash Log** (if a crash happened) or open a
[GitHub issue](https://github.com/cosmictaserdev-creator/whispry/issues) with your phone's exact
brand, model, and Android/OEM software version. "Accessibility permission locked" reports without
the OEM version are hard to act on since the fix above is different per manufacturer and even per
OS version.
