/* cfg-for-funcs.scala

   This script returns a Json representation of the CFG for each method contained in the currently loaded CPG.

   Input: A valid CPG
   Output: Json

   Running the Script
   ------------------
   see: README.md

   The JSON generated has the following keys:

    "file": The file (as full path) the CPG was generated from
    "functions": Array of all methods contained in the currently loaded CPG
      |_ "function": Method name as String
      |_ "id": Method id as String (String representation of the underlying Method node)
      |_ "CFG": Array of all nodes connected via CFG edges
          |_ "id": Node id as String (String representation of the underlying CFG node)
          |_ "properties": Array of properties of the current node as key-value pair
          |_ "edges": Array of all CFG edges where the current node is referenced as inVertex or outVertex
              |_ "id": Edge id as String (String representation of the CFG edge)
              |_ "in": Node id as String of the inVertex node (String representation of the inVertex node)
              |_ "out": Node id as String of the outVertex node (String representation of the outVertex node)

   Sample Output
   -------------
   {
    "file" : "/path/to/free/free.c",
    "functions" : [
      {
        "function" : "free_list",
        "id" : "io.shiftleft.codepropertygraph.generated.nodes.Method@b",
        "CFG" : [
          {
            "id" : "io.shiftleft.codepropertygraph.generated.nodes.Call@12",
            "edges" : [
              {
                "id" : "io.shiftleft.codepropertygraph.generated.edges.Cfg@1bec0",
                "in" : "io.shiftleft.codepropertygraph.generated.nodes.Call@12",
                "out" : "io.shiftleft.codepropertygraph.generated.nodes.Identifier@15"
              },
              {
                "id" : "io.shiftleft.codepropertygraph.generated.edges.Cfg@1d4e9",
                "in" : "io.shiftleft.codepropertygraph.generated.nodes.Identifier@18",
                "out" : "io.shiftleft.codepropertygraph.generated.nodes.Call@12"
              }
            ],
            "properties" : [
              {
                "key" : "DISPATCH_TYPE",
                "value" : "STATIC_DISPATCH"
              },
              {
                "key" : "METHOD_INST_FULL_NAME",
                "value" : "<operator>.assignment"
              },
              {
                "key" : "NAME",
                "value" : "<operator>.assignment"
              },
              {
                "key" : "CODE",
                "value" : "*p = head"
              },
              {
                "key" : "LINE_NUMBER",
                "value" : "9"
              },
              // ...
 */

import scala.collection.JavaConverters._

import io.circe.syntax._
import io.circe.generic.semiauto._
import io.circe.{Encoder, Json}

import io.shiftleft.codepropertygraph.generated.nodes.CfgNode

import gremlin.scala._
import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.VertexProperty


final case class CfgForFuncsFunction(function: String, id: String, CFG: List[CfgNode])
final case class CfgForFuncsResult(file: String, functions: List[CfgForFuncsFunction])

implicit val encodeFuncResult: Encoder[CfgForFuncsResult] = deriveEncoder
implicit val encodeFuncFunction: Encoder[CfgForFuncsFunction] = deriveEncoder
implicit val encodeVertex: Encoder[CfgNode] =
  (node: CfgNode) =>
    Json.obj(
      ("id", Json.fromString(node.toString)),
      ("edges",
        Json.fromValues(
          node.graph.E
            .hasLabel("CFG")
            .l
            .collect {
              case e if e.inVertex == node  => e
              case e if e.outVertex == node => e
            }
            .map { edge: Edge =>
              Json.obj(
                ("id", Json.fromString(edge.toString)),
                ("in", Json.fromString(edge.inVertex().toString)),
                ("out", Json.fromString(edge.outVertex().toString))
              )
            })),
      ("properties", Json.fromValues(node.properties().asScala.toList.map { p: VertexProperty[_] =>
        Json.obj(
          ("key", Json.fromString(p.key())),
          ("value", Json.fromString(p.value().toString))
        )
      }))
    )

CfgForFuncsResult(
  cpg.file.name.l.head,
  cpg.method.name.l.map { methodName =>
    val method = cpg.method.name(methodName)
    CfgForFuncsFunction(methodName, cpg.method.name(methodName).l.head.toString, method.cfgNode.l)
  }
).asJson