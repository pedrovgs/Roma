package com.github.pedrovgs.roma.storage

import com.github.pedrovgs.roma.FirebaseError
import com.github.pedrovgs.roma.config.ConfigLoader
import com.google.firebase.database._
import com.google.firebase.tasks.Task
import com.google.firebase.{FirebaseApp, FirebaseOptions}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

object Firebase {

  private val credentials = getClass.getResourceAsStream("/firebaseCredentials.json")
  private val options = {
    val config      = ConfigLoader.loadFirebaseConfig()
    val databaseUrl = config.map(_.databaseUrl).getOrElse("")
    new FirebaseOptions.Builder()
      .setDatabaseUrl(databaseUrl)
      .setServiceAccount(credentials)
      .build()
  }

  def save[T](path: String, value: T, pushFirst: Boolean): Future[T] = save(path, Seq(value), pushFirst).map(_.head)

  def save[T](path: String, values: Seq[T], pushFirst: Boolean = true): Future[Seq[T]] = {
    val futures = values.map { value =>
      val promise   = Promise[T]
      val reference = if (pushFirst) ref(path).push() else ref(path)
      reference
        .setValue(
          value,
          new DatabaseReference.CompletionListener {
            override def onComplete(databaseError: DatabaseError, databaseReference: DatabaseReference): Unit = {
              if (databaseError == null) {
                promise.trySuccess(value)
              } else {
                promise.failure(new FirebaseError)
              }
            }
          }
        )
      promise.future
    }
    Future.sequence(futures)
  }

  def get[T](path: String, tyze: Class[T]): Future[Option[T]] = {
    val promise = Promise[Option[T]]
    ref(path).addListenerForSingleValueEvent(new ValueEventListener {
      override def onCancelled(databaseError: DatabaseError): Unit = {
        promise.failure(databaseError.toException)
      }

      override def onDataChange(dataSnapshot: DataSnapshot): Unit = {
        val value: T = dataSnapshot.getValue(tyze)
        promise.success(Option(value))
      }
    })
    promise.future
  }

  def remove(path: String): Task[Void] = {
    ref(path).removeValue()
  }

  FirebaseApp.initializeApp(options)
  private val database = FirebaseDatabase.getInstance()

  private def ref(path: String): DatabaseReference = database.getReference(path)
}
