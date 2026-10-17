# Fixed focal-method population — enumeration and stop report

Status: **blocked by conflicting fixed decisions**. No DatasetPrepare run, dataset_meta.json, or dataset_info.json was produced. The candidates below satisfy the non-private rule, but exclude required original focal methods that are private. They are not a generation-ready population.

All counts, signatures and line numbers: from KTester/docker, `docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c 'python /work/docker/part1_codec_population.py'`. Evidence: population-enumeration.json and candidates.csv. Every scoped file in work/t was verified byte-for-byte against `git show cd16988dd46ac7533771eb6856a5f56c7aeaffba:<source-path>` before parsing.

Spoon is loaded through the existing project-info-process.jar and project-index-builder.jar, following the Stage 0 JVM setup. It reads upstream src/main/java at t and uses the authors’ libs only as a read-only parsing classpath. CtClass.getMethods supplies declarations of the named outer class; private, abstract and bodyless methods are excluded. Constructors and nested-class methods are not included. DatasetPrepare would need the authors’ snapshot through a local symlink as instructed; that stage has not been entered.

| Class | Eligible methods |
|---|---:|
| org.apache.commons.codec.binary.Base32 | 5 |
| org.apache.commons.codec.binary.Base64 | 23 |
| org.apache.commons.codec.digest.Md5Crypt | 10 |
| org.apache.commons.codec.digest.MurmurHash3 | 22 |
| org.apache.commons.codec.digest.Sha2Crypt | 6 |

Total eligible methods: 66; within the required interval: True. Existing scoped focal methods: 7 of 18 total dataset focal methods. Eligible existing focal methods: 5. Overload groups: 12, containing 47 methods (same invocation).

## Existing dataset membership

Every fixed class occurs in the authors’ dataset. Per-method declarations at t:

| Original id | Class and method | Visibility at t | Line | Included |
|---|---|---|---:|---|
| Base64_decode | org.apache.commons.codec.binary.Base64.decode(byte[], int, int, Context) | package-private | 745 | True |
| Base32_decode | org.apache.commons.codec.binary.Base32.decode(byte[], int, int, Context) | package-private | 402 | True |
| Base64_encode | org.apache.commons.codec.binary.Base64.encode(byte[], int, int, Context) | package-private | 827 | True |
| Base32_encode | org.apache.commons.codec.binary.Base32.encode(byte[], int, int, Context) | package-private | 507 | True |
| MurmurHash3_hash128x64Internal | org.apache.commons.codec.digest.MurmurHash3.hash128x64Internal(byte[], int, int, long) | private | 515 | False |
| Md5Crypt_md5Crypt | org.apache.commons.codec.digest.Md5Crypt.md5Crypt(byte[], String, String, Random) | public | 275 | True |
| Sha2Crypt_sha2Crypt | org.apache.commons.codec.digest.Sha2Crypt.sha2Crypt(byte[], String, String, int, String) | private | 170 | False |

## Manual cross-check

Read upstream Sha2Crypt.java and counted these eligible public declarations; the parser result is asserted against this explicit list:

- `sha256Crypt(byte[])`
- `sha256Crypt(byte[], String)`
- `sha256Crypt(byte[], String, Random)`
- `sha512Crypt(byte[])`
- `sha512Crypt(byte[], String)`
- `sha512Crypt(byte[], String, Random)`

The private sha2Crypt helper is intentionally excluded by the non-private rule. This independently explains the eligible count rather than silently including it to preserve dataset membership.

## Overload groups (pre-generation)

| Class and method | Overloads |
|---|---:|
| org.apache.commons.codec.binary.Base64.decodeBase64 | 2 |
| org.apache.commons.codec.binary.Base64.encodeBase64 | 4 |
| org.apache.commons.codec.binary.Base64.isBase64 | 3 |
| org.apache.commons.codec.digest.Md5Crypt.apr1Crypt | 5 |
| org.apache.commons.codec.digest.Md5Crypt.md5Crypt | 5 |
| org.apache.commons.codec.digest.MurmurHash3.hash128 | 3 |
| org.apache.commons.codec.digest.MurmurHash3.hash128x64 | 2 |
| org.apache.commons.codec.digest.MurmurHash3.hash32 | 9 |
| org.apache.commons.codec.digest.MurmurHash3.hash32x86 | 2 |
| org.apache.commons.codec.digest.MurmurHash3.hash64 | 6 |
| org.apache.commons.codec.digest.Sha2Crypt.sha256Crypt | 3 |
| org.apache.commons.codec.digest.Sha2Crypt.sha512Crypt | 3 |

The Stage 0 duplicate-ID warning still applies. Generated-ID uniqueness and field-by-field author-entry comparison were not attempted because the fixed population is already inconsistent. No Java source was patched, no private method was added, and no existing focal method was silently dropped.

## Required resolution

Part 1 says decisions must not be reopened and directs a stop if one is impossible. It simultaneously requires every existing scoped focal method to belong to the population and excludes private methods. The private declarations listed above cannot meet both requirements. This report and candidates.csv preserve evidence only.

TODO (Gary): Resolve whether the two original private focal methods should be excluded from the inclusion requirement or whether the non-private rule should change. Once clarified, resume the population driver and then the mandated DatasetPrepare collision/missing-entry checks.
