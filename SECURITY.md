# Security Policy

## Supported versions

Only the latest release of Whispry receives security fixes. Older
releases are not patched — please update before reporting an issue if
you're not on the latest version.

| Version        | Supported          |
| -------------- | ------------------- |
| Latest release | :white_check_mark: |
| Older releases | :x:                 |

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security
vulnerabilities.

Use one of these instead:

- **GitHub Private Vulnerability Reporting** (preferred): go to the
  [Security tab](https://github.com/cosmictaserdev-creator/whispry/security/advisories/new)
  of this repo and open a new draft security advisory.
- **Email**: cosmictaser.dev@gmail.com — include a description of the
  issue, steps to reproduce, and the app version affected.

## What to expect

- Acknowledgement of your report: best effort, typically within a few
  days.
- This is a community-maintained project (not a company with a dedicated
  security team), so response and fix timelines are best-effort, not
  contractual. Critical issues (e.g. anything touching recorded audio,
  transcripts, or stored API keys) are prioritized.
- You'll be credited in the release notes when the fix ships, unless you
  ask not to be.

## Scope

Whispry runs entirely on-device except for calls to whichever
transcription/formatting AI provider you configure in Settings (your own
API key, sent directly from your device to that provider). Reports about
those third-party providers' own infrastructure should go to the
provider, not here — but if Whispry itself mishandles keys, audio, or
transcripts (e.g. insecure storage, leaking data to the wrong place),
that's in scope.
