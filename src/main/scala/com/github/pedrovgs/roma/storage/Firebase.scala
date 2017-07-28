package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.config.ConfigLoader
import com.google.firebase.database.{DatabaseReference, FirebaseDatabase}
import com.google.firebase.{FirebaseApp, FirebaseOptions}

private[this] object Firebase {

  private val credentials = getClass.getResourceAsStream("/firebaseCredentials.json")
  private val options = {
    val config = ConfigLoader.loadFirebaseConfig()
    val databaseUrl = config.map(_.databaseUrl).getOrElse("")
    new FirebaseOptions.Builder()
      .setDatabaseUrl(databaseUrl)
      .setServiceAccount(credentials)
      .build()
  }

  FirebaseApp.initializeApp(options)
  private val database = FirebaseDatabase.getInstance()

  def ref(path: String): DatabaseReference = database.getReference(path)
}
