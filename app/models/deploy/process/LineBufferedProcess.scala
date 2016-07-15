package models.deploy.process

import com.sun.javafx.Logging


/**
  * Created by liangkai1 on 16/7/12.
  */
class LineBufferedProcess(process: Process) extends Logging {

  private[this] val _inputStream = new LineBufferedStream(process.getInputStream)
  private[this] val _errorStream = new LineBufferedStream(process.getErrorStream)

  def inputLines: IndexedSeq[String] = _inputStream.lines
  def errorLines: IndexedSeq[String] = _errorStream.lines

  def inputIterator: Iterator[String] = _inputStream.iterator
  def errorIterator: Iterator[String] = _errorStream.iterator

  def destroy(): Unit = {
    process.destroy()
  }

  def isAlive: Boolean = isProcessAlive(process)

  def exitValue(): Int = {
    process.exitValue()
  }

  def waitFor(): Int = {
    val returnCode = process.waitFor()
    _inputStream.waitUntilClose()
    _errorStream.waitUntilClose()
    returnCode
  }

  def isProcessAlive(process: Process): Boolean = {
    try {
      process.exitValue()
      false
    } catch {
      case _: IllegalThreadStateException =>
        true
    }
  }
}

