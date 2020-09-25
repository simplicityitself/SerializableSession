import grails.util.Environment

eventConfigureTomcat = { def tomcat ->
	if (Environment.current in [Environment.DEVELOPMENT, Environment.TEST]) {
		def valveClass = this.class.classLoader.loadClass('com.flashsales.session.SessionTrackerValve')
		tomcat.host.addValve(valveClass.newInstance(tomcat))
	}
}
