package com.jdavidescobar.trackit.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.{DialogInterface, Intent}
import android.os.Bundle
import android.util.Base64
import android.view.{Menu, View}
import android.widget.{Button, EditText, LinearLayout, TextView}
import java.net.URL
import org.json.JSONObject
import scala.concurrent._
import scala.util.{Success, Failure}
import ExecutionContext.Implicits.global
import com.jdavidescobar.trackit.TrackItApplication._
import com.jdavidescobar.trackit.{Constants, R, TrackItApplication}


class LoginActivity extends Activity {
  
  override def onCreate(savedInstanceState:Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.login_activity)
    progressLayout.setVisibility(View.GONE)
    progressTextView.setText("Logging in...")
    
    if(TrackItApplication.instance.apiToken.isEmpty) {
      loginButton.setOnClickListener((view:View) => {
	    progressLayout.setVisibility(View.VISIBLE)
	    onSignIn(loginEditText.getText.toString, passwordEditText.getText.toString)
	  })
    } else {
      val intent = new Intent(this, classOf[MainTrackerActivity])
      intent.putExtra("api_token", TrackItApplication.instance.apiToken)
      startActivity(intent)
    }
  }
  
  override def onCreateOptionsMenu(menu:Menu) = {
	// Inflate the menu; this adds items to the action bar if it is present.
	//getMenuInflater().inflate(R.menu.login, menu)
	true
  }
    
  private def onSignIn(username:String, password:String):Unit = {
    val signInFuture:Future[String] = future {
      val credentialsStr = Base64.encodeToString(s"$username:$password".getBytes, Base64.DEFAULT)
      val headers = Map("Authorization" -> credentialsStr)
      TrackItApplication.makeHttpRequest(s"${Constants.PTBaseURL}/me", headers=headers)
    }
    
    val errorMsg = getResources.getString(R.string.login_failed)
    signInFuture.onComplete {
      case Success(jsonStr) => {
        try {
          val json = new JSONObject(jsonStr)
          TrackItApplication.instance.apiToken = json.getString("api_token")
          if(TrackItApplication.instance.apiToken.isEmpty) {
            runOnUiThread(() => {
              passwordEditText.setText("")
	          passwordEditText.requestFocus
	          progressLayout.setVisibility(View.GONE)
	          val dialog = new AlertDialog.Builder(this)
	          				.setCancelable(false)
	           				.setMessage(errorMsg)
	            			.setPositiveButton("OK", (dlg:DialogInterface, which:Int) => dlg.dismiss)
	          dialog.show:Unit
            })
          } else {
            val activityIntent = new Intent(getBaseContext, classOf[MainTrackerActivity])
            activityIntent.putExtra("json", jsonStr)
            startActivity(activityIntent)
          }
        } catch {
          case e:Exception => runOnUiThread(() => {
            passwordEditText.setText("")
	        passwordEditText.requestFocus
	        progressLayout.setVisibility(View.GONE)
	        val dialog = new AlertDialog.Builder(this)
	          				.setCancelable(false)
	           				.setMessage(errorMsg)
	           				.setPositiveButton("OK", (dlg:DialogInterface, which:Int) => dlg.dismiss)
	        dialog.show:Unit
          })
        }
      }
      case Failure(error) => runOnUiThread(() =>
        TrackItApplication.showToast(this, error.getLocalizedMessage))
    }    
  }
  
  // UI elements
  private lazy val progressLayout = findViewById(R.id.loginProgressLayout).asInstanceOf[LinearLayout]
  private lazy val progressTextView = progressLayout.findViewById(R.id.progressTextView).asInstanceOf[TextView]
  private lazy val loginEditText = findViewById(R.id.loginEditText).asInstanceOf[EditText]
  private lazy val passwordEditText = findViewById(R.id.passwordEditText).asInstanceOf[EditText]
  private lazy val loginButton = findViewById(R.id.loginButton).asInstanceOf[Button]
}
