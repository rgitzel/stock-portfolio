package example

import akka.actor.ActorSystem
import com.github.rgitzel.stocks.influxdb.{InfluxDbPricesRepository, InfluxDbQueryRunner}
import com.github.rgitzel.stocks.models.TradingDay

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.global

object PricesExample extends InfluxDbExample  {

  implicit val system: ActorSystem = ActorSystem("it-tests")

  def main(args: Array[String]): Unit = {

    val repository = new InfluxDbPricesRepository(new InfluxDbQueryRunner(influxDBClient))(global)

    val prices = repository.closingPrices(TradingDay(5, 20, 2022))

    Await.result(prices, Duration.Inf).foreach(println)

    influxDBClient.close()
    system.terminate()
  }
}
