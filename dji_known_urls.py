#!/usr/bin/env python3
"""
Enfoque Directo: URLs Conocidas de DJI SDK
Usar URLs específicas conocidas de la documentación DJI
"""

# URLs conocidas de DJI SDK v4 Android API
DJI_KNOWN_URLS = [
    # SDK Manager
    "https://developer.dji.com/api-reference/android-api/Components/SDKManager/DJISDKManager.html",
    
    # Camera y relacionados
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJICamera.html",
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIMediaManager.html",
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIPlaybackManager.html",
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJICameraDisplayNames.html",
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJICameraParameters.html",
    
    # Media Files
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIMediaFile.html",
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJIFetchMediaTaskScheduler.html",
    
    # Camera Settings
    "https://developer.dji.com/api-reference/android-api/Components/Camera/DJICameraSettingsDefinitions.html",
    
    # Flight Controller
    "https://developer.dji.com/api-reference/android-api/Components/FlightController/DJIFlightController.html",
    
    # Gimbal
    "https://developer.dji.com/api-reference/android-api/Components/Gimbal/DJIGimbal.html",
    
    # Battery
    "https://developer.dji.com/api-reference/android-api/Components/Battery/DJIBattery.html",
    
    # Remote Controller
    "https://developer.dji.com/api-reference/android-api/Components/RemoteController/DJIRemoteController.html",
    
    # Common/Utils
    "https://developer.dji.com/api-reference/android-api/Components/Utils/DJICommonCallbacks.html",
    "https://developer.dji.com/api-reference/android-api/Components/Utils/DJIError.html",
    
    # Missions
    "https://developer.dji.com/api-reference/android-api/Components/Missions/DJIMissionManager.html",
    "https://developer.dji.com/api-reference/android-api/Components/Missions/DJIWaypointMission.html",
    
    # AirLink
    "https://developer.dji.com/api-reference/android-api/Components/AirLink/DJIAirLink.html",
    
    # KeyManager
    "https://developer.dji.com/api-reference/android-api/Components/KeyManager/DJIKeyManager.html",
]

# Guardar en archivo
def save_known_urls():
    with open('dji_known_urls.txt', 'w', encoding='utf-8') as f:
        f.write("# URLs Conocidas de DJI SDK v4 Android API\n")
        f.write("# Generado automáticamente\n\n")
        for url in DJI_KNOWN_URLS:
            f.write(f"{url}\n")
    
    print(f"📝 Guardadas {len(DJI_KNOWN_URLS)} URLs conocidas en 'dji_known_urls.txt'")
    
    # Categorizar las URLs
    categories = {
        'camera': [],
        'media': [],
        'flight': [],
        'gimbal': [],
        'battery': [],
        'remote': [],
        'missions': [],
        'utils': [],
        'general': []
    }
    
    for url in DJI_KNOWN_URLS:
        url_lower = url.lower()
        if 'camera' in url_lower or 'media' in url_lower or 'playback' in url_lower:
            if 'media' in url_lower or 'playback' in url_lower:
                categories['media'].append(url)
            else:
                categories['camera'].append(url)
        elif 'flight' in url_lower:
            categories['flight'].append(url)
        elif 'gimbal' in url_lower:
            categories['gimbal'].append(url)
        elif 'battery' in url_lower:
            categories['battery'].append(url)
        elif 'remote' in url_lower:
            categories['remote'].append(url)
        elif 'mission' in url_lower:
            categories['missions'].append(url)
        elif 'util' in url_lower or 'common' in url_lower or 'error' in url_lower:
            categories['utils'].append(url)
        else:
            categories['general'].append(url)
    
    # Guardar por categorías
    for category, urls in categories.items():
        if urls:
            with open(f'dji_urls_{category}.txt', 'w', encoding='utf-8') as f:
                for url in urls:
                    f.write(f"{url}\n")
            print(f"📁 {category}: {len(urls)} URLs")

if __name__ == "__main__":
    print("🎯 GENERANDO URLs CONOCIDAS DE DJI SDK")
    print("=" * 50)
    save_known_urls()
    print("\n✅ URLs preparadas para crawler!")
