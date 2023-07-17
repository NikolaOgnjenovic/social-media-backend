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

  def getByImageId(imageId: Long): Future[Option[Comment]] =
    commentRepository.getByImageId(imageId)

  def updateContent(id: Long, content: String): Future[Option[String]] =
    commentRepository.updateContent(id, content)

  def updateLikeCount(id: Long, likeCount: Int): Future[Option[Int]] =
    commentRepository.updateLikeCount(id, likeCount)
  def delete(id: Long): Future[Option[Int]] = commentRepository.delete(id)

  def deleteCommentsByImageId(imageId: Long): Future[Option[Int]] =
    commentRepository.deleteByImageId(imageId)
}
