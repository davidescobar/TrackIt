package com.jdavidescobar.trackit.data_adapters

import android.app.Activity
import android.content.{Context, Intent}
import android.util.Log
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ArrayAdapter, ImageView, LinearLayout, TextView}
import collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import com.jdavidescobar.trackit.TrackItApplication._
import com.jdavidescobar.trackit.activities.StoryActivity
import com.jdavidescobar.trackit.R
import com.jdavidescobar.trackit.models.Story


class StoriesArrayAdapter(context:Context, resource:Int, var stories:ArrayBuffer[Story])
	extends ArrayAdapter(context, resource, stories) {
  
  override def getView(position:Int, convertView:View, parent:ViewGroup) = {
    val newView =
      if(convertView == null)
        LayoutInflater.from(context).inflate(R.layout.story_item_partial, parent, false).asInstanceOf[LinearLayout]
      else
        convertView.asInstanceOf[LinearLayout]

    val story = getItem(position)
    val storyTypeImageView = newView.findViewById(R.id.storyTypeImageView).asInstanceOf[ImageView]
    val imgRes = story.storyType match {
      case "feature" => R.drawable.star_gold_256
      case "bug" => R.drawable.bug
      case _ => -1
    }
    storyTypeImageView.setBackgroundResource(imgRes)
    newView.findViewById(R.id.nameTextView).asInstanceOf[TextView].setText(story.name)
    newView.setTag(story)
    newView
  }
  
  def getStories = stories
}
