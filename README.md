# SunwayMinecraft Plugin  
**Enhance Your Minecraft Experience**  
*A collection of gameplay systems for Sunway Minecraft servers*  

---

## 🎮 For Players  
### Features You Can Use  

#### 🏮 Beacon Control System  
- **Temporarily disable/resume beacon effects** if admins allow player control  
- See visual changes when beacon update speeds are modified  

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

#### Extending Systems  
Example: Add custom light behaviors by extending `CelestialLightScheduler`:  
```java
public class CustomLightScheduler extends CelestialLightScheduler {
  @Override
  public void onMidnightTrigger() {
    // Your custom light logic
  }
}