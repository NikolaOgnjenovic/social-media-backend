package repositories

import org.postgresql.util.PSQLException
import slick.jdbc.JdbcProfile

import javax.inject.Inject
import models.JwtToken
import scala.concurrent.{ExecutionContext, Future}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import db.MyPostgresProfile.api._

class JwtRepository @Inject() (
    override protected val dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext)
    extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val blacklistedTokens = TableQuery[JwtTokenTable]

  def createTable(): Future[Unit] = {
    db.run(blacklistedTokens.schema.createIfNotExists)
  }

  def blacklist(jwtToken: String): Future[Option[JwtToken]] = {
    db.run(
      (blacklistedTokens returning blacklistedTokens) += JwtToken(0, jwtToken)
    ).map(Some.apply)
      .recover { case _: PSQLException =>
        None
      }
  }

  def getAll: Future[Seq[String]] =
    db.run(blacklistedTokens.map(_.token).result)

  private class JwtTokenTable(tag: Tag)
      extends Table[JwtToken](tag, "blacklisted_tokens") {
    private def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def token = column[String]("token")

    // Maps table data to the case class
    override def * =
      (id, token) <> ((JwtToken.apply _).tupled, JwtToken.unapply)
  }
}
