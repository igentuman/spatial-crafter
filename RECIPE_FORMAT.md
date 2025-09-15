# Spatial Crafter Recipe Format

This document describes the JSON format for Spatial Crafter recipes.

## Basic Recipe Structure

```json
{
  "type": "spatialcrafter:spatial_crafting",
  "multiblock": "spatialcrafter:spatial_structures/example_structure.nbt",
  "processing_time": 200,
  "energy_consumption": 1000,
  "do_not_destroy": false,
  "outputs": [
    {
      "item": "minecraft:diamond",
      "count": 1
    }
  ]
}
```

## Properties

### Required Properties

- **type**: Must be `"spatialcrafter:spatial_crafting"`
- **multiblock**: Resource location pointing to the multiblock structure NBT file
- **outputs**: Array of item outputs (can be empty if only entity outputs are used)

### Optional Properties

- **processing_time**: Time in ticks to complete the recipe (default: 200)
- **energy_consumption**: Energy required to complete the recipe (default: 1000)
- **do_not_destroy**: Boolean flag to prevent destroying the multiblock structure during crafting (default: false)
- **entity_outputs**: Array of entity outputs to spawn
- **global_nbt**: NBT data to apply to all output items

## New Feature: do_not_destroy

The `do_not_destroy` property is a boolean flag that controls whether the multiblock structure is destroyed when crafting begins.

- **false** (default): The multiblock structure will be destroyed when crafting starts, as usual
- **true**: The multiblock structure will remain intact during crafting, allowing for reusable structures

### Example with do_not_destroy

```json
{
  "type": "spatialcrafter:spatial_crafting",
  "multiblock": "spatialcrafter:spatial_structures/reusable_altar.nbt",
  "processing_time": 500,
  "energy_consumption": 2000,
  "do_not_destroy": true,
  "outputs": [
    {
      "item": "minecraft:diamond",
      "count": 2
    },
    {
      "item": "minecraft:emerald",
      "count": 1
    }
  ]
}
```

This recipe will process without destroying the multiblock structure, making it perfect for altar-like or shrine-like crafting setups that should remain permanent.

## Entity Outputs

```json
{
  "entity_outputs": [
    {
      "entity": "minecraft:villager",
      "count": 1,
      "position": [0, 1, 0],
      "nbt": "{CustomName:'{\"text\":\"Summoned Villager\"}'}"
    }
  ]
}
```

## Item Output with NBT

```json
{
  "outputs": [
    {
      "item": "minecraft:diamond_sword",
      "count": 1,
      "nbt": "{Enchantments:[{id:\"minecraft:sharpness\",lvl:5}]}"
    }
  ]
}
```