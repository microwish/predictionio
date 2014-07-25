package io.prediction.controller

import io.prediction.core.BaseAlgorithm
import io.prediction.core.BaseServing

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD
import org.json4s.Formats
import org.json4s.native.Serialization

import scala.reflect._
import scala.reflect.runtime.universe._

/** Base class of local serving. For deployment, there should only be local
  * serving class.
  *
  * @tparam AP Algorithm parameters class.
  * @tparam Q Input query class.
  * @tparam P Output prediction class.
  */
abstract class LServing[AP <: Params : ClassTag, Q, P]
  extends BaseServing[AP, Q, P] {
  def serveBase(q: Q, ps: Seq[P]): P = {
    serve(q, ps)
  }

  /** Implement this method to combine multiple algorithms' predictions to
    * produce a single final prediction.
    *
    * @param query Input query.
    * @param predictions A list of algorithms' predictions.
    */
  def serve(query: Q, predictions: Seq[P]): P

  @transient lazy val formats: Formats = Utils.json4sDefaultFormats

  def stringToQ[Q : TypeTag : ClassTag](query: String): Q = {
    implicit val f = formats
    Serialization.read[Q](query)
  }
}

/** A concrete implementation of [[LServing]] returning the first algorithm's
  * prediction result directly without any modification.
  */
class FirstServing[Q, P] extends LServing[EmptyParams, Q, P] {
  /** Returns the first algorithm's prediction. */
  def serve(query: Q, predictions: Seq[P]): P = predictions.head
}

/** A concrete implementation of [[LServing]] returning the first algorithm's
  * prediction result directly without any modification.
  */
object FirstServing {
  /** Returns an instance of [[FirstServing]]. */
  def apply[Q, P](a: Class[_ <: BaseAlgorithm[_, _, _, Q, P]]) =
    classOf[FirstServing[Q, P]]
}

/** A concrete implementation of [[LServing]] returning the average of all
  * algorithms' predictions. The output prediction class is Double.
  */
class AverageServing[Q] extends LServing[EmptyParams, Q, Double] {
  /** Returns the average of all algorithms' predictions. */
  def serve(query: Q, predictions: Seq[Double]): Double = {
    predictions.sum / predictions.length
  }
}

/** A concrete implementation of [[LServing]] returning the average of all
  * algorithms' predictions. The output prediction class is Double.
  */
object AverageServing {
  /** Returns an instance of [[AverageServing]]. */
  def apply[Q](a: Class[_ <: BaseAlgorithm[_, _, _, Q, _]]) =
    classOf[AverageServing[Q]]
}
