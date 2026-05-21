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

#### 📜 City Contracts
*Take on specialized tasks issued by the city for monetary rewards*

**How It Works**:
1. **Find Work**: Use `/contracts board` to view available tasks from the city.
2. **Review Details**: Use `/contracts info <id>` to check requirements, rewards, and objectives.
3. **Accept**: Use `/contracts accept <id>` to start the contract.
4. **Complete**: 
   - **Hauling**: Gather the required materials and use `/contracts complete <id>` at the designated depot.
   - **Courier/Survey**: Travel to the destination endpoint or target asset.
   - **Maintenance**: Visit the target asset and perform the required interaction.
5. **Get Paid**: Successful completion instantly grants a fixed monetary reward.

**Rules**:
- **Cooldowns**: Abandoning or failing a contract applies a cooldown period before it can be taken again.
- **Limits**: Players can hold a limited number of active contracts at once (default: 3).
- **Inventory**: Hauling contracts consume materials from your inventory upon completion.
- **Timing**: Complete tasks before their expiration time to avoid failure.

**Commands**:
- `/contracts board` - View available city contracts
- `/contracts info <id>` - View contract details and rewards
- `/contracts active` - View your currently active contracts
- `/contracts complete <id>` - Turn in a completed contract
- `/contracts abandon <id>` - Drop a contract (applies cooldown)

**Pro Tips**:
- Use contracts to earn extra money while exploring or gathering resources.
- Hauling contracts are a great way to profit from excess materials like stone or wood.
- Check `/contracts active` frequently to track your remaining time.

#### 📢 City Events
*Timed operations and special promotions that modify city life and work rewards*

**How It Works**:
1. **Stay Informed**: Watch for city-wide announcements when events start or end.
2. **Check Current**: Use `/events current` to see which events are active and how much time remains.
3. **Take Advantage**: Active events provide **Reward Multipliers** (e.g., 1.5x pay) and **Boosted Categories** for City Contracts.
4. **Learn More**: Use `/events info <id>` to see the exact benefits of an active or configured event.

**Common Event Types**:
- **Municipal Supply Drive**: Higher rewards for Hauling and Courier tasks.
- **Safety Inspection Week**: Boosted pay for Maintenance and Survey work.
- **Golden Vein Discovery**: Major bonus for Recovery tasks in the Mining World.
- **Trade Promotion**: Economic stimulus for all delivery-related contracts.

**Rules**:
- **Multipliers**: If multiple events apply to a contract, only the **highest** multiplier is used (they do not stack).
- **Visibility**: Boosted contracts are clearly marked as `[BOOSTED]` on the `/contracts board`.

**Commands**:
- `/events current` - View currently active city events
- `/events info <id>` - View detailed information about an event
- `/events upcoming` - See scheduled future events

#### 🏮 Beacon Light Shows  
... (rest of features)

---

## 🛠 For Server Admins & Developers  

### Key Systems  
| System               | Description                                  | Main Classes               |  
|----------------------|----------------------------------------------|----------------------------|  
| **City Events**      | Managed operational events with reward boosts| `CityEventsManager`, `EventModifierService`, `EventsCommands` |
| **City Contracts**   | Managed task system for procurement and logistics | `ContractsManager`, `ContractsCommands`, `ContractVerificationService` |
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
- `city-events.yml` - Definitions for all available City Events
- `city-event-settings.yml` - Global module settings for City Events
- `city-event-state.yml` - Persistent state for active events
- `contracts.yml` - Definitions for all available City Contracts  
- `contract-endpoints.yml` - Registered locations for pickup, dropoff, and task points  
- `contract-settings.yml` - Global module settings for City Contracts  
...

**Reload safely with**:  
- `/eventadmin reload` - Event configurations
- `/contractadmin reload` - Contract configurations  
- `/reloadsunwayconfig` - Main settings  
...

### Developer Notes  
#### Architecture  
- **Modular Design**: Each system (e.g., `BeaconManager`, `PetFinderManager`) operates independently  
- **Event-Driven**: Uses Bukkit listeners (e.g., `BenchInteractListener`, `SwitchListener`)  
- **Schedulers**: Repeating tasks for healing, light checks, beacon updates, and event cleanup
