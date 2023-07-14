import scala.concurrent.Future
import javax.inject._
import play.api.inject.ApplicationLifecycle
import repositories.{CommentRepository, FolderRepository, ImageRepository}

class ApplicationStart @Inject() (
    lifecycle: ApplicationLifecycle,
    imageRepository: ImageRepository,
    folderRepository: FolderRepository,
    commentRepository: CommentRepository
) {
  // Shut-down hook
  lifecycle.addStopHook { () =>
    Future.successful(())
  }

  imageRepository.createTable()
  folderRepository.createTable()
  commentRepository.createTable()
}
