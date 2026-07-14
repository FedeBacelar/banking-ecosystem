# Local password blocklist

`nerva-passwords.txt` is a small, repository-owned list used to make the local
credential policy deterministic and testable. It contains common long
passwords and Nerva-specific guesses that still pass the 15-character minimum.

The file is UTF-8, one password per line, and intentionally lowercase because
Keycloak performs case-insensitive comparisons.

This development list is not a substitute for a maintained compromised-password
corpus in a real deployment. Selecting, licensing, updating and monitoring that
corpus remains a release requirement.
