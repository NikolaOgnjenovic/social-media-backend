package repositories

import models.Image
import org.postgresql.util.PSQLException
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import db.MyPostgresProfile.api._

class ImageRepository @Inject() (
    override protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val images = TableQuery[ImageTable]

  createTable()
  def createTable(): Future[Unit] = {
    db.run(images.schema.createIfNotExists)
  }

  def create(image: Image): Future[Option[Image]] = {
    createTable()
    db.run((images returning images) += image)
      .map(Some.apply[Image])
      // Throw a PSQLException if the query fails
      .recover { case _: PSQLException =>
        None
      }
  }

  def getAll(userId: Long): Future[Seq[Image]] =
    db.run(images.filter(_.authorId === userId).result)

  def getById(id: Long): Future[Option[Image]] = {
    // Filter all images and return the one with the given id
    db.run(images.filter(_.id === id).result).map(_.headOption)
  }

  def getByTags(tags: List[String]): Future[Seq[Image]] = {
    db.run(images.filter(_.tags @> tags.bind).result)
  }

  def getByTitle(title: String): Future[Seq[Image]] = {
    db.run(images.filter(_.title.like("%" + title + "%")).result)
  }

  def getByFolderId(folderId: Long): Future[Seq[Image]] = {
    db.run(images.filter(_.folderId === folderId).result)
  }

  def updateTags(
      userId: Long,
      id: Long,
      tags: List[String]
  ): Future[Option[List[String]]] = {
    db.run(
      images
        .filter(image => image.authorId === userId && image.id === id)
        .map(_.tags)
        .update(tags)
        .map {
          case 0       => None
          case 1       => Some(tags)
          case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }
  def updateLikeCount(
      userId: Long,
      id: Long,
      likeCount: Int
  ): Future[Option[Int]] = {
    db.run(
      images
        .filter(image => image.authorId === userId && image.id === id)
        .map(_.likeCount)
        .update(likeCount)
        .map {
          case 0       => None
          case 1       => Some(likeCount)
          case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }

  def updateEditorIds(
      userId: Long,
      id: Long,
      editorIds: List[Long]
  ): Future[Option[List[Long]]] = {
    db.run(
      images
        .filter(image => image.authorId === userId && image.id === id)
        .map(_.editorIds)
        .update(editorIds)
        .map {
          case 0       => None
          case 1       => Some(editorIds)
          case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }

  def updateFolderId(
      userId: Long,
      id: Long,
      folderId: Long
  ): Future[Option[Long]] = {
    db.run(
      images
        .filter(image => image.authorId === userId && image.id === id)
        .map(_.folderId)
        .update(folderId)
        .map {
          case 0       => None
          case 1       => Some(folderId)
          case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }

  def delete(userId: Long, id: Long): Future[Option[Int]] = {
    db.run(
      images
        .filter(image => image.authorId === userId && image.id === id)
        .delete
    ).map {
      case 0       => None
      case 1       => Some(1)
      case deleted => throw new RuntimeException(s"Deleted $deleted rows")
    }
  }

  private class ImageTable(tag: Tag) extends Table[Image](tag, "images") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def authorId = column[Long]("author_id")
    def tags = column[List[String]]("tags")

    def title = column[String]("title")
    def likeCount = column[Int]("like_count")
    def editorIds = column[List[Long]]("editor_ids")
    def folderId = column[Long]("folder_id")

    // Maps table data to the case class
    override def * =
      (
        id,
        authorId,
        tags,
        title,
        likeCount,
        editorIds,
        folderId
      ) <> ((Image.apply _).tupled, Image.unapply)
  }
}
