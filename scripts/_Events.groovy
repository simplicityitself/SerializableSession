eventConfigureTomcat = { def tomcat ->
	def valveClass = this.class.classLoader.loadClass('com.flashsales.session.SessionTrackerValve')
	tomcat.host.addValve(valveClass.newInstance(tomcat))
}
