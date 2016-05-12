package graph.tick

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.{ActorMaterializer, ClosedShape}
import com.sun.tools.internal.xjc.reader.xmlschema.bindinfo.BIConversion.User
import graph.BusinessOperations

import scala.concurrent.duration.FiniteDuration

object AsyncMultipleSourceStoreExample extends App with BusinessOperations {

  implicit val system = ActorSystem("TickNewJob")

  implicit val dispacher = system.dispatcher

  implicit val materializer = ActorMaterializer()

  val initialDelay = FiniteDuration(0, TimeUnit.SECONDS)

  //get user data eveny 2 seconds
  val interval = FiniteDuration(2, TimeUnit.SECONDS)

  val listOfCountries = List("CountryA", "CountryB", "CountryC")


  def createTickGraph = {
    import GraphDSL.Implicits._
    RunnableGraph.fromGraph(GraphDSL.create() { implicit builder =>

      val merge = builder.add(Merge[User](listOfCountries.length))
      val flow = builder.add(Flow[User].mapAsync(parallelism = 10)(x => saveInformation(x)))

      //source tick returns Future[List[User]]
      Source.tick(initialDelay, interval
        , getUsers(listOfCountries.head)).mapAsync(1)(x => x).mapConcat(identity) ~> merge ~> flow ~> Sink
        .foreachParallel(parallelism = 10)(println)

      for {
        i <- listOfCountries.tail //for each other country list
      } yield (Source.tick(initialDelay, interval
        , getUsers(i)).mapAsync(10)(x => x).mapConcat(identity) ~> merge) //create source of remaining countries

      ClosedShape
    })
  }


  createTickGraph.run()

}
