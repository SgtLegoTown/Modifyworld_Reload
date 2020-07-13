# Modifyworld_Reload
Restriction plugin for Bukkit. Part of PermissionsEx bundle.

An attempt to change the project to my needs.
Namely:
1. Divide the left click and right click with objects in hand into blocks.
2. Add a separation of rights for finished items in the stove and other mechanisms. Like on a workbench. Anvil, potion, enchantment table ...

Event:
Removed:
sneaking, running, sleeping, buckets, chat, picking up things, throwing things away, chest control, digestion

Change PHYSICAL to LEFT_CLICK_BLOCK and change the rule (use => left)
Add the same action on RIGHT_CLICK_BLOCK and change the rule (use => right)
