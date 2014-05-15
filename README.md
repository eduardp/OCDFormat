OCDFormat
=========

A small eclipse plugin that will align the equals signs on multiple lines and align words on multiple lines in columns.

Like most developers, I'm pretty particular about how I format my code. For example when I've got a few assignment statements together, I like to align the equals signs in a column. Eclipse does not do this for you inside methods. I also like to have my variable declarations to be columnized, Eclipse can do this, but not in the nicest way. I found myself spending a lot of time formatting code. So I created a plugin that will format my code in the way I like it.

Installation
------------
Download the binary (CDFormat_1.0.0.1.jar) from the repo add it to your Eclipse plugins or dropins directory

Usage
-----
Highlight a piece of code that you'd like to have formatted, and use either the keyboard shortcut (Ctrl+4) or the toolbar button (![Alt Text](https://github.com/eduardp/OCDFormat/blob/master/icons/icon.gif?raw=true)) to apply the formatting. The idea is that you'd highlight small coherent blocks of code to format - this is not a Ctrl+A / Format type of plugin.

Examples 
--------
Here are some example of the difference in formatting :
### OCDFormat ###
![Alt Text](http://github.com/eduardp/OCDFormat/blob/master/images/ocdfinal.png?raw=true)

### Eclipse ###
![Alt Text](http://github.com/eduardp/OCDFormat/blob/master/images/eclipsefinal.png?raw=true)

As you can see, Eclipse does not format declarations and assignment statements inside methods at all. At field level Eclipse will always align all your variable declarations and assignments together, even if you select just a few lines and tell Eclipse to format them, it will format them as if you'd selected all the statements, leaving the assignments hanging way out to the right, and not allowing separate formatting for the 'groups' of assignments. With OCDFormat, you can format smaller regions independently.

Conclusion
----------
I would have liked to incorporate the plugin into the Eclipse formatter, but I don't have the time to figure out the integration.

Update
------
I added a new small feature to the plugin - a command that will move the cursor to the opening brace of the scope that the cursor is currently in. Both round and curly braces are considered. The command is bound to Ctrl+5 by default
