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

#### 🪑 Healing Benches  
*Rest while sitting to recover health*

**How It Works**:  
1. **Find a Bench**: Look for stair blocks marked as benches (usually in public areas)  
2. **Sit Down**: Right-click the **top face** of the stair while holding nothing  
3. **Get Buffs**:  
   - Instantly gain 4-second **Regeneration** ❤️  

**Rules**:  
- Works with the sitting plugin - just sit normally!  
- 4-second cooldown between uses (you'll see a warning message)  
- Only works on specific "bottom half" stairs configured as benches  

**Pro Tips**:  
- Great for recovering after battles  
- Combine with food for faster healing  
- Don't break bench blocks in the zone - they're special rest zones!

#### 🐾 Cat Companion Healing  
*Tamed cats now passively heal their owners*  

**How It Works**:  
1. **Tame Cats**: Befriend cats with fish to create healing helpers  
2. **Keep Nearby**: Cats must be within 10 blocks of you and **not sitting**  
3. **Auto-Heal**: Gain 1❤️ per active cat every 2.5 seconds automatically  

**Healing Details**:  
- Works while moving/exploring - no special actions needed  
- Multiple cats stack heals (3 cats = 3❤️ per heal cycle)  
- Never exceeds your max health level  
- Only works with cats you personally own  

**Rules**:  
🔸 Cats must be:  
   - Tamed and owned by you  
   - On their feet (not sitting command used)  
   - Within 10-block spherical radius  
🔸 Works in all game modes including PvP  
🔸 Silent operation - no messages or particles  

**Pro Tips**:  
- Create cat squads for rapid healing during exploration  
- Use name tags to keep track of your cats
- Combine with food for emergency health boosts  
- Keep beds nearby to prevent cats from wandering off

#### 🔦 Smart Light Systems  
*Automated and switch-controlled lighting for immersive day/night cycles*  

**What Players See**:  
- Special switches (buttons/levers) that control entire light zones  
- Lights toggle between **glowing** (e.g., Glowstone) and **non-glowing** (e.g., Cobblestone) states  
- Automatic behavior:  
  - All lights turn **off** at midnight (server time)  
  - All lights turn **on** at dawn (server time)  
- Multi-block lighting effects - entire rooms or areas change simultaneously  

**How It Works**:  
1. **Find a Switch**: Look for configured buttons/levers in buildings or areas  
2. **Activate**: Interact to toggle linked lights manually  
3. **Auto-Scheduling**:  
   - Midnight (tick 18000): Lights switch off (Glowstone → Cobblestone, Sea Lantern → Concrete)  
   - Dawn (tick 0): Lights restore to glowing versions  
4. **Safety Checks**:  
   - Prevents duplicate toggles (e.g., won’t turn off twice at midnight)  
   - Skips unloaded chunks to avoid lag  

**Admin Setup**:  
- Link switches to light blocks  
- **Commands**:  
  - Scan regions: `/scanlights`  
  - Export light data: `/exportlights`  
  - Debug: `/checklightregion`, `/lightinfo`  
  - Reload configs: `/reloadsunwayswitches`  
- Configure max region size (1M blocks) to prevent lag  

**Pro Tips**:  
- Use auto-scheduling for streetlights or dungeon day/night mechanics  
- Combine manual switches + timers for puzzle maps  
- Preset mappings ensure perfect transitions - don’t alter blocks manually!  

#### 🐾 Pet Finder  
*Locate your lost dogs and cats across Minecraft worlds*

**Commands**  
- `/findpets` - Search all loaded chunks for your missing pets  
- `/findpetsinarea <x1> <y1> <z1> <x2> <y2> <z2> [player]` (Admin) - Scan a specific cubic area for pets  

**Features**  
- Real-time chunk scanning with server-friendly batches  
- Filters by owner and custom pet names  
- Shows detailed information for each found pet:  
  - Pet type (Dog/Cat) and custom name  
  - Current/max health values  
  - Exact world coordinates  
  - Sitting/standing status  

**For Admins**  
- Area search requires two opposite cube corners (XYZ coordinates)  
- Optional player argument to check others' pets  
- Console shows progress updates during large searches  

**Tips**  
- Named pets appear with their custom names in results  
- Sitting pets remain stationary for easier retrieval  
- Check multiple worlds if pets are missing long-term  
- Uses vanilla taming system - only works with properly claimed pets  

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