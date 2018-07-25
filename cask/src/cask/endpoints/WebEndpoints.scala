package cask.endpoints

import cask.internal.Router
import cask.internal.Router.EntryPoint
import cask.main.Routes
import cask.model.{BaseResponse, ParamContext}

import collection.JavaConverters._


trait WebEndpoint extends Endpoint[BaseResponse]{
  type InputType = Seq[String]
  def wrapMethodOutput(t: BaseResponse) = t
  def parseMethodInput[T](implicit p: QueryParamReader[T]) = p
  def handle(ctx: ParamContext,
             bindings: Map[String, String],
             routes: Routes,
             entryPoint: EntryPoint[Seq[String], Routes, cask.model.ParamContext]): Router.Result[BaseResponse] = {
    val allBindings =
      bindings.map{case (k, v) => (k, Seq(v))} ++
        ctx.exchange.getQueryParameters
          .asScala
          .toSeq
          .map{case (k, vs) => (k, vs.asScala.toArray.toSeq)}

    entryPoint.invoke(routes, ctx, allBindings)
      .asInstanceOf[Router.Result[BaseResponse]]
  }
}
class get(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class post(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class put(val path: String, override val subpath: Boolean = false) extends WebEndpoint
class route(val path: String, val methods: Seq[String], override val subpath: Boolean = false) extends WebEndpoint

abstract class QueryParamReader[T]
  extends Router.ArgReader[Seq[String], T, cask.model.ParamContext]{
  def arity: Int
  def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]): T
}
object QueryParamReader{
  class SimpleParam[T](f: String => T) extends QueryParamReader[T]{
    def arity = 1
    def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]): T = f(v.head)
  }

  implicit object StringParam extends SimpleParam[String](x => x)
  implicit object BooleanParam extends SimpleParam[Boolean](_.toBoolean)
  implicit object ByteParam extends SimpleParam[Byte](_.toByte)
  implicit object ShortParam extends SimpleParam[Short](_.toShort)
  implicit object IntParam extends SimpleParam[Int](_.toInt)
  implicit object LongParam extends SimpleParam[Long](_.toLong)
  implicit object DoubleParam extends SimpleParam[Double](_.toDouble)
  implicit object FloatParam extends SimpleParam[Float](_.toFloat)
  implicit def SeqParam[T: QueryParamReader] = new QueryParamReader[Seq[T]]{
    def arity = 1
    def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]): Seq[T] = {
      v.map(x => implicitly[QueryParamReader[T]].read(ctx, label, Seq(x)))
    }
  }
  implicit def paramReader[T: ParamReader] = new QueryParamReader[T] {
    override def arity = 0

    override def read(ctx: cask.model.ParamContext, label: String, v: Seq[String]) = {
      implicitly[ParamReader[T]].read(ctx, label, v)
    }
  }

}
