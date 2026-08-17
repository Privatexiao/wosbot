import cv2
import os
import sys
import time

def crop_and_save(img_path, window_name, save_path):
    print(f"Opening {img_path} for {window_name}...")
    img = cv2.imread(img_path)
    if img is None:
        print(f"Error loading {img_path}")
        return False
    
    cv2.namedWindow(window_name, cv2.WINDOW_NORMAL)
    cv2.resizeWindow(window_name, 720, 1280)
    cv2.setWindowProperty(window_name, cv2.WND_PROP_TOPMOST, 1)
    
    r = cv2.selectROI(window_name, img, showCrosshair=True, fromCenter=False)
    cv2.destroyWindow(window_name)
    
    if r[2] > 0 and r[3] > 0:
        cropped = img[int(r[1]):int(r[1]+r[3]), int(r[0]):int(r[0]+r[2])]
        os.makedirs(os.path.dirname(save_path), exist_ok=True)
        cv2.imwrite(save_path, cropped)
        print(f"Saved to {save_path}")
        return True
    else:
        print("Selection cancelled or invalid.")
        return False

def main():
    base_dir = r"E:\MeComputer\Desktop\wosbot"
    img1 = os.path.join(base_dir, "image", "1.png")
    img2 = os.path.join(base_dir, "image", "2.png")
    
    save1 = os.path.join(base_dir, "modules", "vision", "src", "main", "resources", "templates", "city", "hospital_field_icon.png")
    save2 = os.path.join(base_dir, "modules", "vision", "src", "main", "resources", "templates", "city", "hospital_heal_button.png")
    
    print("Please look at the popup windows and drag a box around the requested icon.")
    
    success1 = crop_and_save(img1, "Draw box around 1% HOSPITAL ICON, then press Space/Enter", save1)
    time.sleep(1)
    success2 = crop_and_save(img2, "Draw box around HEAL button, then press Space/Enter", save2)
    
    # Update properties
    props_file = os.path.join(base_dir, "modules", "api", "src", "main", "resources", "config", "templates.properties")
    lines = []
    if os.path.exists(props_file):
        with open(props_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            
    new_props = []
    if success1:
        new_props.append("HOSPITAL_FIELD_ICON=/templates/city/hospital_field_icon.png\n")
    if success2:
        new_props.append("HOSPITAL_HEAL_BUTTON=/templates/city/hospital_heal_button.png\n")
        
    if new_props:
        with open(props_file, 'a', encoding='utf-8') as f:
            for prop in new_props:
                key = prop.split('=')[0]
                if not any(line.startswith(key + '=') for line in lines):
                    f.write(prop)
        print("Updated templates.properties!")
        
    print("Done! You can close this window if it's still open.")

if __name__ == '__main__':
    main()
