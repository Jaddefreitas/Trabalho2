import hashlib, uuid, unicodedata, sys
from pathlib import Path

okfile = Path('ok/folha-2005-01-07.txt')
snap_dir = Path('debug-snapshots')
cref = Path('debug-snapshots/creation-log.txt')

if not okfile.exists():
    print('OK file not found:', okfile)
    sys.exit(1)

ok_ids = []
with okfile.open(encoding='utf-8') as f:
    for ln in f:
        ln = ln.strip()
        if 'Empregado ' in ln:
            start = ln.index('Empregado ')+len('Empregado ')
            end = ln.find(' ', start)
            if end==-1:
                end = ln.find('|', start)
            if end==-1:
                end = len(ln)
            ok_ids.append(ln[start:end].strip())

okset = set(ok_ids)
print('Loaded', len(ok_ids), 'ok ids')
in_okset = set(ok_ids)

# pick latest snapshot for 7-1
snaps = sorted(snap_dir.glob('debug-snapshot-7-1-*.txt'))
if not snaps:
    print('No snapshot for 7-1-2005')
    sys.exit(1)
snap = snaps[-1]
print('Using snapshot', snap)

snapshot_names = []
with snap.open(encoding='utf-8') as f:
    lines = [l.strip() for l in f.readlines()]
for ln in lines[1:]:
    if '|' in ln:
        parts = [p.strip() for p in ln.split('|')]
        if len(parts) >= 2:
            emp = parts[0]
            name = parts[1]
            desc = parts[2] if len(parts) > 2 else ''
            snapshot_names.append((emp, name, desc))

print('Snapshot contains', len(snapshot_names), 'entries')

# parse creation log mapping id->name if present
creation = []
if cref.exists():
    for ln in cref.read_text(encoding='utf-8').splitlines():
        if '|' in ln:
            parts = [p.strip() for p in ln.split('|')]
            if len(parts) >= 3:
                creation.append((parts[0], parts[1], parts[2] if len(parts)>2 else ''))
print('Creation log entries:', len(creation))

# Known name->address mapping (from Main.replay-us7 scenario) to try name+address combos
known_addresses = {
    'Fernanda Montenegro': 'end1',
    'Paloma Duarte': 'end2',
    'Lavinia Vlasak': 'end3',
    'Claudia Abreu': 'end4',
    'Claudia Raia': 'end5',
    'Natalia do Valle': 'end6',
    'Regina Duarte': 'end7',
    'Flavia Alessandra': 'end8',
    'Deborah Secco': 'end9',
    'Ana Paula Arosio': 'end10',
    'Suzana Vieira': 'end11',
    'Maite Proenca': 'end12',
}

# Java nameUUIDFromBytes equivalent
def java_name_uuid(b):
    md5 = bytearray(hashlib.md5(b).digest())
    md5[6] = (md5[6] & 0x0f) | 0x30
    md5[8] = (md5[8] & 0x3f) | 0x80
    return str(uuid.UUID(bytes=bytes(md5)))

candidates_tested = 0
matches = []

for idx, (emp, name, desc) in enumerate(snapshot_names, start=1):
    variants = []
    name_norm = ''.join(c for c in unicodedata.normalize('NFD', name) if unicodedata.category(c) != 'Mn')
    variants.append(('raw', name))
    variants.append(('lower', name.lower()))
    variants.append(('trim', name.strip()))
    variants.append(('norm', name_norm))
    variants.append(('norm_lower', name_norm.lower()))
    variants.append(('name|desc', name + '|' + desc))
    variants.append(('name:desc', name + ':' + desc))
    variants.append(('name#idx', f"{name}#{idx}"))
    variants.append(('EMPidx', f"EMP-{idx}"))
    # add name+address if available
    if name in known_addresses:
        variants.append(('name+addr', name + '|' + known_addresses[name]))
        variants.append(('addr+name', known_addresses[name] + '|' + name))

    for tag, v in variants:
        for enc in ('utf-8', 'latin1', 'cp1252'):
            try:
                b = v.encode(enc)
            except Exception:
                continue
            # java name uuid (MD5-based)
            u_java = java_name_uuid(b)
            candidates_tested += 1
            if u_java in in_okset:
                matches.append(('java_name_uuid', tag, enc, v, u_java, emp, name))
            # uuid3/5 with namespaces
            for ns_tag, ns in [('DNS', uuid.NAMESPACE_DNS), ('URL', uuid.NAMESPACE_URL), ('OID', uuid.NAMESPACE_OID), ('X500', uuid.NAMESPACE_X500)]:
                try:
                    u3 = str(uuid.uuid3(ns, v))
                    u5 = str(uuid.uuid5(ns, v))
                except Exception:
                    continue
                candidates_tested += 2
                if u3 in in_okset:
                    matches.append(('uuid3_'+ns_tag, tag, enc, v, u3, emp, name))
                if u5 in in_okset:
                    matches.append(('uuid5_'+ns_tag, tag, enc, v, u5, emp, name))

# also try name+creation timestamp combos
for ts, nm, extra in creation:
    s = nm + ts
    for enc in ('utf-8','latin1'):
        try:
            u = java_name_uuid(s.encode(enc))
            candidates_tested += 1
            if u in in_okset:
                matches.append(('java_name_uuid_nameplus_ts', nm, ts, s, u))
        except Exception:
            continue

print('Candidates tested:', candidates_tested)
if matches:
    print('Matches FOUND:')
    for m in matches:
        print(m)
else:
    print('No matches found with heuristics tried')