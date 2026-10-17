# Frozen developer baseline

All counts and timings: from KTester/docker, `docker compose run --rm -v /Users/donggi/_projects/experiment-projects/commons-codec:/repo ktester bash -c 'python /repo/experiments/ktester-commit-survival/tools/baseline.py'`. The shared compose ktester-m2 volume is used by every Maven invocation. Exact Maven argument arrays, JDK/Maven versions and timings are in ../results/developer-baseline.json. No offline mode was used.

The clean upstream export is at `cd16988dd46ac7533771eb6856a5f56c7aeaffba`. Package wall time: 8.073 seconds. Whole suite: 970 (class, method) units, 968 pass all runs (reference only). In scope: 144 methods; **developer denominator: 143** pass all runs.

| Run | Cases reported | Executed (not skipped) | Failures | Errors | Skipped | Wall seconds |
|---:|---:|---:|---:|---:|---:|---:|
| 1 | 1715 | 1713 | 0 | 0 | 2 | 12.666 |
| 2 | 1715 | 1713 | 0 | 0 | 2 | 11.413 |
| 3 | 1715 | 1713 | 0 | 0 | 2 | 12.165 |

## Method normalization and status

Surefire actually writes names such as `testCustomCharset(String)[1]`; the Java identifier before the first parenthesis or bracket is the method. Plain identifiers remain unchanged. No display-only name beginning with [case] was observed; such a name would stop this script rather than be guessed. Every testcase/raw-name mapping is recorded in surefire-method-map.csv. All testcases of a (class, method) pair must pass; any skipped/failing/error case rejects that method for the run. Missing methods reject it too. Parameterized methods count once. The cases column is the per-run case count (or slash-separated run counts if they differ).

The whole-suite unit includes the concrete execution class, so inherited tests executed in different classes are distinct units; source-annotation grep estimates are not the execution denominator. The scoped count is within the specified tolerance.

## Scope classes

The package must equal a scope package and the test name must be ClassTest or ClassSuffixTest. ClassSuffix is excluded when it is another production class at t. Production names are the Java source file names in that package, which include all candidate stream classes. Helpers without a Test suffix do not enter the denominator.

| Class | Methods | Pass all runs |
|---|---:|---:|
| org.apache.commons.codec.binary.Base32Test | 32 | 32 |
| org.apache.commons.codec.binary.Base64Codec13Test | 8 | 8 |
| org.apache.commons.codec.binary.Base64Test | 67 | 66 |
| org.apache.commons.codec.digest.Md5CryptTest | 9 | 9 |
| org.apache.commons.codec.digest.MurmurHash3Test | 27 | 27 |
| org.apache.commons.codec.digest.Sha2CryptTest | 1 | 1 |

Excluded scope-prefix classes (same invocation):

- org.apache.commons.codec.binary.Base64TestData: 0 executed method units; helper/non-Test class.
- org.apache.commons.codec.binary.Base32OutputStreamTest: 7 executed method units; prefix before Test names a production class.
- org.apache.commons.codec.binary.Base64OutputStreamTest: 8 executed method units; prefix before Test names a production class.
- org.apache.commons.codec.binary.Base32InputStreamTest: 17 executed method units; prefix before Test names a production class.
- org.apache.commons.codec.binary.Base64InputStreamTest: 21 executed method units; prefix before Test names a production class.
- org.apache.commons.codec.binary.Base32TestData: 0 executed method units; helper/non-Test class.

Methods not passing every run (same invocation):

- org.apache.commons.codec.binary.Base64Test#testCodec265: run1=skipped, run2=skipped, run3=skipped; in-scope=yes.
- org.apache.commons.codec.net.PercentCodecTest#testBasicSpace: run1=skipped, run2=skipped, run3=skipped; in-scope=no.

Frozen Java sources and test resources are in src-test-t/, with original relative paths beneath java/ and resources/. Their content digests are in frozen-source-sha256.json. Raw Surefire reports remain locally in ignored work/surefire-run*/. The tracked testcase-to-method map preserves the method-level evidence.
