package com.jdavidescobar.trackit.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.{Editable, TextWatcher}
import android.util.Log
import android.view.{MenuItem, View}
import android.widget.{AdapterView, ArrayAdapter, Button, EditText, Spinner, TextView}
import android.widget.AdapterView.OnItemSelectedListener
import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL}
import org.json.JSONObject
import scala.collection.mutable.ArrayBuffer
import scala.concurrent._
import scala.util.{Success, Failure}
import ExecutionContext.Implicits.global
import com.jdavidescobar.trackit.{Constants, R}
import com.jdavidescobar.trackit.models.{Person, Project, Story}
import com.jdavidescobar.trackit.{IntentFilterActions, TrackItApplication}
import com.jdavidescobar.trackit.TrackItApplication._


class StoryActivity extends Activity {

  override def onCreate(savedInstanceState:Bundle) = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.story_activity)
    getActionBar.setDisplayHomeAsUpEnabled(true)
    
    story = getIntent.getSerializableExtra("story").asInstanceOf[Story]
    storyIDTextView.setText(s"ID: ${story.id}")
    nameEditText.setText(story.name)
    project = getIntent.getSerializableExtra("project").asInstanceOf[Project]
    members = getIntent.getSerializableExtra("members").asInstanceOf[ArrayBuffer[Person]]
    populateStoryTypes
    if(story.storyType != "bug") {
      populateStoryPoints
    } else {
      pointsTextView.setVisibility(View.GONE)
      pointsSpinner.setVisibility(View.GONE)
    }
    populateStates
    populateMembers
    
    cancelButton.setOnClickListener((view:View) => {
      setResult(Activity.RESULT_CANCELED, new Intent)
      finish
    })
    saveButton.setOnClickListener((view:View) => {
      val updatedStoryFuture:Future[String] = future {
        val urlStr = s"${Constants.PTBaseURL}/stories/${story.id}"
        var params = Map("name" -> story.name,
        				 "story_type" -> story.storyType,
        				 "current_state" -> story.currentState)
        if(story.points != -1) params += ("estimate" -> story.points.toString)
        TrackItApplication.makeHttpRequest(urlStr, params, "PUT")
      }
      
      updatedStoryFuture.onComplete {
        case Success(jsonResponse) =>
          Log.d("Updated Story", jsonResponse)
          val json = new JSONObject(jsonResponse)
          val description = if(json.has("description")) json.getString("description") else ""
          val updatedStory = new Story(json.getInt("id"), json.getInt("project_id"),
            json.getString("story_type"), json.getString("current_state"),
            json.getString("name"), json.getString("url"), description,
            json.getInt("requested_by_id"), json.getInt("owned_by_id"))
          runOnUiThread(() => {
	        val intent = new Intent
	        intent.putExtra("old_story", story)
	        intent.putExtra("updated_story", updatedStory)
	        setResult(Activity.RESULT_OK, intent)
            finish
          })
        case Failure(error) =>
          Log.d("StoryActivity", error.getLocalizedMessage)
          TrackItApplication.showToast(this, "Something went wrong. Could not update this story.")
      }
    })
    
    nameEditText.addTextChangedListener(new TextWatcher() {
      override def beforeTextChanged(s:CharSequence, start:Int, count:Int, after:Int) = Unit
      override def onTextChanged(s:CharSequence, start:Int, before:Int, count:Int) = Unit
      override def afterTextChanged(s:Editable) = {
        story.name = s.toString
      }
    })
    
    storyTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      override def onItemSelected(parent:AdapterView[_], view:View, position:Int, id:Long) = {
        val storyType = storyTypeSpinner.getSelectedItem.asInstanceOf[String].toLowerCase
        story.storyType = storyType
        storyType match {
          case "feature" =>
            story.storyType = storyType 
            val pointsIndex = pointsSpinner.getAdapter.asInstanceOf[ArrayAdapter[Int]].getPosition(story.points)
            pointsSpinner.setSelection(pointsIndex)
            List(pointsTextView, pointsSpinner).foreach(_.setVisibility(View.VISIBLE))
          case _ =>
            List(pointsTextView, pointsSpinner).foreach(_.setVisibility(View.GONE))
            pointsSpinner.setSelection(0) // Set points to "Unestimated".
        }
      }
      
      override def onNothingSelected(parent:AdapterView[_]) = {
        List(pointsTextView, pointsSpinner).foreach(_.setVisibility(View.GONE))
        pointsSpinner.setSelection(0)  // Set points to "Unestimated".
      }
    })
    
    pointsSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      override def onItemSelected(parent:AdapterView[_], view:View, position:Int, id:Long) = {
        story.points = if(position == 0) -1 else project.pointScale(position).toInt
      }
      
      override def onNothingSelected(parent:AdapterView[_]) = {
        story.points = -1
      }
    })
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
  
  private def populateStoryTypes = {
    val storyTypes = Array("Feature", "Bug", "Chore", "Release")
    val storyTypeAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, storyTypes)
    storyTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    storyTypeSpinner.setAdapter(storyTypeAdapter)
    val storyTypeIndex = storyTypes.indexWhere(_.toLowerCase == story.storyType.toLowerCase)
    if(storyTypeIndex != -1) storyTypeSpinner.setSelection(storyTypeIndex)
  }
    
  private def populateStoryPoints = {
    val pointsArrayAdapter = new ArrayAdapter(this,
      android.R.layout.simple_spinner_item, project.pointScale.toArray)
    pointsArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    pointsSpinner.setAdapter(pointsArrayAdapter)
    val pointsIndex = project.pointScale.indexWhere(_ == story.points.toString)
    pointsSpinner.setSelection(if(pointsIndex != -1) pointsIndex else 0)
  }
  
  private def populateStates = {
    val states = Array("Unstarted", "Started", "Finished", "Delivered", "Rejected", "Accepted")
    val statesArrayAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, states)
    statesArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    statesSpinner.setAdapter(statesArrayAdapter)
    val currentStateIndex = states.indexWhere(_.toLowerCase == story.currentState.toLowerCase)
    if(currentStateIndex != -1) statesSpinner.setSelection(currentStateIndex)
  }
  
  private def populateMembers = {
    val names = members.map(_.name).toArray
    
    val requestersArrayAdapter = new ArrayAdapter(this,
      android.R.layout.simple_spinner_item, names)
    requestersArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    requestersSpinner.setAdapter(requestersArrayAdapter)
    val requesterIndex = members.indexWhere(_.id == story.requestedByID)
    if(requesterIndex != -1) requestersSpinner.setSelection(requesterIndex)
    
    val ownersArrayAdapter = new ArrayAdapter(this,
      android.R.layout.simple_spinner_item, names)
    ownersArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    ownersSpinner.setAdapter(ownersArrayAdapter)
    val ownerIndex = members.indexWhere(_.id == story.ownedByID)
    if(ownerIndex != -1) ownersSpinner.setSelection(ownerIndex)
  }
    

  private var story:Story = new Story
  private var project = new Project
  private var members = new ArrayBuffer[Person]
  private var owners = new ArrayBuffer[Person]
  
  // UI elements
  private lazy val storyIDTextView = findViewById(R.id.storyIDTextView).asInstanceOf[TextView]
  private lazy val nameEditText = findViewById(R.id.nameEditText).asInstanceOf[EditText]
  private lazy val cancelButton = findViewById(R.id.cancelButton).asInstanceOf[Button]
  private lazy val saveButton = findViewById(R.id.saveButton).asInstanceOf[Button]
  private lazy val storyTypeSpinner = findViewById(R.id.storyTypeSpinner).asInstanceOf[Spinner]
  private lazy val pointsTextView = findViewById(R.id.pointsTextView).asInstanceOf[TextView]
  private lazy val pointsSpinner = findViewById(R.id.pointsSpinner).asInstanceOf[Spinner]
  private lazy val statesSpinner = findViewById(R.id.statesSpinner).asInstanceOf[Spinner]
  private lazy val requestersSpinner = findViewById(R.id.requesterSpinner).asInstanceOf[Spinner]
  private lazy val ownersSpinner = findViewById(R.id.ownerSpinner).asInstanceOf[Spinner]
}
