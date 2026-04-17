# SunwayMinecraft Plugin  
**Enhance Your Minecraft Experience**  
*A collection of gameplay systems for Sunway Minecraft servers*  

---

## 🎮 For Players  

### 🌍 Server World Design  
SunwayMinecraft uses a **two-world survival structure**:

- **Living World**: the permanent main world for building, housing, city life, and long-term survival
- **Mining World**: a separate world intended for exploration and resource gathering

This helps preserve the quality of the permanent world while still giving players room to gather materials freely.

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
- Special switches (buttons/levers) that control one or more lights within special regions  
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
  - `/servertime` - Local time (SG/MY time)
  - `/servertimeutc` - UTC time  

#### 🪙 Coin Flip  
*Wager items or currency on chance outcomes*

**How It Works**:  
1. **Place Bet**: Use `/cf <amount> <heads|tails>` for money or `/cf item <amount> <heads|tails>` for items  
2. **Flip Coin**: System generates instant 50/50 outcome  
3. **Get Results**: Win = 2x return, Lose = forfeit wager  

**Rules**:  
- Money bets require sufficient balance  
- Item bets use held stackable items only (no tools/armor)  
- Maximum item bet: 1 stack (64 items)  
- Mute messages with `/cf mute`  

**Pro Tips**:  
- Check stats with `/cf stats` to track performance  
- Start with small wagers to learn probabilities  
- Combine with trading for resource exchanges  
- Use mute feature during frequent plays  

#### ⛏️ Mining World & Living World  
*Two worlds with different purposes: one for renewable resources, one for permanent life*

**What These Worlds Are**:  
- **Living World**: The permanent world where long-term building, housing, city life, and daily survival happen  
- **Mining World**: A separate resource world for gathering materials without permanently stripping the main world  

**Commands**:  
- `/mining` - Teleport to the Mining World  
- `/living` - Teleport to the permanent Living World  

**How It Works**:  
1. Use `/living` when you want to return to the permanent main world  
2. Use `/mining` when you want to gather resources in the separate mining world  
3. The two worlds serve different gameplay purposes:  
   - **Living World** = long-term building and settlement  
   - **Mining World** = renewable exploration and extraction  

**Why This Exists**:  
- Keeps the permanent world cleaner and more livable  
- Prevents the main survival world from becoming stripped and ugly over time  
- Gives players a dedicated place to explore, dig, and gather materials  

**Rules**:  
- Do not treat the Mining World as permanent land for long-term builds  
- Valuable or permanent builds belong in the Living World  
- Resource gathering is encouraged in the Mining World  

**Pro Tips**:  
- Use `/living` before starting long building sessions  
- Use `/mining` when you need wood, stone, ores, or open land to explore  
- Store your important items and builds in the permanent world, not the resource world  

#### 🏠 Residency & Storefronts  
*Rent homes, run businesses, and live inside the city as part of normal survival progression*

**What This System Is**:  
- A city property system for **Residency** and **Storefronts** inside staff-managed municipal zones  
- Designed for the **spawn city** and future registered municipal districts  
- Separate from ordinary wilderness survival and land claiming  
- Built for both **housing** and **commercial life** from the start  

**What Players Can Do**:  
- Rent registered city units such as:  
  - Apartments  
  - Houses or townhouses  
  - Shoplots  
  - Offices  
  - Market stalls  
  - Mixed-use units  
- Browse available properties through the public listing system  
- Move into residential units as part of everyday progression  
- Rent commercial units and use them as public-facing storefronts  
- Manage approved guests for your unit  
- Pay rent and keep your city space active  

**How It Works**:  
1. **Browse Listings**: Use `/residency list` or `/storefront list` to view available city units  
2. **View Details**: Check the unit type, district, rent, deposit, and listing conditions  
3. **Rent a Unit**:  
   - Standard units can be rented directly  
   - Premium or prestige units may require approval  
4. **Move In or Operate**:  
   - Residential units become your city home  
   - Commercial units become your public business space  
5. **Manage Access**: Add guests or approved helpers where allowed  
6. **Keep Up Payments**: Pay your rent on time to avoid restrictions or repossession  

**Residency Features**:  
- Units can be **residential**, **commercial**, or **mixed-use**  
- Buildings can contain multiple separate units  
- Some units include linked side areas such as storage rooms or back sections  
- Every unit belongs to a registered district in the city  
- Rent and deposit values can differ by location, type, and prestige  

**Storefront Features**:  
- Commercial units can allow **selective public access**  
- Shops can be open to visitors without exposing everything inside  
- Public interaction can be allowed in chosen areas while private areas stay restricted  
- Storefronts are intended to support visible city life, not just storage or landholding  

**Access Rules**:  
- Your permission level depends on your role in the unit  
- Roles may include:  
  - Tenant  
  - Manager  
  - Guest  
  - Staff override  
- Residential units are usually more private  
- Commercial units may allow partial public access  
- Event access and guest access can be granted without giving full control  

**Rent and Repossession Rules**:  
- Some units require a **deposit** plus recurring rent  
- Missing payment does not instantly destroy your lease  
- The system uses a progression like:  
  - Warning  
  - Grace period  
  - Restricted access / arrears  
  - Repossession  
- If a unit is repossessed, contents are protected through controlled recovery handling rather than being left open to grief  

**Important Notes**:  
- This is a **city system**, not a wilderness claiming system  
- The city is managed separately from ordinary survival claims  
- Public city life is meant to feel more structured than the open survival world  
- Not every city unit is permanent ownership - many are meant to be rented and actively used  

**Commands**:  
- `/residency list` - View rentable city units  
- `/residency view <unit>` - View details for a specific unit  
- `/residency rent <unit>` - Rent an available unit  
- `/residency myunits` - View your current rented units  
- `/residency pay <unit>` - Pay rent for a unit  
- `/residency guests <unit>` - Manage guest access where allowed  
- `/storefront list` - View available commercial units  

**Pro Tips**:  
- Use residential units to anchor yourself in the city instead of treating spawn as a pass-through area  
- Mixed-use units are useful if you want both city housing and a public-facing presence  
- Keep rent paid before grace periods become a problem  
- Premium locations may be more expensive, but they also matter more socially  
- If you want city visibility, storefronts are one of the best ways to establish your presence  

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
| **Cat Healing**      | Passive healing via nearby tamed cats        | `HealingSystem`            |
| **Coin Flip**        | Player item/money wager system               | `CoinFlipSystem`, `CoinFlipCommands` |
| **World Travel**     | Player-facing travel between permanent and mining worlds | `WorldTravelCommands` |
| **Residency & Storefronts** | Municipal premises engine for rentable housing and commercial city units | `ResidencyManager`, `ResidencyBootstrap`, `PremisesAccessService`, `BillingService` |

### Configuration  
*Handled via YAML files in `/plugins/SunwayMinecraft/`*:  
- `benches.yml` - Bench locations, owners, regions  
- `switches.yml` - Light activation rules/conditions  
- `lights.yml` - Light source configurations  
- `districts.yml` - Municipal district definitions and district regions  
- `buildings.yml` - Registered city buildings and parent structures  
- `units.yml` - Individual rentable units, unit regions, linked subregions, and listing settings  
- `property-pricing.yml` - Default rent, deposit, and billing profile settings  
- `property-policies.yml` - Lease policy, grace-period, and public-access defaults  
- `residency-settings.yml` - General settings for the Residency and Storefronts system  

**Reload safely with**:  
- `/reloadsunwayconfig` - Main settings  
- `/reloadsunwaybenches` - Bench data  
- `/reloadsunwayswitches` - Light systems  

### Developer Notes  
#### Architecture  
- **Modular Design**: Each system (e.g., `BeaconManager`, `PetFinderManager`) operates independently  
- **Event-Driven**: Uses Bukkit listeners (e.g., `BenchInteractListener`, `SwitchListener`)  
- **Schedulers**: Repeating tasks for healing, light checks, and beacon updates