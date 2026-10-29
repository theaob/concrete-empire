"""Minimal zipalign: rewrites an APK so every stored (uncompressed) entry starts on a 4-byte boundary.
resources.arsc is forced to stored, as Android 11+ requires for targetSdk >= 30."""
import sys, zlib, struct, zipfile

src, dst = sys.argv[1], sys.argv[2]
zin = zipfile.ZipFile(src)
out = open(dst, 'wb')
central = []
for info in zin.infolist():
    data = zin.read(info.filename)
    name = info.filename.encode('utf-8')
    store = info.compress_type == zipfile.ZIP_STORED or info.filename == 'resources.arsc' or info.filename.endswith('.png')
    if store:
        method, payload = 0, data
    else:
        co = zlib.compressobj(9, zlib.DEFLATED, -15)
        method, payload = 8, co.compress(data) + co.flush()
    crc = zlib.crc32(data) & 0xffffffff
    off = out.tell()
    extra = b''
    if method == 0:
        pad = (4 - (off + 30 + len(name)) % 4) % 4
        extra = b'\0' * pad
    flags = 0x800
    out.write(struct.pack('<IHHHHHIIIHH', 0x04034b50, 20, flags, method, 0, 0x21,
                          crc, len(payload), len(data), len(name), len(extra)))
    out.write(name); out.write(extra); out.write(payload)
    central.append((name, method, crc, len(payload), len(data), off, flags))
cd_off = out.tell()
for name, method, crc, csize, usize, off, flags in central:
    out.write(struct.pack('<IHHHHHHIIIHHHHHII', 0x02014b50, 20, 20, flags, method, 0, 0x21,
                          crc, csize, usize, len(name), 0, 0, 0, 0, 0, off))
    out.write(name)
cd_size = out.tell() - cd_off
out.write(struct.pack('<IHHHHIIH', 0x06054b50, 0, 0, len(central), len(central), cd_size, cd_off, 0))
out.close()
