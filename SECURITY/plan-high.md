# CVE Security Fix Plan (20260508045515)

- **Project**: haifa
- **Generated**: 2026-05-08 04:55:15
- **Total CVEs found**: 22 across 3 dependencies
- **High Severity Issues**: 6
- **Medium Severity Issues**: 16

## CVE Vulnerabilities

### 1. `ch.qos.logback:logback-classic` — 1.2.3 → 1.5.6 ✅ Upgrade

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2023-6378](https://github.com/advisories/GHSA-vmq6-5m68-f53m) | logback serialization vulnerability |

### 2. `com.fasterxml.jackson.core:jackson-databind` — 2.11.4 → 2.15.4 ✅ Upgrade

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2020-36518](https://github.com/advisories/GHSA-57j2-w4cx-62h2) | Deeply nested json in jackson-databind |
| HIGH | [CVE-2022-42004](https://github.com/advisories/GHSA-rgv9-q543-rqg4) | Uncontrolled Resource Consumption in FasterXML jackson-databind |
| HIGH | [CVE-2022-42003](https://github.com/advisories/GHSA-jjjh-jjxp-wpff) | Uncontrolled Resource Consumption in Jackson-databind |
| HIGH | [CVE-2021-46877](https://github.com/advisories/GHSA-3x8x-79m2-3w2w) | jackson-databind possible Denial of Service if using JDK serialization to serialize JsonNode |

### 3. `org.bouncycastle:bcprov-jdk15on` — 1.53 → 1.78 ✅ Upgrade

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2018-1000180](https://github.com/advisories/GHSA-xqj7-j8j5-f2xr) | Bouncy Castle has a flaw in the Low-level interface to RSA key pair generator |
| HIGH | [CVE-2016-1000338](https://github.com/advisories/GHSA-4vhj-98r6-424h) | In Bouncy Castle JCE Provider it is possible to inject extra elements in the sequence making up the signature and still have it validate |
| HIGH | [CVE-2016-1000340](https://github.com/advisories/GHSA-r97x-3g8f-gx3m) | The Bouncy Castle JCE Provider carry a propagation bug |
| HIGH | [CVE-2016-1000342](https://github.com/advisories/GHSA-qcj7-g2j5-g7r3) | In Bouncy Castle JCE Provider ECDSA does not fully validate ASN.1 encoding of signature on verification |
| HIGH | [CVE-2016-1000343](https://github.com/advisories/GHSA-rrvx-pwf8-p59p) | In Bouncy Castle JCE Provider the DSA key pair generator generates a weak private key if used with default values |
| HIGH | [CVE-2016-1000352](https://github.com/advisories/GHSA-w285-wf9q-5w69) | In Bouncy Castle JCE Provider the ECIES implementation allowed the use of ECB mode |
| HIGH | [CVE-2016-1000344](https://github.com/advisories/GHSA-2j2x-hx4g-2gf4) | In Bouncy Castle JCE Provider the DHIES implementation allowed the use of ECB mode |

## Options

- Minimum CVE severity to fix: HIGH and above
- Working branch: `appmod/security-fix-20260508045515`