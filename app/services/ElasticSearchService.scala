import com.sksamuel.elastic4s.ElasticApi.{
  createIndex,
  intField,
  longField,
  properties,
  textField
}
import com.sksamuel.elastic4s.ElasticDsl.CreateIndexHandler
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.{
  ElasticClient,
  ElasticDsl,
  ElasticProperties,
  RequestFailure,
  RequestSuccess,
  Response
}
import com.sksamuel.elastic4s.http.JavaClient

import scala.concurrent.{Await, ExecutionContext}

class ElasticSearchService()(implicit ec: ExecutionContext) {
  private val elasticClient = ElasticClient(
    JavaClient(ElasticProperties("http://localhost:9005"))
  )

//  def create(): Response[Unit] =
//    elasticClient.execute {
//      createIndex("images").mapping(
//        properties(
//          longField("id"),
//          longField("authorId"),
//          textField("tags"),
//          textField("title"),
//          intField("likeCount"),
//          longField("editorIds"),
//          longField("folderId")
//        )
//      )
//    }.await
}
