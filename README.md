# Ad Astra Space Suit Sealing

A Forge 1.20.1 add-on for [Ad Astra](https://modrinth.com/mod/ad-astra) (1.15.x, needs Botarium). Seal any armor at a
smithing table so it works as a space suit piece, keeping its looks, stats and enchantments.

## Sealing
Smithing table: **sealing template + any armor piece + the matching suit piece** (helmet with helmet, boots with boots...).
The result is the same armor piece with "Space Suit" (purple) or "Insulated Space Suit" (gold) in its tooltip.

| Template | Suit piece used | Works like |
|---|---|---|
| Space Suit Sealing | Space Suit piece | Space suit: oxygen + cold protection |
| Insulated Suit Sealing | Netherite Space Suit piece | Netherite space suit: oxygen + cold + heat (Venus, Mercury), fire immunity |

- Any mix of sealed pieces and real Ad Astra suit pieces counts as a full suit. The weakest piece decides: heat
  protection needs every piece insulated (or netherite suit).
- A space-sealed piece can be re-sealed as insulated.
- Pieces Ad Astra already counts as a suit (its own suits, or anything in `ad_astra:space_suit_items`) can't be
  sealed. To make another mod's armor space-worthy, seal it rather than adding it to Ad Astra's tags: tagged
  chestplates have no oxygen tank.
- A sealed chestplate gets its own oxygen tank (1,000 mB, like the space suit). Fill it in the Oxygen Loader or with a
  held Gas Tank, exactly like a real suit. Oxygen left in a suit chestplate used for sealing moves into the new tank.
- The oxygen bar on screen is Ad Astra's own, in the same place (Ad Astra's client config moves both).

## Capacity Upgrade
Smithing table: **Capacity Upgrade template + a sealed chestplate or an Ad Astra suit chestplate + a tank**.

| Tank | Adds | Until the tank holds |
|---|---|---|
| Steel Tank | +100 mB | 2,000 mB |
| Desh Tank | +250 mB | 4,000 mB |
| Ostrum Tank | +500 mB | 8,000 mB |
| Calorite Tank | +2,000 mB | 10,000 mB |

Each tank can be used again and again until its limit (the recipe's `max_capacity`). Nothing goes past `maxCapacity`
in `config/adastra_sealing-common.toml` (10,000 mB).

## For pack makers
- Recipes are JSON: `adastra_sealing:sealing` (`template`, `addition`, `seal`: `space`/`insulated`) and
  `adastra_sealing:capacity_upgrade` (`template`, `addition`, `amount`, optional `max_capacity`).
- Tags: `#adastra_sealing:space_suit_pieces`, `#adastra_sealing:insulated_suit_pieces` (what can seal),
  `#adastra_sealing:cannot_be_sealed` (armor that can't be sealed; empty by default).
- Stored in item NBT under `AdAstraSealing` (`Seal`, `Oxygen`, `CapacityBonus`).

## Building
Copy the three jars listed in `libs/README.md` into `libs/`, then `gradlew build` with Java 17.
