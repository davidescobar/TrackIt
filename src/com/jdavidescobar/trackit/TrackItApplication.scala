package com.jdavidescobar.trackit

import android.app.Application
import android.content.{BroadcastReceiver, Context, DialogInterface, Intent, SharedPreferences}
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.{AdapterView, Toast}
import java.io.{BufferedReader, InputStream, InputStreamReader, OutputStreamWriter}
import java.net.{HttpURLConnection, URL}
import scala.annotation.tailrec


class TrackItApplication extends Application {
  override def onCreate {
    super.onCreate
    TrackItApplication.appInstance = Some(this)
    sharedPreferences = Some(getApplicationContext.getSharedPreferences("TrackItPreferences", Context.MODE_PRIVATE))
  }
  
  private var sharedPreferences:Option[SharedPreferences] = None
  
  private var _apiToken = ""
  def apiToken = _apiToken
  def apiToken_=(_apiToken:String) = {
    val editor:SharedPreferences.Editor = sharedPreferences.get.edit
    editor.putString("api_token", _apiToken)
    editor.commit
    this._apiToken = _apiToken
  }
}

object TrackItApplication {
  def showToast(context:Context, message:String, duration:Int = Toast.LENGTH_LONG) = {
	Toast.makeText(context, message, duration).show
  }
  
  def makeHttpRequest(baseURL:String, params:Map[String, String]=Map.empty,
		  method:String="GET", headers:Map[String, String]=Map.empty) = {
    val paramsStr =
      if(params.isEmpty) ""
      else params.map { case(key, value) => s"$key=${Uri.encode(value)}" }.mkString("&")
    val urlStr = if(method.trim.toUpperCase == "GET") s"$baseURL?$paramsStr" else baseURL
    val url = new URL(urlStr)
    val connection = url.openConnection.asInstanceOf[HttpURLConnection]
    headers.foreach { case(key, value) => connection.setRequestProperty(key, value) }
    if(!TrackItApplication.instance.apiToken.isEmpty) {
      connection.setRequestProperty("X-TrackerToken", TrackItApplication.instance.apiToken)
    }
    method.trim.toUpperCase match {
      case "POST" | "PUT" | "PATCH" | "DELETE" =>
        connection.setRequestMethod(method.trim.toUpperCase)
        connection.setDoOutput(true)
        val outputStream = new OutputStreamWriter(connection.getOutputStream)
        outputStream.write(paramsStr)
        outputStream.close
      case _ => ()
    }
      
    try {
      TrackItApplication.readStream(connection.getInputStream)
    } catch {
      case e:Exception =>
        Log.d("TrackItApplication", e.getLocalizedMessage)
        ""
    }
  }
  
  def readStream(inStream:InputStream) = {
    @tailrec
    def buildResponseString(reader:BufferedReader, sb:StringBuilder):String = {
      val line = reader.readLine
      line match {
        case null =>
          if(reader != null) reader.close
          sb.toString
        case _ => buildResponseString(reader, sb.append(line))
      }
    }
    
    val reader = new BufferedReader(new InputStreamReader(inStream))
    buildResponseString(reader, new StringBuilder)
  }
  
  def addParamsToBaseURL(baseURL:String, params:Map[String, String]) = {
    if(params.isEmpty) {
      if(baseURL == null) "" else baseURL
    } else {
      val paramsStr = params.map { case(name, value) => s"$name=${Uri.encode(value)}" }.mkString("&")
      s"$baseURL?$paramsStr"
    }
  }
  
  // Implicit conversions
  implicit def onClickToOnClickListener(view:View => Unit) = {
	new OnClickListener() {
	  override def onClick(source:View) = view(source)
	}
  }
  
  implicit def onReceiveToOnBroadcastReceiverOnReceive(func:(Context, Intent) => Unit) = {
    new BroadcastReceiver() {
      override def onReceive(context:Context, intent:Intent) = func(context, intent)
    }
  }
  
  implicit def onItemClickToOnItemClickListener(func:(AdapterView[_], View, Int, Long) => Unit) = {
    new AdapterView.OnItemClickListener() {
      override def onItemClick(parent:AdapterView[_], view:View, position:Int, id:Long) = {
        func(parent, view, position, id)
      }
    }
  }
    
  implicit def setAlertDialogButton(func:(DialogInterface, Int) => Unit) = {
    new DialogInterface.OnClickListener() {
      override def onClick(dialog:DialogInterface, which:Int) = func(dialog, which)
    }
  }

  implicit def functionToRunnable(func:() => Unit) = {
	new Runnable() { override def run = func() }
  }
  
  private var appInstance:Option[TrackItApplication] = None
  def instance = appInstance.get
}
