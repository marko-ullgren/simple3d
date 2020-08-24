# simple3d

This repository holds a vintage Java 1.1 application originally written by me in January 1998. Gently refactored in August 2020 from applet to application, also simple maven build added. Code also slightly reformatted and some JavaDocs inserted. Did not fix depracated code to maintain that 1990's feeling :)

## How to compile:

`mvn compile`

## How to run:

`mvn exec:java`

## Known issues

* Much of the used APIs are depracated. Some of them were depracated even in 1998
* Event handing is naive and using the depracated API. Oh well. As a result for example the frame cannot be closed normally but the process needs to be killed instead
* Much of the naming and commenting is in Finnish. Looks silly.
* I would do the OO design much differently today
