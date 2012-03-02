package org.scalaquery.ast

import OptimizerUtil._
import scala.collection.mutable.{ArrayBuffer, HashMap}
import org.scalaquery.ql.{AbstractTable, Join}
import org.scalaquery.util.Logging

case class Comprehension(from: Seq[(Symbol, Node)], where: Seq[Node], select: Option[Node]) extends Node with DefNode {
  protected[this] def nodeChildGenerators = from.map(_._2) ++ where ++ select
  override protected[this] def nodeChildNames = from.map("from " + _._1) ++ where.zipWithIndex.map("where" + _._2) ++ select.map(_ => "select")
  def nodeMapChildren(f: Node => Node) = mapChildren(f, f)
  def mapChildren(fromMap: Node => Node, otherMap: Node => Node): Node = {
    val fromO = nodeMapNodes(from.view.map(_._2), fromMap)
    val whereO = nodeMapNodes(where, otherMap)
    val selectO = select.map(otherMap)
    if(fromO.isDefined || whereO.isDefined || selectO != select)
      copy(from = fromO.map(f => from.view.map(_._1).zip(f)).getOrElse(from), where = whereO.getOrElse(where), select = selectO)
    else this
  }
  def nodeGenerators = from
  override def toString = "Comprehension"
  def nodeMapGenerators(f: Symbol => Symbol) = {
    val gens = from.map(_._1)
    mapOrNone(gens, f) match {
      case Some(s) => copy(from = from.zip(s).map { case ((_, n), s) => (s, n) })
      case None => this
    }
  }
  def nodePostGeneratorChildren = select.toSeq
  def nodeMapScopedChildren(f: (Option[Symbol], Node) => Node) = {
    val fn = (n: Node) => f(None, n)
    val from2 = from.map{ case (s, n) => f(Some(s), n) }
    val fromO = if(from.zip(from2).forall{ case ((_, n1), n2) => n1 eq n2 }) None else Some(from2)
    val whereO = nodeMapNodes(where, fn)
    val selectO = select.map(fn)
    if(fromO.isDefined || whereO.isDefined || selectO != select)
      copy(from = fromO.map(f => from.view.map(_._1).zip(f)).getOrElse(from), where = whereO.getOrElse(where), select = selectO)
    else this
  }
}