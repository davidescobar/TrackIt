package com.jdavidescobar.trackit.models

class Story(var id:Int = -1, var projectID:Int = -1, var storyType:String = "",
    		var currentState:String = "", var name:String = "",
    		val url:String = "", var description:String = "",
    		var requestedByID:Int = -1, var ownedByID:Int = -1, var points:Int = -1) extends Serializable
