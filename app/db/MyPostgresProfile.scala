package db

import com.github.tminglei.slickpg._

trait MyPostgresProfile extends ExPostgresProfile with PgArraySupport {
  override val api = MyAPI
  object MyAPI extends API with ArrayImplicits
}

object MyPostgresProfile extends MyPostgresProfile
