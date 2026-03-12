# NeoForge 1.21.1 Migration Plan for Spatial Crafter

This rule outlines the detailed steps to migrate the **Spatial Crafter** mod from Forge 1.20.1 to **NeoForge 1.21.1**, based on patterns established in `multi-builder-tool`.
Consider existing implementation here: /mnt/data/IdeaProjects/multi-builder-tool
## Status: Phase 1 Completed
- [x] Gradle configuration updated (`build.gradle`, `gradle.properties`).
- [x] Dependencies updated to 1.21.1 versions.
- [x] Project imported and indexed.

## Phase 2: Registry and Basic Mod Structure (CURRENT)
- **Task**: Fix `Setup.java` and `Main.java` registration logic.
- **Steps**:
  1. **Update Imports**: Replace `net.minecraftforge` with `net.neoforged`.
  2. **DeferredRegister**: Change to `net.neoforged.neoforge.registries.DeferredRegister`.
  3. **RegistryObject to DeferredHolder**: Replace all `RegistryObject` with `DeferredHolder`.
  4. **Registries**: Use `Registries.BLOCK`, `Registries.ITEM`, etc., instead of `ForgeRegistries`.
  5. **Main Constructor**: Use the `IEventBus` passed to the constructor instead of `FMLJavaModLoadingContext`.
  6. **MenuTypes**: Replace `IForgeMenuType` with `IMenuTypeExtension`.
  7. **Screen Registration**: Move `MenuScreens.register` from `Main.init` to a listener for `RegisterMenuScreensEvent` on the mod bus.

## Phase 3: Networking
- **Task**: Migrate `NetworkHandler.java` and packets to the **Payload** system.
- **Steps**:
  1. Implement `CustomPacketPayload` for all packets.
  2. Define `Type<T>` and `StreamCodec` for each packet.
  3. Use `RegisterPayloadHandlersEvent` to register packets on the mod bus.

## Phase 4: Capabilities
- **Task**: Migrate `SpatialCrafterBlockEntity.java` to the new **Block Capability** system.
- **Steps**:
  1. Remove `LazyOptional` and `getCapability` overrides.
  2. Register capabilities using `RegisterCapabilitiesEvent`.
  3. Update accessors to use `level.getCapability(...)`.

## Phase 5: Data Components & Resource Locations
- **Task**: Replace NBT with Data Components where necessary and update `ResourceLocation` usage.
- **Steps**:
  1. Replace `new ResourceLocation(...)` with `ResourceLocation.fromNamespaceAndPath(...)`.
  2. Identify NBT data on items and migrate to `DataComponentType`.

## Phase 6: Recipes and JSON Resources
- **Task**: Update recipe serializers and JSON formats.
- **Steps**:
  1. Update `SpatialCrafterRecipeSerializer` to use `MapCodec` and `StreamCodec`.
  2. Ensure JSON files in `src/main/resources` match 1.21.1 formats.

