package models

import java.io.File
import java.util.concurrent.Executors._

import akka.actor.{ActorRef, Cancellable, Props}
import controllers._
import models.actor.InstrumentedActor
import models.deploy._
import models.deploy.process.{LineBufferedProcess, SparkProcessBuilder}
import models.utils.Config
import org.apache.commons.lang.StringUtils
import org.apache.spark.SparkEnv
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.Files.TemporaryFile
import play.api.mvc.MultipartFormData.FilePart
import play.libs.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

/**
  * Created by liangkai1 on 16/7/11.
  */
case  class ExecuteModel(
                          master:String,
                          executeClass:String,
                          numExecutors:String,
                          driverMemory:String,
                          executorMemory:String,
                          total_executor_cores:String,
                          jarLocation:String,
                          args1:String)

object  JobManagerActor{
  case class SubmitJob(request: CreateBatchRequest)
  case class SubmitJobed(msg:String)
  case class StoreJar(userName: String, filePart: FilePart[TemporaryFile])

  case class Initializ(executeModel: ExecuteModel)
  case class InitError(t: Throwable)
  case class JobFinish(msg: String)
  case class SaveTask(appId:String)


  case class InvalidJar(error:String)
  case class JarStored(uri :String)

  def props(config:Config,jobDAO: JobDAO,taskDao: TaskDao): Props = Props(classOf[JobManagerActor], config,jobDAO,taskDao)
}

/**
  * [[models.JobDAO]]
  *
  * @param jobDAO
  */
private class JobManagerActor(config:Config,jobDAO:JobDAO,taskDao: TaskDao) extends InstrumentedActor{


  import JobManagerActor._

  //val config = mutable.HashMap.empty[String,String]
  val executionContext = ExecutionContext.fromExecutorService(newFixedThreadPool(config.getInt("task.pool.max")))

  private val push_akka ="/user/MessagePool"
  private val yarn="yarn-cluster"
  private val uri ="spark"
  private val YARN = 1
  private val STANDALONE = 2
  private val MESOS = 4
  private val LOCAL = 8



  /**
    * jar 文件验证
    *
    * @param fileName
    * @return
    */
  def validateJar(fileName:String): Boolean ={
    val prefix=fileName.substring(fileName.lastIndexOf(".")+1);
    StringUtils.equals(prefix,"jar")
  }

  /**
    * 完整校验
    *
    * @param jarBytes
    * @return
    */
  def validateJarBytes(jarBytes: Array[Byte]): Boolean = {
    jarBytes.size > 4 && jarBytes(0) == 0x50 && jarBytes(1) == 0x4b && jarBytes(2) == 0x03 && jarBytes(3) == 0x04
  }

  def sparkSubmit(): String = {
    sparkHome.map { _ + "bin" + File.separator + "spark-submit" }.get
  }

  def sparkHome(): Option[String] = Some(config.getString("spark.home"))

  def masterHost():Option[String] =Some(config.getString("spark.master.url"))

  def sparkMaster():String={
    masterHost
      .map{
         uri +":"+File.separator+
          File.separator+ _
      }.get
  }


  def Sealing:PartialFunction[String,State] ={
    case m if(m.equals("RUNNING")) => RUNNING
    case m if(m.equals("FINISHED")) => FINISHED
    case m if(m.equals("KILLED")) => KILLED
    case m if(m.equals("FAILED")) => FAILED
    case _  => FINISHED

  }

  def wrappedReceive: Receive = {

    case Initializ(executeModel) => {
      val request: CreateBatchRequest = CreateBatchRequest()
      request.className=Some(executeModel.executeClass)
      request.numExecutors=Some(executeModel.numExecutors)
      request.total_executor_cores=Some(executeModel.total_executor_cores)
      request.driverMemory=Some(executeModel.driverMemory)
      request.executorMemory=Some(executeModel.executorMemory)
      request.jarLocation = Some(executeModel.jarLocation)

      val masters: Int = executeModel.master match {
        case m if m.startsWith("yarn") =>   request.master=Some(yarn); YARN
        case m if m.startsWith("standalone") => request.master=Some(sparkMaster()); STANDALONE
        case m if m.startsWith("mesos") => MESOS
        case m if m.startsWith("local") => request.master=Some(executeModel.master);LOCAL
      }
      Logger.info(s"select mode $masters")
      val args: Array[String] = executeModel.args1.split("\\s+")
      request.args=args.toList
      sender ! request
    }


    case SubmitJob(request) => {
      runJobFuture(request, sparkEnv =SparkEnv.get,sender)
    }

    case StoreJar(userName,filePart) => {
      if(!validateJar(filePart.filename)){
        sender ! InvalidJar("文件类型不正确!")
      }else{
        val jar = jobDAO.saveJar(userName,DateTime.now(),filePart)
        if(jar.nonEmpty){
          sender ! JarStored(jar)
        }else{
          sender ! InvalidJar("文件路径保存失败!")
        }
      }
    }

    case JobFinish(appId) => {
      if(appId.startsWith("application")){
         val user: String = taskDao.findyarnTaskUser(appId)
         val act = Akka.system.actorSelection(push_akka+user)
         taskDao.queryYarnState(appId).map{
          info =>
            Message.addMessage(TaskMessage(info.application_id,info.state,user))
            act ! Sealing(info.state) ;
            Logger.info(s"任务结束,当前状态==>"+info.state);
        }
      }else {
        val user: String = taskDao.findTaskUser(appId)
        val act = Akka.system.actorSelection(push_akka+user)
        taskDao.queryState(appId).map{
          info =>
            Message.addMessage(TaskMessage(info.app_id,info.state,user));
            act ! Sealing(info.state);Logger.info(s"任务结束,当前状态==>"+info.state);
        }
      }
      }

  }


  /**
    * 任务部署
    *
    * @param request
    */
  def runJobFuture(request:CreateBatchRequest, sparkEnv: SparkEnv,act :ActorRef): Future[Any] = {
    logger.info("Starting job")
    Future {
      try {
        SparkEnv.set(sparkEnv)
        val builder = new SparkProcessBuilder(act)
        request.className.foreach(builder.className)
        request.driverMemory.foreach(builder.driverMemory)
        request.executorMemory.foreach(builder.executorMemory)
        request.total_executor_cores.foreach(builder.executorCores)
        request.numExecutors.foreach(builder.numExecutors)
        request.jarLocation.foreach(builder.jarLocation)
        request.master.foreach(builder.master)
        val process: LineBufferedProcess = builder.start(Some(sparkSubmit), request.args)
        val output = process.inputIterator.mkString("\n")
        //val regex = """Shutdown (.*)""".r.unanchored
        //val regex = """Shutdown hook called(.*)""".r.unanchored
        val regex: Regex = MatchEngine.matchMode(request.master.get)
        output match {
          case regex(appId) => {
              JobRunFinish(appId)
          }
          case _ =>
            throw new JobRunExecption(output)
        }
      } catch {
        case e: AbstractMethodError => {
          logger.error("AbstractMethodError")
          throw e
        }
        case e: Throwable => {
          logger.error("Got Throwable", e)
          throw new JobRunExecption(e.getMessage)
        }
      }
    }(executionContext).andThen {
      case scala.util.Success(result:Any) =>  Logger.info("执行结束");  context.system.scheduler.scheduleOnce(7 seconds,self ,JobFinish(result.msg))
      case scala.util.Failure(error :Throwable) => Logger.info("执行异常");act ! JobRunExecption(error.getMessage)
    }
  }




}
