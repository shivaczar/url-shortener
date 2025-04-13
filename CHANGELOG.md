# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Semantic Versioning](https://semver.org/).

---

## [1.2.0] - 2025-04-13

### Added
- ✅ Support for bulk URL creation for enterprise users.
- ✅ Ability to set custom short codes while creating links.
- ✅ Password protection for short URLs.
- ✅ Middleware to log request details (timestamp, IP, etc.).
- ✅ API key-based authentication and authorization.
- ✅ Health check endpoint: `/health`.

### Changed
- 🔁 Allowed multiple short codes to point to the same original URL.

---

## [1.1.1] - 2025-04-01

### Fixed
- 🐛 Fixed bug where redirect endpoint failed for certain short codes.
- 🐛 Improved logging format for IP addresses and user-agent.

---

## [1.1.0] - 2025-03-25

### Added
- ✅ Top 10 shortened URLs and top 10 clicked URLs endpoints.
- ✅ `/api/urls/all` and `/api/urls/recent` endpoints for stats.

---

## [1.0.0] - 2025-03-15

### Initial Release
- 🚀 Shorten a long URL and get a short code.
- 🔄 Redirect to original URL using short code.
- ❌ Delete a short URL.
