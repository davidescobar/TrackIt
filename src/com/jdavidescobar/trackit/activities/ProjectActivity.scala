package com.jdavidescobar.trackit.activities

import android.app.Activity
import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.net.Uri
import android.os.{Bundle, Handler}
import android.util.Log
import android.view.{MenuItem, View}
import android.widget.{AdapterView, LinearLayout, ListView, ProgressBar, TextView}
import java.net.{HttpURLConnection, URL}
import java.util.ArrayList
import org.json.{JSONArray, JSONObject}
import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import scala.util.{Success, Failure}
import ExecutionContext.Implicits.global
import com.jdavidescobar.trackit.{Constants, R, TrackItApplication}
import com.jdavidescobar.trackit.TrackItApplication._
import com.jdavidescobar.trackit.data_adapters.StoriesArrayAdapter
import com.jdavidescobar.trackit.models.{Person, Project, Story}
import com.jdavidescobar.trackit.IntentFilterActions


class ProjectActivity extends Activity {
  override def onCreate(savedInstanceState:Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.project_activity)
    getActionBar.setDisplayHomeAsUpEnabled(true)
    
    project = getIntent.getSerializableExtra("project").asInstanceOf[Project]
    progressTextView.setText("Loading your stories...")
    progressLayout.setVisibility(View.VISIBLE)
    getProjectDetails
  }
  
  override def onOptionsItemSelected(item:MenuItem) = {
    item.getItemId() match {
      case android.R.id.home => {
        finish
        true
      }
      case _ => false
    }
  }
  
  override def onPause = {
    super.onPause
    firstVisibleStoryIndex = storiesListView.getFirstVisiblePosition
  }
    
  override def onActivityResult(requestCode:Int, resultCode:Int, intent:Intent) = {
    requestCode match {
      case ProjectActivity.STORY_DETAIL_REQUEST_CODE => {
        resultCode match {
          case Activity.RESULT_OK =>
            val oldStory = intent.getSerializableExtra("old_story").asInstanceOf[Story]
	        val updatedStory = intent.getSerializableExtra("updated_story").asInstanceOf[Story]
	        val story = storiesListView.getAdapter.asInstanceOf[StoriesArrayAdapter].getStories.find(_.id == oldStory.id)
	        story.foreach(_.name = updatedStory.name)
	        storiesListView.getAdapter.asInstanceOf[StoriesArrayAdapter].notifyDataSetChanged
	        TrackItApplication.showToast(this, s"Story ${updatedStory.id} updated.")
          case Activity.RESULT_CANCELED => ()
        }
      }
    }
  }
  
  private def getProjectDetails:Unit = {
    val detailsFuture = future {
      val urlStr = s"${Constants.PTBaseURL}/projects/${project.projectID}"
      TrackItApplication.makeHttpRequest(urlStr)
    }
    
    val membersFuture = detailsFuture.map { jsonStr =>
      Log.d("Project Details", jsonStr)
	  val json = new JSONObject(jsonStr)
	  if(json.length > 0) {
	    project.pointScale = "Unestimated" :: json.getString("point_scale").split(raw"\s*,\s*").toList
	    if(!json.isNull("description"))
	  	  project.description = json.getString("description")
	  }
      
      val urlStr = s"${Constants.PTBaseURL}/projects/${project.projectID}/memberships"
      TrackItApplication.makeHttpRequest(urlStr)
    }
    
    membersFuture.onComplete {
      case Success(jsonStr) =>
        Log.d("Members", jsonStr)
        val json = new JSONArray(jsonStr)
        for(i <- 0 until json.length) {
          val person = json.getJSONObject(i)
          val personDetails = person.getJSONObject("person")
          members.append(new Person(personDetails.getInt("id"), personDetails.getString("name"),
              personDetails.getString("username"), personDetails.getString("kind"),
              personDetails.getString("email"), personDetails.getString("initials")))
        }
        
	    runOnUiThread(() => {
	      myWorkTextView.setText(s"My Work - ${project.name.capitalize}")
	      storiesListView.setOnItemClickListener((parent:AdapterView[_], view:View, position:Int, id:Long) => {
		    val intent = new Intent(parent.getContext, classOf[StoryActivity])
		    intent.putExtra("story", parent.getItemAtPosition(position).asInstanceOf[Story])
		    intent.putExtra("project", project)
		    intent.putExtra("members", members)
		    startActivityForResult(intent, ProjectActivity.STORY_DETAIL_REQUEST_CODE)
	      })
	    })
	    getMyWorkStories
	    
      case Failure(error) => Log.d("ProjectActivity", error.getLocalizedMessage)
    }
  }

  private def getMyWorkStories:Unit = {
    val myWorkFuture:Future[String] = future {
      val urlStr = s"${Constants.PTBaseURL}/projects/${project.projectID}/stories"
      val params = Map("filter" -> "mywork:davidescobar")
      TrackItApplication.makeHttpRequest(urlStr, params)
    }
    
    myWorkFuture.onComplete {
      case Success(jsonStr) =>
        Log.d("Stories", jsonStr)
        stories.clear
        val json = new JSONArray(jsonStr)
        for(i <- 0 until json.length) {
    	  val story = json.getJSONObject(i)
    	  val description = if(story.has("description")) story.getString("description") else ""
    	  val points = if(story.has("estimate")) story.getInt("estimate") else -1
          stories.append(new Story(story.getInt("id"), story.getInt("project_id"), story.getString("story_type"),
            story.getString("current_state"), story.getString("name"), story.getString("url"), description,
            story.getInt("requested_by_id"), story.getInt("owned_by_id"), points))
        }
        runOnUiThread(() => {
          storiesAdapter = Some(new StoriesArrayAdapter(getBaseContext, android.R.layout.activity_list_item, stories))
          storiesListView.setAdapter(storiesAdapter.get)
		  progressLayout.setVisibility(View.GONE)
          
          // Restore the listview's previous scroll position if possible.
		  if(storiesListView.getCount > firstVisibleStoryIndex) {
		    storiesListView.setSelectionFromTop(firstVisibleStoryIndex, 0)
		  } else {
		    storiesListView.setSelectionFromTop(0, 0)
		  }
        })
      case Failure(error) => Log.d("ProjectActivity", error.getLocalizedMessage)
    }
  }
      

  private var project = new Project
  private var stories = new ArrayBuffer[Story]
  private var members = new ArrayBuffer[Person]
  private var storiesAdapter:Option[StoriesArrayAdapter] = None
  private var firstVisibleStoryIndex = 0
  
  // UI elements
  private lazy val progressLayout = findViewById(R.id.projectProgressLayout).asInstanceOf[LinearLayout]
  private lazy val progressTextView = progressLayout.findViewById(R.id.progressTextView).asInstanceOf[TextView]
  private lazy val myWorkTextView = findViewById(R.id.myWorkTextView).asInstanceOf[TextView]
  private lazy val storiesListView = findViewById(R.id.storiesListView).asInstanceOf[ListView]
}

object ProjectActivity {
  val STORY_DETAIL_REQUEST_CODE = 0
}
