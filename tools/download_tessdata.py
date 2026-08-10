import urllib.request
import os

urls = {
    "eng.traineddata": "https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata",
    "chi_sim.traineddata": "https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata",
    "osd.traineddata": "https://github.com/tesseract-ocr/tessdata_fast/raw/main/osd.traineddata"
}

directories = [
    r"e:\MeComputer\Desktop\wosbot\tools\tesseract",
    r"e:\MeComputer\Desktop\wosbot\fg-app\target\lib\tesseract",
    r"e:\MeComputer\Desktop\wosbot\fg-app\src\main\resources\tesseract"
]

for d in directories:
    os.makedirs(d, exist_ok=True)
    for filename, url in urls.items():
        filepath = os.path.join(d, filename)
        print(f"Downloading {filename} to {filepath}...")
        urllib.request.urlretrieve(url, filepath)
        size = os.path.getsize(filepath)
        print(f"  Done. Size: {size} bytes")

print("All tessdata downloads completed successfully!")
