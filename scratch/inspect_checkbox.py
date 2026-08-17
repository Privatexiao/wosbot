import sys
import cv2
import numpy as np

img = cv2.imread(sys.argv[1])
# Assuming text is found around x=150. Checkbox is likely around x=80.
# We'll crop x=40 to 120 for y=500 to 800 and look at colors.
crop = img[500:800, 40:120]
cv2.imwrite("scratch/checkbox_area.png", crop)

# Print a few pixels to see if there's green/gold
unique, counts = np.unique(crop.reshape(-1, 3), axis=0, return_counts=True)
colors = sorted(zip(counts, unique), reverse=True)
print("Top 10 colors in checkbox area (B, G, R):")
for count, color in colors[:10]:
    print(f"Count: {count}, Color: {color}")
