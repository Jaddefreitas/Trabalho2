import re
from pathlib import Path

# Configure files to compare
root = Path('.')
ok_file = root / 'ok' / 'folha-2005-01-07.txt'
gen_file = root / 'folha-2005-01-07.txt'
snap_dir = root / 'debug-snapshots'

id_re = re.compile(r'Empregado\s+([0-9a-f\-]+)')

def parse_folha(path):
    lines = []
    if not path.exists():
        return lines
    for ln in path.read_text(encoding='utf-8').splitlines():
        m = id_re.search(ln)
        if m:
            emp = m.group(1)
        else:
            emp = None
        lines.append((emp, ln.strip()))
    return lines

ok_lines = parse_folha(ok_file)
gen_lines = parse_folha(gen_file)

print('OK file lines:', len(ok_lines))
print('Generated file lines:', len(gen_lines))

print('\nDifferences by position (first 10):')
for i in range(max(len(ok_lines), len(gen_lines))):
    ok = ok_lines[i][0] if i < len(ok_lines) else None
    gen = gen_lines[i][0] if i < len(gen_lines) else None
    if ok != gen:
        print(f'  Line {i+1}: OK id={ok}  GEN id={gen}')
        print('    OK:', ok_lines[i][1] if i < len(ok_lines) else '')
        print('    GEN:', gen_lines[i][1] if i < len(gen_lines) else '')

# attempt to map generated IDs to names using nearest snapshot (7-1)
snap = None
snaps = sorted(snap_dir.glob('debug-snapshot-7-1-*.txt'))
if snaps:
    snap = snaps[-1]

if snap and snap.exists():
    print('\nUsing snapshot:', snap)
    mapping = {}
    for ln in snap.read_text(encoding='utf-8').splitlines()[1:]:
        parts = ln.split('|')
        if len(parts) >= 2:
            emp = parts[0].strip()
            name = parts[1].strip()
            mapping[emp] = name
    print('\nGenerated folha IDs with names:')
    for emp, line in gen_lines:
        name = mapping.get(emp, '<unknown>')
        print(' ', emp, name)
else:
    print('\nNo snapshot for 7-1 found in debug-snapshots')

# Summary
ok_set = set(e for e, _ in ok_lines if e)
gen_set = set(e for e, _ in gen_lines if e)
only_ok = ok_set - gen_set
only_gen = gen_set - ok_set
print('\nIDs only in OK file:', len(only_ok))
for e in list(only_ok)[:20]:
    print(' ', e)
print('\nIDs only in generated file:', len(only_gen))
for e in list(only_gen)[:20]:
    print(' ', e)
