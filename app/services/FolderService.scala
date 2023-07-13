package services

import models.Folder
import repositories.FolderRepository

import javax.inject.Inject
import scala.concurrent.Future

class FolderService @Inject() (folderRepository: FolderRepository) {
  def create(folder: Folder): Future[Option[Folder]] =
    folderRepository.insert(folder)

  def getAll: Future[Seq[Folder]] = folderRepository.getAll

  def getById(id: Long): Future[Option[Folder]] = folderRepository.getById(id)
  def getByAuthorId(authorId: Long): Future[Option[Folder]] =
    folderRepository.getByAuthorId(authorId)

  def updateTitle(id: Long, title: String): Future[Option[String]] =
    folderRepository.updateTitle(id, title)

  def delete(id: Long): Future[Option[Int]] = folderRepository.delete(id)
}
