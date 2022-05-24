package example

import akka.actor.ActorSystem
import com.github.rgitzel.stocks.influxdb.{InfluxDbForexRepository, InfluxDbQueryRunner}
import com.github.rgitzel.stocks.models.TradingDay

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.global

object ForexExample extends InfluxDbExample {

  implicit val system: ActorSystem = ActorSystem("it-tests")

  def main(args: Array[String]): Unit = {

    val repository = new InfluxDbForexRepository(new InfluxDbQueryRunner(influxDBClient))(global)

    val rates = repository.closingRates(TradingDay(5, 20, 2022))

    Await.result(rates, Duration.Inf).foreach(println)

    influxDBClient.close()
    system.terminate()
  }
}
