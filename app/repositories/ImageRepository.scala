package repositories

import models.Image
import org.postgresql.util.PSQLException
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

class ImageRepository @Inject() (
    override protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val images = TableQuery[ImageTable]

  db.run(images.schema.createIfNotExists)

  def insert(image: Image): Future[Option[Image]] =
    db.run((images returning images) += image)
      .map(Some.apply[Image])
      // Throw a PSQLException if the query fails
      .recover { case e: PSQLException =>
        None
      }

  def getAll: Future[Seq[Image]] = db.run(images.result)

  def getById(id: Long): Future[Option[Image]] = {
    // Filter all images and return the one with the given id
    db.run(images.filter(_.id === id).result).map(_.headOption)
  }

  def getByTag(tag: String): Future[Seq[Image]] = {
    db.run(images.filter(_.tags.like("%" + tag + "%")).result)
  }

  def getByTitle(title: String): Future[Seq[Image]] = {
    db.run(images.filter(_.title.like("%" + title + "%")).result)
  }

  def delete(id: Long): Future[Option[Int]] = {
    //
    db.run(images.filter(_.id === id).delete)
      .map {
        case 0       => None
        case 1       => Some(1)
        case deleted => throw new RuntimeException(s"Deleted $deleted rows")
      }
  }

  def update(id: Long, image: Image): Future[Option[Image]] = {
    db.run(
      images
        .filter(_.id === id)
        .update(image.copy(tags = image.tags, title = image.title))
        .map {
          case 0       => None
          case 1       => Some(image)
          case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }

  private class ImageTable(tag: Tag) extends Table[Image](tag, "images") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def authorId = column[Long]("author_id")
    def tags = column[String]("tags") // csv

    def title = column[String]("title")
    def likes = column[Int]("likes")

    // Maps table data to the case class
    override def * =
      (
        id,
        authorId,
        tags,
        title,
        likes
      ) <> ((Image.apply _).tupled, Image.unapply)
  }
}