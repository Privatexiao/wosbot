import cv2
import sys

image_path = sys.argv[1]
img = cv2.imread(image_path)
if img is None:
    print(f"Failed to load image: {image_path}")
    sys.exit(1)

print(f"Image size: {img.shape}")
