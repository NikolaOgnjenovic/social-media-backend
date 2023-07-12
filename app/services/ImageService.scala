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

  def getByTag(tag: String): Future[Seq[Image]] =
    imageRepository.getByTag(tag)

  def getByTitle(title: String): Future[Seq[Image]] =
    imageRepository.getByTitle(title)
  def update(id: Long, image: Image): Future[Option[Image]] =
    imageRepository.update(id, image)

  def delete(id: Long): Future[Option[Int]] = imageRepository.delete(id)
}
