package controllers


import play.api.Play.current
import models.JobManagerActor.{InvalidJar, JarStored}
import models._
import play.api.Logger
import play.api.cache.Cache
import play.api.data.Forms._
import play.api.data._
import play.api.mvc._


object SparkJar extends Controller with Secured {

  val executeForm:Form[ExecuteModel] = Form{
    mapping (
      "executeClass"->text,
      "numExecutors"->text,
      "driverMemory"->text,
      "executorMemory"->text,
      "executorCores"->text,
      "jarLocation"->text,
      "args1"->text
      )(ExecuteModel.apply)(ExecuteModel.unapply)
    }


    def upload = Action(parse.multipartFormData) { implicit request =>
      request.body.file("file").map { jobFile =>
         session.get("email").map { user =>
           Logger.info("用户名=>"+user)
           Execute.storeJar(user,jobFile) match {
            case JarStored(uri) => Redirect(routes.SparkJar.executejarpage())
            case InvalidJar(error) => Logger.info(error); BadRequest(error)
            case _ => NotFound
          }
        }.getOrElse {
          Unauthorized("用户不存在,请重新登录!")
        }
        }.getOrElse {
          Redirect(routes.SparkJar.errorpage())
        }
      }

      def executejarpage = Action { implicit request =>
        Ok(views.html.upload(executeForm))
      }

      def executejar = IsAuthenticated { username => implicit request =>
        executeForm.bindFromRequest.fold(
          formWithErrors => {
            formWithErrors.errors.map(x => Logger.info(x.message))
            formWithErrors.globalError.map(x => Logger.info(x.message))
            BadRequest(views.html.error(formWithErrors.toString))
          },
          executeArguments => {
          Execute.main(executeArguments)
          match {
              case JobSubmitSuccess(msg) => Logger.info(msg); Redirect(routes.SparkJar.he(msg))
              case JobRunExecption(error) => BadRequest(error)
              case _ => NotFound
            }
       }
     )
}

    def errorpage = IsAuthenticated {username => implicit request =>
      Ok(views.html.index())
    }

    def logs(id:String) =Action{
      Ok(Cache.getAs[String](id).getOrElse("none"))
    }

    def he(jobId:String)=Action{
      Ok(views.html.he(jobId))
    }



 }






