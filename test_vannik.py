import urllib.request
import zipfile
import io

url = "https://repo1.maven.org/maven2/com/vanniktech/emoji-ios/0.21.0/emoji-ios-0.21.0.jar"
req = urllib.request.Request(url)
resp = urllib.request.urlopen(req)
z = zipfile.ZipFile(io.BytesIO(resp.read()))
for name in z.namelist():
    if name.endswith("SmileysAndPeopleCategoryChunk1.class") or "People" in name:
        pass # we can't easily decompile class files in python
print("Downloaded")
