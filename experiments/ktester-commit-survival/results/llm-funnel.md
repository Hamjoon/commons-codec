# LLM tests at t

Developer denominator: **143**.

| Round | Files | Compiled | Methods listed | Pass all three | New unique | Cumulative unique |
|---:|---:|---:|---:|---:|---:|---:|
| 1 | 52 | 24 | 151 | 126 | 126 | 126 |

LLM denominator so far: **126**, versus **143** developer methods. Duplicates removed: **0**. Shortfall: **17**.

Each source is compiled independently against production classes at t and the 18 fixed test jars. Each compiled class is launched three times in separate JVMs with a 600-second wall-clock limit per run. Compile failures are recorded with the first javac error and contribute no listed methods. Files and commands are in llm-roundN-validation.json; per-case and per-method outcomes are in the corresponding CSVs. Raw logs/XML remain under ignored work/llm-roundN/.

Parameterized/repeated invocations count as one method; every invocation must pass. Any class timeout marks all methods timeout for that run. Assertion failures are fail; other exceptions are error; disabled or unreported source test methods are skipped (or error when the launcher fails). Source-annotated tests and XML-discovered tests are included in the listed count.

De-duplication compares exact Java method bodies after removing comments and formatting whitespace, retaining identifiers and literal contents. Order is round, file name, source line, class and method. Later identical bodies are excluded even across rounds; llm-duplicates.csv links each excluded method to the retained one. Source files are never rewritten.
