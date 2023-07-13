package services

import models.Image
import repositories.ImageRepository

import javax.inject.Inject
import scala.concurrent.Future

class ImageService @Inject() (imageRepository: ImageRepository) {
  def create(image: Image): Future[Option[Image]] =
    imageRepository.insert(image)

  def getAll: Future[Seq[Image]] = imageRepository.getAll

  def getById(id: Long): Future[Option[Image]] = imageRepository.getById(id)

  def getByTags(tags: List[String]): Future[Seq[Image]] =
    imageRepository.getByTags(tags)

  def getByTitle(title: String): Future[Seq[Image]] =
    imageRepository.getByTitle(title)

  def getByFolderId(folderId: Long): Future[Seq[Image]] =
    imageRepository.getByFolderId(folderId)

  def updateTags(id: Long, tags: List[String]): Future[Option[List[String]]] =
    imageRepository.updateTags(id, tags)
  def updateLikes(id: Long, likes: Int): Future[Option[Int]] =
    imageRepository.updateLikes(id, likes)
  def updateEditorIds(
      id: Long,
      editorIds: List[Long]
  ): Future[Option[List[Long]]] =
    imageRepository.updateEditorIds(id, editorIds)
  def updateFolderId(id: Long, folderId: Long): Future[Option[Long]] =
    imageRepository.updateFolderId(id, folderId)
  def delete(id: Long): Future[Option[Int]] = imageRepository.delete(id)
}
