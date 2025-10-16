import uuid

def nameuuid_bytes(s):
    return uuid.UUID(bytes=__import__('hashlib').md5(s.encode('utf-8')).digest())

ok_ids = [
"8adb43b3-ceba-43a5-ba8f-dfc20c8c9de3",
"843aacc5-13f6-42c2-8992-4baf0935e38d",
"8806b971-462c-457d-b702-afb3b3eb4f4e",
"0ac48206-5526-4584-8ef7-103a28794e4d",
"cec9785c-c28e-4c47-bdc7-357215439c9a",
"d63eac72-4dc2-4142-8b54-01c3c4ee0b57",
"0434fa90-4946-4db5-97a7-7d552be16564",
"bb8afe9c-c5e1-4887-8ba1-7fe85ecbb3fe",
"a8892c14-a89a-454e-b346-abbeb376af33",
]

for i in range(1,101):
    s = "EMP#%d"%i
    u = nameuuid_bytes(s)
    if str(u) in ok_ids:
        print('Found match:', s, str(u))

# print some for inspection
for i in range(1,21):
    s = "EMP#%d"%i
    print(s, str(nameuuid_bytes(s)))
