# Focal-method population: 66 non-private methods

The second addendum fixes membership at the existing candidates.csv. The two private original focal methods are explicitly excluded. No visibility changes or wrappers were used.

Status: **validated after addendum 3 renumbering**. dataset_info.json contains 66 unique IDs; dataset_info.raw.json preserves the original extraction. Historical raw-output checks are retained below.

Counts and checks below come from population-validation.json, produced from KTester/docker with:

```sh
docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c 'python /work/docker/part1_codec_dataset_prepare.py'
```

Source: the authors’ data/projects/commons-codec snapshot through the local commons-codec symlink. The bundled jars and Stage 0 JVM settings are unchanged. Metadata lists the five included original focal methods first (authors’ order), then the remaining candidates in CSV order, preserving original base IDs for the field comparison. That describes raw extraction order; addendum 3 naming changes are recorded below.

The earlier population-enumeration.json, enumeration-run.txt and docker/part1_codec_population.py preserve the first enumeration and its then-required stop. Their private-method inclusion conflict is superseded by addendum 2; do not rerun the historical driver to resume generation.

| Class | Methods |
|---|---:|
| org.apache.commons.codec.binary.Base32 | 5 |
| org.apache.commons.codec.binary.Base64 | 23 |
| org.apache.commons.codec.digest.Md5Crypt | 10 |
| org.apache.commons.codec.digest.MurmurHash3 | 22 |
| org.apache.commons.codec.digest.Sha2Crypt | 6 |

Total: 66; within 60–100: True.

## Dataset focal methods in scope

| Original ID | Class and method | Visibility at t | Included |
|---|---|---|---|
| Base64_decode | org.apache.commons.codec.binary.Base64.decode(byte[], int, int, Context) | package-private | yes |
| Base32_decode | org.apache.commons.codec.binary.Base32.decode(byte[], int, int, Context) | package-private | yes |
| Base64_encode | org.apache.commons.codec.binary.Base64.encode(byte[], int, int, Context) | package-private | yes |
| Base32_encode | org.apache.commons.codec.binary.Base32.encode(byte[], int, int, Context) | package-private | yes |
| MurmurHash3_hash128x64Internal | org.apache.commons.codec.digest.MurmurHash3.hash128x64Internal(byte[], int, int, long) | private | no — private |
| Md5Crypt_md5Crypt | org.apache.commons.codec.digest.Md5Crypt.md5Crypt(byte[], String, String, Random) | public | yes |
| Sha2Crypt_sha2Crypt | org.apache.commons.codec.digest.Sha2Crypt.sha2Crypt(byte[], String, String, int, String) | private | no — private |

## Overload groups

12 groups contain 47 methods. Groups use the same class and name with different parameter lists.

- `org.apache.commons.codec.binary.Base64.decodeBase64`: `decodeBase64(byte[])`; `decodeBase64(String)`
- `org.apache.commons.codec.binary.Base64.encodeBase64`: `encodeBase64(byte[])`; `encodeBase64(byte[], boolean)`; `encodeBase64(byte[], boolean, boolean)`; `encodeBase64(byte[], boolean, boolean, int)`
- `org.apache.commons.codec.binary.Base64.isBase64`: `isBase64(byte)`; `isBase64(byte[])`; `isBase64(String)`
- `org.apache.commons.codec.digest.Md5Crypt.apr1Crypt`: `apr1Crypt(byte[])`; `apr1Crypt(byte[], Random)`; `apr1Crypt(byte[], String)`; `apr1Crypt(String)`; `apr1Crypt(String, String)`
- `org.apache.commons.codec.digest.Md5Crypt.md5Crypt`: `md5Crypt(byte[])`; `md5Crypt(byte[], Random)`; `md5Crypt(byte[], String)`; `md5Crypt(byte[], String, String)`; `md5Crypt(byte[], String, String, Random)`
- `org.apache.commons.codec.digest.MurmurHash3.hash128`: `hash128(byte[])`; `hash128(byte[], int, int, int)`; `hash128(String)`
- `org.apache.commons.codec.digest.MurmurHash3.hash128x64`: `hash128x64(byte[])`; `hash128x64(byte[], int, int, int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash32`: `hash32(byte[])`; `hash32(byte[], int)`; `hash32(byte[], int, int)`; `hash32(byte[], int, int, int)`; `hash32(long)`; `hash32(long, int)`; `hash32(long, long)`; `hash32(long, long, int)`; `hash32(String)`
- `org.apache.commons.codec.digest.MurmurHash3.hash32x86`: `hash32x86(byte[])`; `hash32x86(byte[], int, int, int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash64`: `hash64(byte[])`; `hash64(byte[], int, int)`; `hash64(byte[], int, int, int)`; `hash64(int)`; `hash64(long)`; `hash64(short)`
- `org.apache.commons.codec.digest.Sha2Crypt.sha256Crypt`: `sha256Crypt(byte[])`; `sha256Crypt(byte[], String)`; `sha256Crypt(byte[], String, Random)`
- `org.apache.commons.codec.digest.Sha2Crypt.sha512Crypt`: `sha512Crypt(byte[])`; `sha512Crypt(byte[], String)`; `sha512Crypt(byte[], String, Random)`

## Raw DatasetPrepare validation (historical)

Generated entries: 66. Distinct IDs: 43. Colliding IDs: 9; affected methods: 32. Missing methods: 0; unexpected methods: 0; empty fields: 0.

| Colliding ID | Affected methods |
|---|---|
| Base64_encodeBase64_2 | org.apache.commons.codec.binary.Base64.encodeBase64(byte[], boolean)<br>org.apache.commons.codec.binary.Base64.encodeBase64(byte[], boolean, boolean)<br>org.apache.commons.codec.binary.Base64.encodeBase64(byte[], boolean, boolean, int) |
| Base64_isBase64_2 | org.apache.commons.codec.binary.Base64.isBase64(byte[])<br>org.apache.commons.codec.binary.Base64.isBase64(String) |
| Md5Crypt_apr1Crypt_2 | org.apache.commons.codec.digest.Md5Crypt.apr1Crypt(byte[], Random)<br>org.apache.commons.codec.digest.Md5Crypt.apr1Crypt(byte[], String)<br>org.apache.commons.codec.digest.Md5Crypt.apr1Crypt(String)<br>org.apache.commons.codec.digest.Md5Crypt.apr1Crypt(String, String) |
| Md5Crypt_md5Crypt_2 | org.apache.commons.codec.digest.Md5Crypt.md5Crypt(byte[])<br>org.apache.commons.codec.digest.Md5Crypt.md5Crypt(byte[], Random)<br>org.apache.commons.codec.digest.Md5Crypt.md5Crypt(byte[], String)<br>org.apache.commons.codec.digest.Md5Crypt.md5Crypt(byte[], String, String) |
| MurmurHash3_hash128_2 | org.apache.commons.codec.digest.MurmurHash3.hash128(byte[], int, int, int)<br>org.apache.commons.codec.digest.MurmurHash3.hash128(String) |
| MurmurHash3_hash32_2 | org.apache.commons.codec.digest.MurmurHash3.hash32(byte[], int)<br>org.apache.commons.codec.digest.MurmurHash3.hash32(byte[], int, int)<br>org.apache.commons.codec.digest.MurmurHash3.hash32(byte[], int, int, int)<br>org.apache.commons.codec.digest.MurmurHash3.hash32(long)<br>org.apache.commons.codec.digest.MurmurHash3.hash32(long, int)<br>org.apache.commons.codec.digest.MurmurHash3.hash32(long, long)<br>org.apache.commons.codec.digest.MurmurHash3.hash32(long, long, int)<br>org.apache.commons.codec.digest.MurmurHash3.hash32(String) |
| MurmurHash3_hash64_2 | org.apache.commons.codec.digest.MurmurHash3.hash64(byte[], int, int)<br>org.apache.commons.codec.digest.MurmurHash3.hash64(byte[], int, int, int)<br>org.apache.commons.codec.digest.MurmurHash3.hash64(int)<br>org.apache.commons.codec.digest.MurmurHash3.hash64(long)<br>org.apache.commons.codec.digest.MurmurHash3.hash64(short) |
| Sha2Crypt_sha256Crypt_2 | org.apache.commons.codec.digest.Sha2Crypt.sha256Crypt(byte[], String)<br>org.apache.commons.codec.digest.Sha2Crypt.sha256Crypt(byte[], String, Random) |
| Sha2Crypt_sha512Crypt_2 | org.apache.commons.codec.digest.Sha2Crypt.sha512Crypt(byte[], String)<br>org.apache.commons.codec.digest.Sha2Crypt.sha512Crypt(byte[], String, Random) |

## Raw original-entry field comparison

Every field is compared, permitting CRLF/LF differences only. Full per-field evidence is in population-validation.json.

| Original ID | Equal up to line endings | Differing fields |
|---|---|---|
| Base64_decode | True |  |
| Base32_decode | True |  |
| Base64_encode | True |  |
| Base32_encode | True |  |
| Md5Crypt_md5Crypt | True |  |

The protected-input SHA-256 manifest matches before and after extraction. This includes the authors’ dataset, snapshot sources/libs, packaged extractor jars, DatasetPrepare.java and settings.py. No model call or index rebuild occurred.

dataset_meta.json, the preserved dataset_info.raw.json and the renumbered dataset_info.json are tracked in both repositories. The local source symlink is ignored, and no jars are copied.

## Renumbering

Within each (class, method name) group, sort by parameter count, then parameter-list string (case-sensitive lexicographic order); assign base ID, _2, _3, and so on.

Invocation (from commons-codec): `python3 experiments/ktester-commit-survival/tools/renumber_dataset_ids.py --kt /Users/donggi/_projects/experiment-projects/KTester`. Evidence: id-renumbering-validation.json; id-renumbering.csv lists all entries, including unchanged IDs.

Validation: 66 entries, 66 distinct IDs; 34 IDs changed and 32 remained unchanged. Every test-class and test-path follows the author naming convention. No other field changed, including focal-method and class-code; entry order is preserved. Applying the transformation again is idempotent.

The explicit sort also changes 7 previously unique IDs. The addendum’s sentence claiming only duplicates change does not hold for this ordering; the explicit sort rule governs. Affected previously unique IDs:

| Method | Old ID | New ID |
|---|---|---|
| org.apache.commons.codec.digest.Md5Crypt.md5Crypt(byte[], String, String, Random) | Md5Crypt_md5Crypt | Md5Crypt_md5Crypt_5 |
| org.apache.commons.codec.binary.Base64.decodeBase64(byte[]) | Base64_decodeBase64 | Base64_decodeBase64_2 |
| org.apache.commons.codec.binary.Base64.decodeBase64(String) | Base64_decodeBase64_2 | Base64_decodeBase64 |
| org.apache.commons.codec.binary.Base64.isBase64(byte) | Base64_isBase64 | Base64_isBase64_2 |
| org.apache.commons.codec.digest.Md5Crypt.apr1Crypt(byte[]) | Md5Crypt_apr1Crypt | Md5Crypt_apr1Crypt_2 |
| org.apache.commons.codec.digest.MurmurHash3.hash128(byte[]) | MurmurHash3_hash128 | MurmurHash3_hash128_2 |
| org.apache.commons.codec.digest.MurmurHash3.hash32(byte[]) | MurmurHash3_hash32 | MurmurHash3_hash32_2 |

The original Md5Crypt.md5Crypt(byte[], String, String, Random) entry is now Md5Crypt_md5Crypt_5. Its id, test-class and test-path change; all other original-entry fields still match the authors up to line endings. The four Base32/Base64 original entries keep their naming fields. No Java source, packaged jar, authors’ dataset, prompts or settings were changed.
