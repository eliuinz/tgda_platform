package com.github.tgda.dataAnalyze.feature.transformation.messagePayload

trait RemoteMessage extends Serializable
//worker -->master
case class RegisterWorker(id:String,memory:Int,cores:Int) extends RemoteMessage
//master--->worker
case class RegisteredWorker(masterUrl:String) extends RemoteMessage
//worker---master
case class HeartBeat(id:String) extends RemoteMessage

case class AnalyzeTreesCrownAreaInSection(crownSize:Int,treeType:String)
