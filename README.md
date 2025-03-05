class to make accessing and reading from a database easier in other programs.

To use:
in intellij:
press ctrl + alt + shift + s
navigate to modules > dependencies
click the +
add from jar or directory
select the jar file from releases

Usage:

import com.ethankiers.databasemanagement.databasemanager.DatabaseManager;

the constructor is looking for the path to the sqlite db file as a string. Absolute or relative should work.

Methods:



