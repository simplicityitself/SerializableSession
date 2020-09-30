eventConfigureTomcat = { def tomcat ->
	def valveClass = this.class.classLoader.loadClass('com.dawsonsystems.session.SessionTrackerValve')
	tomcat.host.addValve(valveClass.newInstance(tomcat))
}
