
eventConfigureTomcat = {def tomcat ->
  def valveClass = classLoader.loadClass("com.dawsonsystems.session.SessionTrackerValve")

  tomcat.host.addValve(valveClass.newInstance(tomcat))
}
