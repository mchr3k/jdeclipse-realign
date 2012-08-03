jdeclipse-realign
=================

Credits
-------

* Main JD Eclipse plugin (http://java.decompiler.free.fr/) by Emmanuel Dupuy
* This project is a fork of Realignment for JD-Eclipse (http://sourceforge.net/projects/realignmentjd/) by Alex Kosinsky

Features
--------

This project adds the following:

*01/08/2012 (1.0.6)*

* Add a custom editor icon. 

*08/07/2012 (1.0.5)*

* Add "Open Class With" dynamic submenu to choose which editor to open class files with.
* Add "Attach Source..." action to "Open Class With" dynamic submenu.
* Fix source caching so that after decompiling a class you can still open the normal "Class Viewer" and see attached source.

*05/07/2012 (1.0.4)*

* Alignment for fields and enums in addition to method alignment which was implemented by Alex.
* Automated file association setup on first startup. In Eclipse Juno the new "class without source" file type 
is defaulted to JD Eclipse and "class" is defaulted back to the built in class viewer so that real 
source will be displayed.
* A single Eclipse update site to download JD-Eclipse along with this project: http://mchr3k-eclipse.appspot.com/