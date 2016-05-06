package graph

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape}
import scala.concurrent.Future

case class User(name: String)


object AsyncMultipleSourceStoreExample extends App {

  implicit val system = ActorSystem("NewJob")
  implicit val dispacher = system.dispatcher
  implicit val materializer = ActorMaterializer()

  val listOfCountries = List("US", "INDIA", "CANADA")

  import GraphDSL.Implicits._

  def createGraph() = {
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      val merge = builder.add(Merge[User](listOfCountries.length))
      val flow = builder.add(Flow[User].mapAsync(parallelism = 10)(x =>  saveInformation(x)))

      Source.fromFuture(getUsers(listOfCountries.head)).mapConcat(identity) ~> merge ~> flow ~> Sink.foreachParallel(parallelism = 10)(println)

      for {
        i <- listOfCountries.tail //for each other country list
      } yield (Source.fromFuture(getUsers(i)).mapConcat(identity) ~> merge) //create source of remaining countries

      ClosedShape
    })
  }


  /**
    * get user information from database/other system;
    * they might respond slow or fast depend on upon amount of
    * data and system latency
    * @param countryCode
    * @return
    */
  def getUsers(countryCode: String): Future[List[User]] = {
    countryCode match {
      case "US" => {
        Future {
          println(s"now return for US, current time :${System.currentTimeMillis()}")
          //this can be reading from database
          List(User("US User1"), User("US User2"))
        }

      }
      case "CANADA" => {
        Future {
          println(s"now return for India, current time: ${System.currentTimeMillis()}")
          //this can be reading from database
          List(User("Canada User1"), User("Canada User2"), User("Canada User3"))
        }
      }
      case "INDIA" => {
        Future {
          //make this simulate slowness
           Thread.sleep(1000)
          println(s"now return for 100 time: ${System.currentTimeMillis()}")
          //this can be reading from database
          List(User("India User1"), User("India User2"))
        }
      }
    }
  }

  /**
    * save information what we received
    * @param obj
    * @return
    */
  def saveInformation(obj: User) = {
    Future {
      println("Saving db " + obj)
      "Saved"
    }
  }

  createGraph.run()

}
