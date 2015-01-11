package com.jdavidescobar.trackit.models

class Project(var id:Int = -1, var projectID:Int = -1, var name:String = "",
    var pointScale:List[String] = List.empty, var description:String = "") extends Serializable
    