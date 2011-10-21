Serializable Session grails plugin
==================================


Introduction
------------


This plugin adds a Tomcat valve that will ensure that any data placed in the session (including the Flash scope) is serializable according
to the Java Serializable conventions.


Features
--------

* Logs whenever a serialisation error has been detected.
* Optionally throw an exception to present a 500 error during the request.
* Optional exit the VM when a serialization error is detected.  This is useful when used in concert with a good set of HTTP based functional tests (eg selenium /Geb)
* Replaces the session contents with the data that has been de/serialized, ensuring a similar process is applied at development as at producton time.


Configuration
-------------

The following configuration options are available, with their defaults

    serializableSessions {
       throwExceptionOnFailure = true
       systemExitOnFailure = false
       replaceSession = true
    }

`throwExceptionOnFailure` indicates whether the system should fail the request by throwing an exception, this will generate an http 500 error when a serialization error occurs

`systemExitOnFailure` indicates that the entire VM should be shut down when serialization occurrs.  This is a good way to enforce rapid fixing of serialization errors as they are created.

`replaceSession`  indicates whether the contents of the session should be replaced by the new de/serialized contents.

New in 0.4
----------

Addition of the `replaceSession` option and feature.
