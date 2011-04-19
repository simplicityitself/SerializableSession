class SerializableSessionGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.3.0 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    def scm = [ url: "https://github.com/dawsonsystems/SerializableSession" ]

    def author = "David Dawson"
    def authorEmail = "david@dawsonsystems.com"
    def title = "Enforce Serializable Session"
    def description = '''\\
For development only, installs a new Tomcat Valve that will monitor the
HTTP session and ensure that all objects that go into it can be serialized/ deserialized
according to java.io.Serializable rules.

It does this by actually serializing/ deserializing the session on every request and throwing an exception
(which is logged) if it does not conform.

To see more information on what is going on, set the logging level of com.dawsonsystems.session to INFO or DEBUG.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/serializable-session"
}
