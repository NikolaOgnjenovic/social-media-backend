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

  def createTable(): Future[Unit] = db.run(comments.schema.createIfNotExists)

  // Adds the comment to the table and gets the comment by id in order to return the comment with the new id
  def create(comment: Comment): Future[Option[Comment]] =
    db.run((comments returning comments.map(_.id)) += comment)
      .flatMap { generatedId =>
        getById(generatedId)
      }
      .recoverWith { case _: PSQLException =>
        Future.successful(None)
      }

  def getAll: Future[Seq[Comment]] = db.run(comments.result)
  def getAllByUserId(userId: Long): Future[Seq[Comment]] =
    db.run(comments.filter(_.authorId === userId).result)

  def getById(id: Long): Future[Option[Comment]] =
    db.run(comments.filter(_.id === id).result).map(_.headOption)

  def getByAuthorId(authorId: Long): Future[Option[Comment]] =
    db.run(comments.filter(_.authorId === authorId).result).map(_.headOption)

  def getByImageId(imageId: Long): Future[Option[Comment]] =
    db.run(comments.filter(_.imageId === imageId).result).map(_.headOption)

  def updateContent(
      userId: Long,
      id: Long,
      content: String
  ): Future[Option[String]] =
    db.run(
      comments
        .filter(comment => comment.authorId === userId && comment.id === id)
        .map(_.content)
        .update(content)
    ).flatMap {
      case 0 => Future.successful(None)
      case 1 => Future.successful(Some(content))
      case updatedRowCount =>
        Future.failed(
          throw new RuntimeException(s"Updated $updatedRowCount rows")
        )
    }

  def updateLikeCount(
      id: Long,
      likeCount: Int
  ): Future[Option[Int]] =
    db.run(
      comments
        .filter(_.id === id)
        .map(_.likeCount)
        .update(likeCount)
    ).flatMap {
      case 0 => Future.successful(None)
      case 1 => Future.successful(Some(likeCount))
      case updatedRowCount =>
        Future.failed(
          throw new RuntimeException(s"Updated $updatedRowCount rows")
        )
    }

  def delete(userId: Long, id: Long): Future[Option[Long]] =
    db.run(
      comments
        .filter(comment => comment.authorId === userId && comment.id === id)
        .delete
    ).flatMap {
      case 0 => Future.successful(None)
      case 1 => Future.successful(Some(id))
      case deletedRowCount =>
        Future.failed(
          throw new RuntimeException(s"Deleted $deletedRowCount rows")
        )
    }

  def deleteByImageId(imageId: Long): Future[Option[Boolean]] =
    db.run(comments.filter(_.imageId === imageId).delete)
      .flatMap {
        case 0 => Future.successful(Some(true))
        case 1 => Future.successful(Some(true))
        case deletedRowCount =>
          Future.failed(
            throw new RuntimeException(s"Deleted $deletedRowCount rows")
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
