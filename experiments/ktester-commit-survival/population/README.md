# Population: 52 non-deprecated methods

Rule: non-private, non-deprecated methods with a body, excluding constructors, in the five fixed classes. The authors’ index is unchanged. Developer denominator: 143; commit range: 539.

The previous 66-method artifacts are preserved in population-66/. Historical enumeration files describe that previous population. Current evidence is population-validation.json, deprecation-audit.csv and deprecated-exclusions.csv.

| Class | Methods |
|---|---:|
| org.apache.commons.codec.binary.Base32 | 5 |
| org.apache.commons.codec.binary.Base64 | 22 |
| org.apache.commons.codec.digest.Md5Crypt | 10 |
| org.apache.commons.codec.digest.MurmurHash3 | 9 |
| org.apache.commons.codec.digest.Sha2Crypt | 6 |

All 14 excluded methods carry @Deprecated in both the snapshot and upstream cd16988. Parsed method declarations match after comment removal. Whole Base32/Base64 files have comment differences, so source equivalence is verified at the method level. None of the retained 52 methods is deprecated; all match the existing JSON index. Annotation and index checks cover the full prior 66-method population.

Excluded signatures:

- `org.apache.commons.codec.binary.Base64.isArrayByteBase64(byte[])`
- `org.apache.commons.codec.digest.MurmurHash3.hash128(byte[], int, int, int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash128(String)`
- `org.apache.commons.codec.digest.MurmurHash3.hash32(byte[])`
- `org.apache.commons.codec.digest.MurmurHash3.hash32(byte[], int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash32(byte[], int, int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash32(byte[], int, int, int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash32(String)`
- `org.apache.commons.codec.digest.MurmurHash3.hash64(byte[])`
- `org.apache.commons.codec.digest.MurmurHash3.hash64(byte[], int, int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash64(byte[], int, int, int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash64(int)`
- `org.apache.commons.codec.digest.MurmurHash3.hash64(long)`
- `org.apache.commons.codec.digest.MurmurHash3.hash64(short)`

## Extraction and renumbering

Run `python /work/docker/part2_reduce_population.py` in the prescribed container. The unmodified DatasetPrepare jars read the authors’ snapshot through the existing symlink. The five original included focal entries match every author field up to line endings.

Preserve every ID that is unique in the raw output. Within each (class, method name) group, sort only entries whose raw IDs collide by parameter count, then parameter-list string (case-sensitive lexicographic order). Assign the lowest available base ID, _2, _3, and so on, skipping all reserved unique IDs.

Measured: 52 entries and unique IDs; 39 raw unique IDs; 13 changed IDs; 39 unchanged IDs. All previously unique entries and every non-naming field are preserved. Renumbering uses tools/renumber_dataset_ids.py; only its validation count is parameterized. All code fields are nonempty and naming paths are consistent.

Settings, source, jars, author dataset and index hashes are unchanged across extraction. No model calls were made.
