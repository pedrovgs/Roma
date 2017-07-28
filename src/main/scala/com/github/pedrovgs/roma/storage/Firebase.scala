package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.FirebaseError
import com.github.pedrovgs.roma.config.ConfigLoader
import com.google.firebase.database.{DatabaseError, DatabaseReference, FirebaseDatabase}
import com.google.firebase.{FirebaseApp, FirebaseOptions}

import scala.concurrent.{Future, Promise}

class Firebase {

  private val credentials = getClass.getResourceAsStream("/firebaseCredentials.json")
  private val options = {
    val config = ConfigLoader.loadFirebaseConfig()
    val databaseUrl = config.map(_.databaseUrl).getOrElse("")
    new FirebaseOptions.Builder()
      .setDatabaseUrl(databaseUrl)
      .setServiceAccount(credentials)
      .build()
  }

  def save[T](path: String, value: T): Future[T] = {
    val promise = Promise[T]
    ref(path).setValue(value, new DatabaseReference.CompletionListener {
      override def onComplete(databaseError: DatabaseError, databaseReference: DatabaseReference): Unit = {
        if (databaseError == null) {
          promise.trySuccess(value)
        } else {
          promise.failure(new FirebaseError)
        }
      }
    })
    promise.future
  }

  FirebaseApp.initializeApp(options)
  private val database = FirebaseDatabase.getInstance()

  private def ref(path: String): DatabaseReference = database.getReference(path)
}
