package services

import models.Comment
import repositories.CommentRepository

import javax.inject.Inject
import scala.concurrent.Future

class CommentService @Inject() (commentRepository: CommentRepository) {
  def create(comment: Comment): Future[Option[Comment]] =
    commentRepository.insert(comment)

  def getAll: Future[Seq[Comment]] = commentRepository.getAll

  def getById(id: Long): Future[Option[Comment]] = commentRepository.getById(id)
  def getByAuthorId(authorId: Long): Future[Option[Comment]] =
    commentRepository.getByAuthorId(authorId)

  def updateContent(id: Long, content: String): Future[Option[String]] =
    commentRepository.updateContent(id, content)

  def updateLikes(id: Long, likes: Int): Future[Option[Int]] =
    commentRepository.updateLikes(id, likes)
  def delete(id: Long): Future[Option[Int]] = commentRepository.delete(id)
}