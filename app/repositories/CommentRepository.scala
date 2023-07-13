package repositories

import models.Comment
import org.postgresql.util.PSQLException
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import db.MyPostgresProfile.api._
class CommentRepository @Inject() (
    override protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val comments = TableQuery[CommentTable]
  // TODO: create on startup??
  db.run(comments.schema.createIfNotExists)

  def insert(comment: Comment): Future[Option[Comment]] =
    db.run((comments returning comments) += comment)
      .map(Some.apply[Comment])
      // Throw a PSQLException if the query fails
      .recover { case e: PSQLException =>
        None
      }

  def getAll: Future[Seq[Comment]] = db.run(comments.result)

  def getById(id: Long): Future[Option[Comment]] = {
    // Filter all comments and return the one with the given id
    db.run(comments.filter(_.id === id).result).map(_.headOption)
  }

  def getByAuthorId(authorId: Long): Future[Option[Comment]] = {
    db.run(comments.filter(_.authorId === authorId).result).map(_.headOption)
  }

  def delete(id: Long): Future[Option[Int]] = {
    db.run(comments.filter(_.id === id).delete)
      .map {
        case 0       => None
        case 1       => Some(1)
        case deleted => throw new RuntimeException(s"Deleted $deleted rows")
      }
  }

  def updateContent(id: Long, content: String): Future[Option[String]] = {
    db.run(
      comments
        .filter(comment => comment.id === id)
        .map(_.content)
        .update(content)
        .map {
          case 0       => None
          case 1       => Some(content)
          case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }

  def updatelikeCount(id: Long, likeCount: Int): Future[Option[Int]] = {
    db.run(
      comments
        .filter(comment => comment.id === id)
        .map(_.likeCount)
        .update(likeCount)
        .map {
          case 0       => None
          case 1       => Some(likeCount)
          case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }

  private class CommentTable(tag: Tag) extends Table[Comment](tag, "comments") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def authorId = column[Long]("author_id")
    def imageId = column[Long]("image_id")

    def content = column[String]("content")

    def likeCount = column[Int]("like_count")

    // Maps table data to the case class
    override def * =
      (
        id,
        authorId,
        imageId,
        content,
        likeCount
      ) <> ((Comment.apply _).tupled, Comment.unapply)
  }
}
