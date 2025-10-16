import hashlib
import uuid

def java_name_uuid(name_bytes):
    md5 = bytearray(hashlib.md5(name_bytes).digest())
    # set version to 3
    md5[6] = (md5[6] & 0x0f) | 0x30
    # set variant to IETF
    md5[8] = (md5[8] & 0x3f) | 0x80
    return uuid.UUID(bytes=bytes(md5))

snapshot = 'debug-snapshots/debug-snapshot-7-1-2005-1760631016397.txt'
try:
    with open(snapshot, 'r', encoding='utf-8') as f:
        lines = [l.strip() for l in f.readlines() if '|' in l]
except FileNotFoundError:
    print('Snapshot not found:', snapshot)
    raise SystemExit(1)

print('Computed Java name-based UUIDs from snapshot names:')
for line in lines:
    parts = [p.strip() for p in line.split('|')]
    if len(parts) >= 3:
        id_, name, desc = parts[0], parts[1], parts[2]
        u = java_name_uuid(name.encode('utf-8'))
        print(name, '->', str(u), ' (snapshot id=', id_, ')')
