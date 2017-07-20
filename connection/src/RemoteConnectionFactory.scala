package priceprobe.connection

import java.io.File
import java.util.Properties
import com.jcraft.jsch.{ChannelExec, JSch, Session}
import com.typesafe.config.{Config, ConfigFactory}

/**
  * Created by andream16 on 27.06.17.
  */
class RemoteConnectionFactory {
  val configFile = new File("./config/application.conf")
  val fileConfig: Config = ConfigFactory.parseFile(configFile)
  val config: Config = ConfigFactory.load(fileConfig)
  val host: String = getConf("server", "api")
  val port: Int = getConf("server", "port").toInt
  val sshUrl: String = getConf("credentials", "ssh-url")
  val sshPort: Int = getConf("credentials", "ssh-port").toInt
  val cassandraPort: Int = getConf("credentials", "cassandra-port").toInt
  val forwardPort: Int = getConf("credentials", "forward-port").toInt
  val sshUserName: String = getConf("credentials", "user-name")
  val sshPemPath: String = getConf("credentials", "pem-path")
  val sshTimeout : Int = getConf("credentials", "timeout").toInt

  var session : Session = _
  var channel : ChannelExec = _

  def initRemoteConnection(): Unit = {
    //Connect remotely via SSH
    val jsch: JSch = new JSch()
    jsch.addIdentity(sshPemPath)
    session = jsch.getSession(sshUserName, sshUrl, sshPort)
    val config = new Properties()
    config.put("StrictHostKeyChecking", "no")
    session.setTimeout(sshTimeout)
    session.setConfig(config)
    session.connect()

    //Prepare Execution
    channel = session.openChannel("exec").asInstanceOf[ChannelExec]
    channel.connect()
    session.setPortForwardingL(forwardPort, host, cassandraPort)

  }

  def getRemoteConnection: (ChannelExec, Session) = {
    (channel, session)
  }

  def disconnectFromRemoteConnection(): Unit = {
    try {
      channel.disconnect()
    } catch {
      case e : Exception => e.printStackTrace(); None
      case _ : Throwable => None
    } finally {
      try {
        session.disconnect()
      } catch {
        case e : Exception => e.printStackTrace(); None
        case _ : Throwable => None
      } finally {
        None
      }
    }
  }

  def getConf(group: String, key: String): String = {
    config.getString("config." + group + "." + key + ".value")
  }

}