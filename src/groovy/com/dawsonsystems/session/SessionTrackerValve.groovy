package com.dawsonsystems.session

import org.apache.catalina.Session
import org.apache.catalina.connector.Request
import org.apache.catalina.connector.Response
import org.apache.catalina.session.StandardSession
import org.apache.catalina.startup.Tomcat
import org.apache.catalina.valves.ValveBase
import org.apache.catalina.util.CustomObjectInputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.servlet.ServletException

import static grails.util.Holders.getConfig

class SessionTrackerValve extends ValveBase {

	private static Logger log = LoggerFactory.getLogger(SessionTrackerValve)

	Tomcat tomcat

	SessionTrackerValve(def tomcat) {
		this.tomcat = tomcat
		log.info("SessionTrackerValve: Checking to ensure all items placed in the session are serializable...")
	}

	@Override
	void invoke(Request request, Response response) throws IOException, ServletException {
		try {
			getNext().invoke(request, response)
		} finally {
			storeSession(request, response)
		}
	}

	private void storeSession(Request request, Response response) throws IOException {
		if (request.getRequestedSessionId() == null) {
			log.debug("No Session requested, no serial check required")
			return
		}
		Session session = request.getSessionInternal(false)

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
				} else {
					bytes = serializationInfo.bytes
					itemCount = serializationInfo.itemCount
					serializedOk = true

					log.info("Serialized, session size : ${bytes.size()} bytes")
				}
			} catch (Exception otherEx) {
				log.error("An unexpected error occured while attempting to serialize the session : ${otherEx.message}", otherEx)
				// not throwing as this suggests a bug in the plugin, rather than unserializable session
			}

			if (serializedOk) {
				try {
					if (itemCount) deSerialize(bytes)
					log.info("Deserialization successful, session conforms")
				} catch (NotSerializableException ex) {
					log.error("Serialization FAILED, a serialization error occured while deserializing the session : ${ex.message}", ex)
				} catch (Exception otherEx) {
					log.error("An unexpected error occured while attempting to deserialize the session : ${otherEx.message}", otherEx)
				}
			}
			if (shouldReplaceSession()) {
				replaceSessionContents(standardSession)
				log.info("Replaced session contents with version passed through de/serialized process")
			} else {
				log.info("No Replacement...")
			}
		} else {
			log.debug("No Session created, no serial check made")
		}
	}

	private serialize(StandardSession session) {
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

		return [bytes: bytes, errors: errors, itemCount: attrs.size()]
	}

	private void deSerialize(byte[] bytes) {
		def bis = new ByteArrayInputStream(bytes)
		def ois = new CustomObjectInputStream(bis, getClass().classLoader)

		int size = ois.readInt()

		log.debug("Contains ${size} serialized objects")

		for (int i = 0; i < size; i++) {
			ois.readObject()
		}
	}

	private replaceSessionContents(StandardSession standardSession) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream()
		ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(bos))

		oos.writeLong(standardSession.getCreationTime())
		standardSession.writeObjectData(oos)
		oos.close()

		byte[] data = bos.toByteArray()

		BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(data))
		ObjectInputStream ois = new CustomObjectInputStream(bis, getClass().classLoader)

		standardSession.setCreationTime(ois.readLong())
		standardSession.readObjectData(ois)
	}

	private Boolean shouldReplaceSession() {
		if (!(config.serializableSessions.replaceSession instanceof Boolean)) {
			return true
		}
		return config.serializableSessions.replaceSession
	}

	private Boolean shouldThrowException() {
		if (!(config.serializableSessions.throwExceptionOnFailure instanceof Boolean)) {
			return true
		}
		return config.serializableSessions.throwExceptionOnFailure
	}
}
