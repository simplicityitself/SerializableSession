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
import org.codehaus.groovy.grails.commons.ConfigurationHolder

public class SessionTrackerValve extends ValveBase {
  private static Logger log = LoggerFactory.getLogger(SessionTrackerValve);
  def tomcat

  SessionTrackerValve(def tomcat) {
    this.tomcat = tomcat
    log.info ("SessionTrackerValve: Checking to ensure all items placed in the session are serializable...")
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

      byte[] bytes
      def serializedOk = false
      def itemCount = 0
      try {
        def serializationInfo = serialize(standardSession)
        if (serializationInfo.errors) {
          Map errors = serializationInfo.errors
          def sb = new StringBuilder("Serialization FAILED, ${errors.keySet().size()} error(s) occurred while serializing the session for request '${request.requestURI}' with params ${request.parameterMap}:")
          errors.each { key, value ->
            sb << "\nField: $key, Unserializable Class: $value"
          }
          log.error(sb.toString())
          if (shouldThrowException()) {
              throw new NotSerializableException(errors.values().join(", "))
          }
          if (shouldExit()) {
            log.error("Error Detected in serialization, config is set to abort, killing application ....")
            Thread.sleep(30)
            System.exit(1)
          }
        } else {
          bytes = serializationInfo.bytes
          itemCount = serializationInfo.itemCount
          serializedOk = true
          log.info("Serialized, session size : ${bytes.size()} bytes")
        }
      }
      catch (Exception otherEx) {
        log.error("An unexpected error occured while attempting to serialize the session : ${otherEx.message}",otherEx)
        //not throwing as this suggests a bug in the plugin, rather than unserializable session.
      }

      if (serializedOk) {
        try {
          if (itemCount) deSerialize(bytes)
          log.info("Deserialization successful, session conforms")
        } catch (NotSerializableException ex) {
          log.error("Serialization FAILED, a serialization error occured while deserializing the session : ${ex.message}", ex)
        }
        catch (Exception otherEx) {
          log.error("An unexpected error occured while attempting to deserialize the session : ${otherEx.message}",otherEx)
        }
      }
    } else {
      log.debug("No Session created, no serial check made")
    }
  }

  def serialize(StandardSession session) {
    def errors = [:]
    def bytes = new byte[0]
    def attrs = Collections.list(session.attributeNames)

    log.debug("Session contains ${attrs.size()} attributes")

    if (attrs.size() > 0) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream()

        ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos))
        oos.writeInt(attrs.size())

        attrs.each {
          try {
            def obj = session.getAttribute(it)
            log.debug("Serializing : ${it} [${obj}]")
            oos.writeObject(obj)
          } catch (NotSerializableException ex) {
              errors[it] = ex.message
          }
        }

        oos.flush()
        bytes = bos.toByteArray()
    }

    return [bytes:bytes,errors:errors,itemCount:attrs.size()]
  }

  void deSerialize(byte[] bytes) {

    def manager = new StandardManager()
    manager.container = tomcat.connector.container

    def bis = new ByteArrayInputStream(bytes)

    def ois = new CustomObjectInputStream(bis, getClass().classLoader)

    int size = ois.readInt()

    log.debug("Contains ${size} serialized objects")

    for (int i = 0; i < size; i++) {
      ois.readObject()
    }
  }

  private Boolean shouldThrowException() {
    def config = ConfigurationHolder.config
    //If it isn't set, default it to true - groovy truth and the elvis operator fail here :-(
    if(!(config.serializableSessions.throwExceptionOnFailure instanceof Boolean)) {
      config.serializableSessions.throwExceptionOnFailure = true
    }
    return config.serializableSessions.throwExceptionOnFailure
  }


  private Boolean shouldExit() {
    def config = ConfigurationHolder.config
    //If it isn't set, default it to false
    if(!(config.serializableSessions.systemExitOnFailure instanceof Boolean)) {
      config.serializableSessions.systemExitOnFailure = false
    }
    return config.serializableSessions.systemExitOnFailure
  }
}
