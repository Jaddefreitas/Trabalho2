import hashlib, uuid, unicodedata

ok_ids = set([
"8adb43b3-ceba-43a5-ba8f-dfc20c8c9de3",
"843aacc5-13f6-42c2-8992-4baf0935e38d",
"8806b971-462c-457d-b702-afb3b3eb4f4e",
"0ac48206-5526-4584-8ef7-103a28794e4d",
"cec9785c-c28e-4c47-bdc7-357215439c9a",
"d63eac72-4dc2-4142-8b54-01c3c4ee0b57",
"0434fa90-4946-4db5-97a7-7d552be16564",
"bb8afe9c-c5e1-4887-8ba1-7fe85ecbb3fe",
"a8892c14-a89a-454e-b346-abbeb376af33",
])

snapshot='debug-snapshots/debug-snapshot-7-1-2005-1760631016397.txt'
lines=[]
with open(snapshot, encoding='utf-8') as f:
    for l in f:
        if '|' in l:
            parts=[p.strip() for p in l.split('|')]
            if len(parts)>=3:
                lines.append((parts[0], parts[1], parts[2]))

methods = []
# Java nameUUIDFromBytes(raw bytes) equivalent
def java_name_uuid(s_bytes):
    md5 = bytearray(hashlib.md5(s_bytes).digest())
    md5[6] = (md5[6] & 0x0f) | 0x30
    md5[8] = (md5[8] & 0x3f) | 0x80
    return uuid.UUID(bytes=bytes(md5))

for id,name,desc in lines:
    variants = {}
    variants['name'] = name
    variants['name_lower'] = name.lower()
    variants['name_trim'] = name.strip()
    name_norm = ''.join(c for c in unicodedata.normalize('NFD', name) if unicodedata.category(c) != 'Mn')
    variants['name_norm'] = name_norm
    variants['name_norm_lower'] = name_norm.lower()
    variants['name_addr'] = name + '|' + desc
    for k,v in variants.items():
        u1 = str(java_name_uuid(v.encode('utf-8')))
        if u1 in ok_ids:
            print('MATCH java_name_uuid', k, name, '->', u1)
        u2 = str(uuid.uuid3(uuid.NAMESPACE_DNS, v))
        if u2 in ok_ids:
            print('MATCH uuid3_dns', k, name, '->', u2)
        u3 = str(uuid.uuid5(uuid.NAMESPACE_DNS, v))
        if u3 in ok_ids:
            print('MATCH uuid5_dns', k, name, '->', u3)
        u4 = str(uuid.uuid3(uuid.NAMESPACE_URL, v))
        if u4 in ok_ids:
            print('MATCH uuid3_url', k, name, '->', u4)
        u5 = str(uuid.uuid5(uuid.NAMESPACE_URL, v))
        if u5 in ok_ids:
            print('MATCH uuid5_url', k, name, '->', u5)

print('done')
