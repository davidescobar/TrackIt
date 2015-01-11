package com.jdavidescobar.trackit.data_adapters

import android.content.{Context, Intent}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{ArrayAdapter, LinearLayout, TextView}
import collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import com.jdavidescobar.trackit.TrackItApplication._
import com.jdavidescobar.trackit.models.Project
import com.jdavidescobar.trackit.activities.ProjectActivity
import com.jdavidescobar.trackit.R


class ProjectsArrayAdapter(context:Context, resource:Int, projects:ArrayBuffer[Project])
	extends ArrayAdapter(context, resource, projects) {
  
  override def getView(position:Int, convertView:View, parent:ViewGroup) = {
    val newView =
      if(convertView == null)
        LayoutInflater.from(context).inflate(R.layout.project_item_partial, parent, false).asInstanceOf[LinearLayout]
      else
        convertView.asInstanceOf[LinearLayout]
    
    val project = getItem(position)
    val projectTextView = newView.findViewById(R.id.projectTextView).asInstanceOf[TextView]
    projectTextView.setText(project.name)
    newView.setOnClickListener((view:View) => navigateToProject(project))
    newView
  }
  
  private def navigateToProject(project:Project):Unit = {
    val intent = new Intent(context, classOf[ProjectActivity])
    intent.putExtra("project", project)
    context.startActivity(intent)
  }
}
