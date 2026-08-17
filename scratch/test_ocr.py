import sys
import cv2
import pytesseract

image_path = sys.argv[1]
img = cv2.imread(image_path)

# Crop to the "Select type" area roughly
# 1024x576 image. The list is roughly from y=500 to y=750, x=50 to x=500
cropped = img[500:800, 50:500]
cv2.imwrite("scratch/autojoin_crop.png", cropped)

# Run tesseract
data = pytesseract.image_to_data(cropped, output_type=pytesseract.Output.DICT)
for i in range(len(data['text'])):
    if data['text'][i].strip():
        print(f"Text: '{data['text'][i]}' at ({data['left'][i]}, {data['top'][i]}, {data['width'][i]}, {data['height'][i]})")

text = pytesseract.image_to_string(cropped)
print("--- FULL TEXT ---")
print(text)
