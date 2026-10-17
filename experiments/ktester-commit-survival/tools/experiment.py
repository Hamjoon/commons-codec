"""Shared, fixed experiment decisions; no model or pipeline settings are loaded."""
from pathlib import Path
BASE = 'cd16988dd46ac7533771eb6856a5f56c7aeaffba'
EXPERIMENT = Path(__file__).resolve().parents[1]
REPO = EXPERIMENT.parents[1]
SCOPE = [
 'org.apache.commons.codec.binary.Base32',
 'org.apache.commons.codec.binary.Base64',
 'org.apache.commons.codec.digest.Md5Crypt',
 'org.apache.commons.codec.digest.MurmurHash3',
 'org.apache.commons.codec.digest.Sha2Crypt',
]
SCOPE_FILES = ['src/main/java/'+x.replace('.', '/')+'.java' for x in SCOPE]


def preserve_log(log):
    """Keep exact output locally and trim trailing whitespace in the public log."""
    import shutil
    raw = EXPERIMENT / 'work/raw-logs'
    raw.mkdir(parents=True, exist_ok=True)
    shutil.copy2(log, raw / log.name)
    log.write_text('\n'.join(line.rstrip() for line in log.read_text().splitlines()).rstrip() + '\n')
