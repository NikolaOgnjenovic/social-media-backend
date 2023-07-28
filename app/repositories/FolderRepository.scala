package repositories

import models.Folder
import org.postgresql.util.PSQLException
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import db.MyPostgresProfile.api._

class FolderRepository @Inject() (
    override protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val folders = TableQuery[FolderTable]

  def createTable(): Future[Unit] = db.run(folders.schema.createIfNotExists)

  def create(folder: Folder): Future[Option[Folder]] =
    db.run((folders returning folders) += folder)
      .flatMap { _ =>
        Future.successful(Some(folder))
      }
      .recoverWith { case _: PSQLException =>
        Future.successful(None)
      }

  def getAll: Future[Seq[Folder]] = db.run(folders.result)
  def getAllByUserId(userId: Long): Future[Seq[Folder]] =
    db.run(folders.filter(_.authorId === userId).result)

  // Filter all folders and return the one with the given id
  def getById(id: Long): Future[Option[Folder]] =
    db.run(folders.filter(_.id === id).result).map(_.headOption)

  def getByAuthorId(authorId: Long): Future[Option[Folder]] =
    db.run(folders.filter(_.authorId === authorId).result).map(_.headOption)

  def updateTitle(
      userId: Long,
      id: Long,
      title: String
  ): Future[Option[String]] =
    db.run(
      folders
        .filter(folder => folder.authorId === userId && folder.id === id)
        .map(_.title)
        .update(title)
    ).flatMap {
      case 0 => Future.successful(None)
      case 1 => Future.successful(Some(title))
      case deletedRowCount =>
        Future.failed(new RuntimeException(s"Deleted $deletedRowCount rows"))
    }

  def delete(userId: Long, id: Long): Future[Option[Long]] =
    db.run(
      folders
        .filter(folder => folder.authorId === userId && folder.id === id)
        .delete
    ).flatMap {
      case 0 => Future.successful(None)
      case 1 => Future.successful(Some(id))
      case deletedRowCount =>
        Future.failed(new RuntimeException(s"Deleted $deletedRowCount rows"))
    }

  private class FolderTable(tag: Tag) extends Table[Folder](tag, "folders") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def authorId = column[Long]("author_id")
    def title = column[String]("title")

    // Maps table data to the case class
    override def * = (
      id,
      authorId,
      title
    ) <> ((Folder.apply _).tupled, Folder.unapply)
  }
}
