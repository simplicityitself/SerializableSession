package com.dawsonsystems.session

import javax.servlet.ServletException
import org.apache.catalina.Session
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import org.apache.catalina.session.StandardSession
import org.apache.catalina.valves.ValveBase
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.apache.catalina.util.CustomObjectInputStream
import org.apache.catalina.session.StandardManager

public class SessionTrackerValve extends ValveBase {
  private static Logger log = LoggerFactory.getLogger(SessionTrackerValve);
  def tomcat

  SessionTrackerValve(def tomcat) {
    this.tomcat = tomcat
    log.info ("Session Serializabler Checking Enabled")
  }

  @Override
  public void invoke(Request request, Response response) throws IOException, ServletException {
    try {
      getNext().invoke(request, response);
    } finally {
      storeSession(request, response);
    }
  }

  private void storeSession(Request request, Response response) throws IOException {
    if (request.getRequestedSessionId() == null) {
      log.debug("No Session requested, no serial check required")
      return;
    }
    Session session = request.getSessionInternal(false);

    if (session != null) {
      log.debug("Session created or used, checking contents for Serializable correctness")

      StandardSession standardSession = (StandardSession) session

      try {
        byte[] bytes = serialize(standardSession)
        log.info("Serialised, session size : ${bytes.size()} bytes")
        deSerialize(bytes)
        log.info("Deserialization successful, session conforms")
      } catch (Exception ex) {
        log.error("Serialization FAILED, session is not Serializable : ${ex.message}", ex)
        throw ex
      }
    } else {
      log.debug("No Session created, no serial check made")
    }
  }

  byte[] serialize(StandardSession session) {

    def attrs = Collections.list(session.attributeNames)

    log.debug("Session contains ${attrs.size()} attributes")

    ByteArrayOutputStream bos = new ByteArrayOutputStream()

    ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos))
    oos.writeInt(attrs.size())

    attrs.each {
      def obj = session.getAttribute(it)
      log.debug("Serialising : ${it} [${obj}]")
      oos.writeObject(obj)
    }

    oos.flush()

    return bos.toByteArray()
  }

  void deSerialize(byte[] bytes) {

    def manager = new StandardManager()
    manager.container = tomcat.connector.container

    def bis = new ByteArrayInputStream(bytes)

    def ois = new CustomObjectInputStream(bis, container.findChild("").loader.classLoader)

    int size = ois.readInt()

    log.debug("Contains ${size} serialised objects")

    for (int i = 0; i < size; i++) {
      def object = ois.readObject()
    }
  }
}
