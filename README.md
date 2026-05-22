# SunwayMinecraft Plugin  
**Enhance Your Minecraft Experience**  
*A collection of gameplay systems for Sunway Minecraft servers*  

---

## 🎮 For Players  

### 🌆 City Overview
The city is the heart of the server's economy and social life. Use `/city` to get a summary of what's happening.

**New to the city? Follow these steps:**
1. Use `/city` to see the current state of the city.
2. Check `/events current` to see which activities are currently boosted.
3. Open `/contracts board` to find available city work.
4. Take a **hauling** contract (look for `[BOOSTED]` tags for extra pay!).
5. Gather resources from survival areas or the `/mineworld`.
6. Use `/contracts complete <id>` at a city depot to get paid.
7. Use `/residency list` to find affordable city housing or a storefront for your business.
8. Use `/district` to learn about the rules and zoning of your current location.

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

#### 🏢 Districts / Zoning
*Learn about the rules and characteristics of your current location*

**How It Works**:
- The city is divided into several **Districts**, each with its own purpose (Residential, Commercial, Industrial, etc.).
- Use `/district` while standing anywhere to see which district you are in.
- Some districts may have specific rules or provide different services.

**Commands**:
- `/district` - Show info about your current district
- `/district info <id>` - View details about a specific district
- `/district list` - List all public city districts

#### 🏠 Residency / Storefronts
*Participate in the city economy by renting housing or commercial space*

**How It Works**:
1. **Find a Unit**: Use `/residency list` or `/storefront list` to see available properties.
2. **View Details**: Use `/residency view <id>` to check rent, deposit, and features.
3. **Rent**: If you have enough money, use `/residency rent <id>` to start your lease.
4. **Manage**: Use `/residency guests` to give friends access to your home.
5. **Pay Rent**: Your rent is automatically deducted, but you can use `/residency pay` to pay in advance.

**Commands**:
- `/residency list` - List available residential units
- `/residency myunits` - View your active rentals
- `/residency guests` - Manage access for other players
- `/storefront list` - List available commercial storefronts

---

## 🛠 For Server Admins & Developers  

### Key Systems  
| System               | Description                                  | Main Classes               |  
|----------------------|----------------------------------------------|----------------------------|  
| **City Overview**    | Unified player-facing city summary           | `CityOverviewService`, `CityCommands` |
| **City Metrics**     | Performance and activity tracking            | `CityMetricsManager`, `CityMetricsRepository` |
| **City Validation**  | Cross-system QA and integrity checks         | `CityValidationService`, `CityAdminCommands` |
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
- `city-metrics.yml` - Persistent counters for city activity (Contracts, Events, etc.)
- `city-events.yml` - Definitions for all available City Events
- `city-event-settings.yml` - Global module settings for City Events
- `city-event-state.yml` - Persistent state for active events
- `contracts.yml` - Definitions for all available City Contracts  
- `contract-endpoints.yml` - Registered locations for pickup, dropoff, and task points  
- `contract-settings.yml` - Global module settings for City Contracts  
...

**Reload safely with**:  
- `/cityadmin validate` - Run health checks across all city systems
- `/cityadmin stats` - View real-time activity metrics
- `/eventadmin reload` - Event configurations
- `/contractadmin reload` - Contract configurations  
- `/reloadsunwayconfig` - Main settings  
...

### Developer Notes  
#### Architecture  
- **Modular Design**: Each system (e.g., `BeaconManager`, `PetFinderManager`) operates independently  
- **Event-Driven**: Uses Bukkit listeners (e.g., `BenchInteractListener`, `SwitchListener`)  
- **Schedulers**: Repeating tasks for healing, light checks, beacon updates, and event cleanup
