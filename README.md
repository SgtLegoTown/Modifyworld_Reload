# Modifyworld_Reload

----------------------------------------------------
Compiled. Earned on 1.16.1
----------------------------------------------------


Restriction plugin for Bukkit. Part of PermissionsEx bundle.

An attempt to change the project to my needs.
Namely:
1. Divide the left click and right click with objects in hand into blocks.
2. Add a separation of rights for finished items in the stove and other mechanisms. Like on a workbench. Anvil, potion, enchantment table ...

Event:
-----------

Please test on 1.16!!!
------------------------
Delete:
modifyworld.login
modifyworld.chat
modifyworld.sneak
modifyworld.sprint
modifyworld.chat.private
modifyworld.usebeds
modifyworld.bucket.*

-----------------------------
Changes:
--------------------
modifyworld.items.use.<itemid>.on.block.<blockid> 
on
modifyworld.items.left.<itemid>.on.block.<blockid>
and
modifyworld.items.right.<itemid>.on.block.<blockid>
