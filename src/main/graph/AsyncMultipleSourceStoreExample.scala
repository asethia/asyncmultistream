package graph

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape}
import scala.concurrent.Future


trait BusinessOperations{

  import scala.concurrent.ExecutionContext.Implicits.global

  case class User(name: String, country: String)

  /**
    * get user information from database/other system;
    * they might respond slow or fast depend on upon amount of
    * data and system latency
    *
    * @param countryCode
    * @return
    */
  def getUsers(countryCode: String): Future[List[User]] = {
    countryCode match {
      case "CountryA" => {
        Future {
          //this can be reading from database
          List(User("CountryA User1", "CountryA"), User("US User2", "CountryA"))
        }

      }
      case "CountryB" => {
        Future {
          //this can be reading from database
          List(User("CountryB User1", "CountryB"), User("CountryB User2", "CountryB"), User("CountryB User3", "CountryB"))
        }
      }
      case "CountryC" => {
        Future {
          //make this simulate slowness
          Thread.sleep(1000)
          //this can be reading from database
          List(User("CountryC User1", "CountryC"), User("CountryC User2", "CountryC"))
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

}


/**
    * This code is simulation of reading
    * multiple sources, process and store them parallel
    * 
    */
object AsyncMultipleSourceStoreExample extends App with BusinessOperations {

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




  createGraph.run()

}
