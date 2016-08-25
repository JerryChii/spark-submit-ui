package models

/**
  * Created by king on 16/8/22.
  */
trait TaskProvider[T] {

  def findTaskInfo(app:T)

  def proTaskOnMaster(app:T)

  def proTaskOnYarn(app:T)



}
