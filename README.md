# SunwayMinecraft Plugin  
**Enhance Your Minecraft Experience**  
*A collection of gameplay systems for Sunway Minecraft servers*  

---

## 🎮 For Players  
### Features You Can Use  

#### 🏮 Beacon Light Shows  
*Color-changing rooftop beacons across the server*

**What Players See**:  
- Special beacons on building rooftops with **rotating colored beams**  
- Each beacon has 5 glass blocks above it that cycle through colors  
- Smooth transitions between colors (e.g., red → blue → green)  

**How It Works**:  
- Beacons automatically change colors - no player interaction needed  
- Admins can control:  
  - Color transition speed (`/setbeaconticks`)  
  - Pause/resume effects (`/pausebeacons` & `/resumebeacons`)  

**Pro Tip**:  
- The color patterns cycle over time—be patient and watch how the beacon’s mood shifts!
- Don't break the glass blocks above beacons - they're part of the light system! 

#### 🪑 Interactive Benches  
- Right-click configured benches to sit  
- View bench ownership/region info with commands  

#### 🔦 Smart Light Systems  
- Discover automated lights that activate at midnight (server time)  
- Admins can create light zones with special switches  

#### 🐾 Pet Assistance  
- Use commands to locate lost pets:  
  - `/findpets` - Search for all your missing pets  
  - `/findpetsinarea [radius]` - Check a specific area  

#### ⏰ Real-World Time  
- Check server time synced to real life:  
  - `/servertime` - Local time  
  - `/servertimeutc` - UTC time  

---

## 🛠 For Server Admins & Developers  

### Key Systems  
| System               | Description                                  | Main Classes               |  
|----------------------|----------------------------------------------|----------------------------|  
| **Beacon Manager**   | Handles beacon effect states/tick rates      | `BeaconManager`, `BeaconCommands` |  
| **Bench System**     | Manages sit-able benches with regions        | `RegionManager`, `BenchesConfigManager` |  
| **Light Automation** | Celestial/switch-controlled lighting         | `CelestialLightScheduler`, `SwitchListener` |  
| **Pet Finder**       | Pet tracking via entity scanning             | `PetFinderManager`         |  
| **Real-Time Sync**   | UTC/timezone calculations                    | `RealTimeManager`          |  

### Configuration  
*Handled via YAML files in `/plugins/SunwayMinecraft/`*:  
- `benches.yml` - Bench locations, owners, regions  
- `switches.yml` - Light activation rules/conditions  
- `lights.yml` - Light source configurations  

**Reload safely with**:  
- `/reloadsunwayconfig` - Main settings  
- `/reloadsunwaybenches` - Bench data  
- `/reloadsunwayswitches` - Light systems  

### Developer Notes  
#### Architecture  
- **Modular Design**: Each system (e.g., `BeaconManager`, `PetFinderManager`) operates independently  
- **Event-Driven**: Uses Bukkit listeners (e.g., `BenchInteractListener`, `SwitchListener`)  
- **Schedulers**: Repeating tasks for healing, light checks, and beacon updates