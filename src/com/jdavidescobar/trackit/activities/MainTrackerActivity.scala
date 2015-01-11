package com.jdavidescobar.trackit.activities

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.{LinearLayout, ListView, TextView}
import java.net.URL
import org.json.JSONObject
import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.jdavidescobar.trackit.TrackItApplication._
import com.jdavidescobar.trackit.models.Project
import com.jdavidescobar.trackit.data_adapters.ProjectsArrayAdapter
import com.jdavidescobar.trackit.{Constants, R, TrackItApplication}


class MainTrackerActivity extends Activity {
  
  override def onCreate(savedInstanceState:Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_tracker_activity)
    progressTextView.setText("Loading your projects...")
    mainTrackerProgressLayout.setVisibility(View.VISIBLE)
    
    if(getIntent.hasExtra("json")) {
	  val json = new JSONObject(getIntent.getStringExtra("json"))
	  apiToken = json.getString("api_token")
	  val projects = json.getJSONArray("projects")
	  for {
		i <- 0 until projects.length
	    project = projects.getJSONObject(i)
	  } this.projects.append(new Project(project.getInt("id"), project.getInt("project_id"), project.getString("project_name")))
	  projectsAdapter = Some(new ProjectsArrayAdapter(this, android.R.layout.activity_list_item, this.projects))
	  projectsListView.setAdapter(projectsAdapter.get)
	  mainTrackerProgressLayout.setVisibility(View.GONE)
    } else if(!TrackItApplication.instance.apiToken.isEmpty) {
      val projects:Future[String] = future {
        TrackItApplication.makeHttpRequest(s"${Constants.PTBaseURL}/me")
      }
      
      projects.onSuccess {
        case jsonStr => {
          Log.d("MainTrackerActivity", jsonStr)
          val json = new JSONObject(jsonStr)
          val projects = json.getJSONArray("projects")
          for {
            i <- 0 until projects.length
            project = projects.getJSONObject(i)
          } this.projects.append(new Project(project.getInt("id"), project.getInt("project_id"), project.getString("project_name")))
          runOnUiThread(() => {
        	projectsAdapter = Some(new ProjectsArrayAdapter(this, android.R.layout.activity_list_item, this.projects))
            projectsListView.setAdapter(projectsAdapter.get)
            mainTrackerProgressLayout.setVisibility(View.GONE)
          })
        }
      }
    }
  }
  
  private var apiToken = ""
  private var projects:ArrayBuffer[Project] = new ArrayBuffer()
  private var projectsAdapter:Option[ProjectsArrayAdapter] = None
  
  private lazy val mainTrackerProgressLayout = findViewById(R.id.mainTrackerProgressLayout).asInstanceOf[LinearLayout]
  private lazy val progressTextView = findViewById(R.id.progressTextView).asInstanceOf[TextView]
  private lazy val projectsListView = findViewById(R.id.projectsListView).asInstanceOf[ListView]
}
