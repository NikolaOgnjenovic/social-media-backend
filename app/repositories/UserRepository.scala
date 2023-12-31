package repositories

import auth.JwtAction
import models.User
import org.postgresql.util.PSQLException
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import db.MyPostgresProfile.api._
import org.mindrot.jbcrypt.BCrypt
class UserRepository @Inject() (
    override protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val users = TableQuery[UserTable]

  def createTable(): Future[Unit] = {
    db.run(users.schema.createIfNotExists)
  }

  def create(user: User): Future[Option[User]] = {
    val newUser = new User(
      user.id,
      user.username,
      BCrypt.hashpw(user.password, BCrypt.gensalt(5)),
      List(),
      List()
    )
    db.run((users returning users) += newUser)
      .map(Some.apply[User])
      // Throw a PSQLException if the query fails
      .recover { case _: PSQLException =>
        None
      }
  }

  def login(user: User): Future[Option[User]] = db
    .run(
      users
        .filter(_.username === user.username)
        .result
    )
    .map(_.headOption.filter(u => BCrypt.checkpw(user.password, u.password)))
  def getAll: Future[Seq[User]] = db.run(users.result)

  def getById(id: Long): Future[Option[User]] = {
    // Filter all users and return the one with the given id
    db.run(users.filter(_.id === id).result).map(_.headOption)
  }

  def updateLikedImageIds(
      id: Long,
      likedImageIds: List[Long]
  ): Future[Option[List[Long]]] =
    db.run(
      users
        .filter(user => user.id === id)
        .map(_.likedImageIds)
        .update(likedImageIds)
    ).map {
      case 0       => Some(likedImageIds)
      case 1       => Some(likedImageIds)
      case updated => throw new RuntimeException(s"Updated $updated rows")
    }

  def updateLikedCommentIds(
      id: Long,
      likedCommentIds: List[Long]
  ): Future[Option[List[Long]]] =
    db.run(
      users
        .filter(user => user.id === id)
        .map(_.likedCommentIds)
        .update(likedCommentIds)
    ).map {
      case 0       => Some(likedCommentIds)
      case 1       => Some(likedCommentIds)
      case updated => throw new RuntimeException(s"Updated $updated rows")
    }

  def updatePassword(id: Long, password: String): Future[Option[String]] = {
    db.run(
      users.filter(user => user.id === id).map(_.password).update(password)
    ).map {
      case 0       => None
      case 1       => Some(password)
      case updated => throw new RuntimeException(s"Updated $updated rows")
    }
  }

  def delete(id: Long): Future[Option[Int]] = {
    db.run(users.filter(_.id === id).delete)
      .map {
        case 0       => None
        case 1       => Some(1)
        case deleted => throw new RuntimeException(s"Deleted $deleted rows")
      }
  }

  private class UserTable(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

    def username = column[String]("username")
    def password = column[String]("password")
    def likedImageIds = column[List[Long]]("liked_image_ids")
    def likedCommentIds = column[List[Long]]("liked_comment_ids")

    // Maps table data to the case class
    override def * = (
      id,
      username,
      password,
      likedImageIds,
      likedCommentIds
    ) <> ((User.apply _).tupled, User.unapply)
  }
}
