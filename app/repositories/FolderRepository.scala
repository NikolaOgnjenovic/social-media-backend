package repositories

import models.Folder
import org.postgresql.util.PSQLException
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}

class FolderRepository @Inject() (
    override protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val folders = TableQuery[FolderTable]

  def insert(folder: Folder): Future[Option[Folder]] =
    db.run((folders returning folders) += folder)
      .map(Some.apply[Folder])
      // Throw a PSQLException if the query fails
      .recover { case e: PSQLException =>
        None
      }

  def getAll: Future[Seq[Folder]] = db.run(folders.result)

  def getById(id: Long): Future[Option[Folder]] = {
    // Filter all folders and return the one with the given id
    db.run(folders.filter(_.id === id).result).map(_.headOption)
  }

  def getByAuthorId(authorId: Long): Future[Option[Folder]] = {
    db.run(folders.filter(_.authorId === authorId).result).map(_.headOption)
  }

  def delete(id: Long): Future[Option[Int]] = {
    db.run(folders.filter(_.id === id).delete)
      .map {
        case 0       => None
        case 1       => Some(1)
        case deleted => throw new RuntimeException(s"Deleted $deleted rows")
      }
  }

  // This is a temporary work-around
  def update(id: Long, folder: Folder): Future[Option[Folder]] = {
    delete(id)
    insert(folder)
  }
  def update_old(id: Long, folder: Folder): Future[Option[Folder]] = {
    // TODO: find a way to update a row which has an auto-generated identity always column
    db.run(
      folders
        .filter(folder => folder.id === id)
        .update(
          folder.copy(
            id = folder.id,
            imageIds = folder.imageIds,
            title = folder.title
          )
        )
        //.update(folder.copy(imageIds = folder.imageIds, title = folder.title))
        .map {
          case 0 => None
          case 1 => Some(folder)
          //case updated => throw new RuntimeException(s"Updated $updated rows")
        }
    )
  }

  private class FolderTable(tag: Tag) extends Table[Folder](tag, "folders") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def authorId = column[Long]("author_id")
    def imageIds = column[String]("image_ids") // csv
    def title = column[String]("title")

    // Maps table data to the case class
    override def * = (
      id,
      authorId,
      imageIds,
      title
    ) <> ((Folder.apply _).tupled, Folder.unapply)
  }
}
