# CVE Security Fix Plan (20260507080311)

- **Project**: haifa
- **Generated**: 2026-05-07 08:03:11
- **Total CVEs found**: 42 across 12 dependencies
- **Critical Issues**: 5
- **High Severity Issues**: 19
- **Medium Severity Issues**: 18

## CVE Vulnerabilities by Dependency

### 1. `log4j:log4j` — 1.2.17 → ⚠️ CRITICAL: EOL Library

| Severity | CVE | Description |
|----------|-----|-------------|
| CRITICAL | [CVE-2019-17571](https://github.com/advisories/GHSA-2qrg-x229-3v8q) | Deserialization of untrusted data in SocketServer - Remote Code Execution |
| CRITICAL | [CVE-2022-23307](https://github.com/advisories/GHSA-f7vh-qwp3-x37m) | Deserialization issue in Chainsaw component |
| CRITICAL | [CVE-2022-23305](https://github.com/advisories/GHSA-65fg-84f6-3jq3) | SQL Injection in JDBCAppender via crafted log patterns |
| HIGH | [CVE-2021-4104](https://github.com/advisories/GHSA-fp5r-v3w9-4333) | JMSAppender vulnerable to deserialization when configured with attacker-controlled JNDI settings |
| HIGH | [CVE-2022-23302](https://github.com/advisories/GHSA-w9p3-5cr8-m3jj) | JMSSink vulnerable to deserialization with attacker-controlled LDAP service |
| HIGH | [CVE-2023-26464](https://github.com/advisories/GHSA-vp98-w2p3-mv35) | Denial of Service via specially-crafted hashmap/hashtable in Chainsaw/SocketAppender |

**Recommendation**: Migrate from `log4j:log4j:1.2.17` to `org.apache.logging.log4j:log4j-core:2.x.x` (SLF4J compatible)

---

### 2. `org.springframework:spring-webmvc` — 4.3.17.RELEASE → ⚠️ CRITICAL: Spring4Shell

| Severity | CVE | Description |
|----------|-----|-------------|
| CRITICAL | [CVE-2022-22965](https://github.com/advisories/GHSA-36p3-wjmg-h94x) | Spring4Shell - Remote Code Execution via data binding on JDK 9+ |
| HIGH | [CVE-2024-38819](https://github.com/advisories/GHSA-g5vr-rgqm-vf78) | Path Traversal vulnerability when serving static resources |
| MEDIUM | [CVE-2026-22745](https://github.com/advisories/GHSA-6p4f-wcwh-5vvm) | Denial of Service when resolving static resources on Windows |
| LOW | [CVE-2026-22741](https://github.com/advisories/GHSA-wg35-8jpf-2xv3) | Cache poisoning vulnerability with encoded static resources |

**Recommendation**: Upgrade `spring-webmvc` from 4.3.17.RELEASE to 5.3.x or 6.x

---

### 3. `org.springframework.boot:spring-boot-starter-web` — 2.0.5.RELEASE → ⚠️ CRITICAL: Spring4Shell

| Severity | CVE | Description |
|----------|-----|-------------|
| CRITICAL | [CVE-2022-22965](https://github.com/advisories/GHSA-36p3-wjmg-h94x) | Spring4Shell - Remote Code Execution via data binding on JDK 9+ |

**Recommendation**: Upgrade `spring-boot-starter-web` from 2.0.5.RELEASE to 2.6.6+ or 3.x

---

### 4. `org.apache.zookeeper:zookeeper` — 3.4.14 → ⚠️ CRITICAL: Authorization Bypass

| Severity | CVE | Description |
|----------|-----|-------------|
| CRITICAL | [CVE-2023-44981](https://github.com/advisories/GHSA-7286-pgfv-vxvh) | Authorization Bypass Through User-Controlled Key in SASL authentication |

**Recommendation**: Upgrade `zookeeper` from 3.4.14 to 3.9.1 or 3.8.3

---

### 5. `com.fasterxml.jackson.core:jackson-databind` — 2.11.4 → ✅ Fix Available

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2020-36518](https://github.com/advisories/GHSA-57j2-w4cx-62h2) | Stack overflow via deeply nested JSON objects |
| HIGH | [CVE-2022-42004](https://github.com/advisories/GHSA-rgv9-q543-rqg4) | Resource exhaustion via deeply nested arrays with UNWRAP_SINGLE_VALUE_ARRAYS |
| HIGH | [CVE-2022-42003](https://github.com/advisories/GHSA-jjjh-jjxp-wpff) | Resource exhaustion via wrapper array nesting with UNWRAP_SINGLE_VALUE_ARRAYS |
| HIGH | [CVE-2021-46877](https://github.com/advisories/GHSA-3x8x-79m2-3w2w) | Denial of Service via JDK serialization of JsonNode (2 GB heap usage) |

**Recommendation**: Upgrade `jackson-databind` from 2.11.4 to 2.13.4.2 or later

---

### 6. `ch.qos.logback:logback-classic` — 1.2.3 → ✅ Fix Available

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2023-6378](https://github.com/advisories/GHSA-vmq6-5m68-f53m) | Serialization vulnerability in logback receiver component (DoS) |

**Recommendation**: Upgrade `logback-classic` from 1.2.3 to 1.2.13 or 1.3.x

---

### 7. `commons-io:commons-io` — 2.5 → ✅ Fix Available

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2024-47554](https://github.com/advisories/GHSA-78wr-2p64-hpwj) | Denial of Service via XmlStreamReader resource exhaustion |
| MEDIUM | [CVE-2021-29425](https://github.com/advisories/GHSA-gwrp-pvrq-jmwv) | Path Traversal vulnerability in FileNameUtils.normalize |

**Recommendation**: Upgrade `commons-io` from 2.5 to 2.14.0 or later

---

### 8. `org.bouncycastle:bcprov-jdk15on` — 1.53 → ✅ Fix Available

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2018-1000180](https://github.com/advisories/GHSA-xqj7-j8j5-f2xr) | Flaw in RSA key pair generator - reduced certainty in M-R tests |
| HIGH | [CVE-2016-1000338](https://github.com/advisories/GHSA-4vhj-98r6-424h) | DSA signature validation bypass - extra sequence elements allowed |
| HIGH | [CVE-2016-1000340](https://github.com/advisories/GHSA-r97x-3g8f-gx3m) | Carry propagation bug in elliptic curve operations |
| HIGH | [CVE-2016-1000342](https://github.com/advisories/GHSA-qcj7-g2j5-g7r3) | ECDSA signature validation bypass |
| HIGH | [CVE-2016-1000343](https://github.com/advisories/GHSA-rrvx-pwf8-p59p) | DSA weak private key generation with default parameters |
| HIGH | [CVE-2016-1000352](https://github.com/advisories/GHSA-w285-wf9q-5w69) | ECIES allowed ECB mode (unsafe) |
| HIGH | [CVE-2016-1000344](https://github.com/advisories/GHSA-2j2x-hx4g-2gf4) | DHIES allowed ECB mode (unsafe) |
| MEDIUM | [CVE-2016-1000339](https://github.com/advisories/GHSA-c8xf-m4ff-jcxj) | AES side-channel timing attack via table lookups |
| MEDIUM | [CVE-2016-1000341](https://github.com/advisories/GHSA-r9ch-m4fh-fc7q) | DSA timing attack vulnerability |
| MEDIUM | [CVE-2016-1000345](https://github.com/advisories/GHSA-9gp4-qrff-c648) | DHIES/ECIES CBC padding oracle attack |
| MEDIUM | [CVE-2020-26939](https://github.com/advisories/GHSA-72m5-fvvv-55m6) | OAEP private exponent information disclosure |
| MEDIUM | [CVE-2020-15522](https://github.com/advisories/GHSA-6xx3-rg99-gc3p) | ECDSA timing attack - private key exposure |
| MEDIUM | [CVE-2023-33201](https://github.com/advisories/GHSA-hr8g-6v94-x4m9) | LDAP injection in X509LDAPCertStoreSpi |
| MEDIUM | [CVE-2023-33202](https://github.com/advisories/GHSA-wjxj-5m7g-mg7q) | Denial of Service via PEMParser OutOfMemoryError |
| MEDIUM | [CVE-2024-29857](https://github.com/advisories/GHSA-8xfc-gm6g-vgpv) | High CPU usage in EC parameter evaluation |
| MEDIUM | [CVE-2024-30171](https://github.com/advisories/GHSA-v435-xc8x-wvr9) | Timing side-channel in RSA key exchange (Marvin Attack) |
| LOW | [CVE-2016-1000346](https://github.com/advisories/GHSA-fjqm-246c-mwqg) | DH public key not fully validated |

**Recommendation**: Upgrade `bcprov-jdk15on` from 1.53 to 1.78 or later

---

### 9. `org.assertj:assertj-core` — 3.16.1 → ✅ Fix Available

| Severity | CVE | Description |
|----------|-----|-------------|
| HIGH | [CVE-2026-24400](https://github.com/advisories/GHSA-rqfh-9r24-8c9r) | XML External Entity (XXE) injection in isXmlEqualTo assertion |

**Recommendation**: Upgrade `assertj-core` from 3.16.1 to 3.27.7 or use XMLUnit instead

---

## Summary by Severity

| Severity | Count | Status |
|----------|-------|--------|
| CRITICAL | 5 | ⚠️ All require patching |
| HIGH | 19 | ✅ Patches available |
| MEDIUM | 18 | ✅ Patches available |
| **Total** | **42** | |

## Risk Assessment

**Critical Risk Areas**:
1. **Log4j 1.2.17** - End-of-life library with 3 CRITICAL CVEs. Immediate replacement required.
2. **Spring Framework/Boot** - Spring4Shell (CVE-2022-22965) allows RCE on JDK 9+ with Tomcat WAR deployment
3. **ZooKeeper 3.4.14** - Authorization bypass in SASL Quorum Peer authentication

**Recommended Priority**:
1. **Phase 1 (CRITICAL)**: Replace log4j 1.2.17 with log4j 2.x, upgrade Spring Framework/Boot, upgrade ZooKeeper
2. **Phase 2 (HIGH)**: Upgrade Jackson, Logback, Commons IO, Bouncy Castle
3. **Phase 3 (MEDIUM)**: Address remaining medium-severity CVEs in Bouncy Castle

## Options

- **Minimum CVE severity to fix**: CRITICAL only | **HIGH and above** | MEDIUM and above | ALL
- **Fix strategy**:
  - [ ] Direct dependency upgrades in parent pom.xml
  - [ ] Module-specific version overrides for compatibility
  - [ ] Test with affected modules (haifa-mq, haifa-cache, haifa-java-ee, etc.)
- **Working branch**: `appmod/security-fix-20260507080311`

---

## Next Steps

1. Review this plan and select severity level
2. Approve execution of fixes
3. Run build validation after each phase
4. Re-scan for CVE resolution verification
