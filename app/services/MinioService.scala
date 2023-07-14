package services

import com.google.inject.Inject
import io.minio.{
  BucketExistsArgs,
  GetObjectArgs,
  MakeBucketArgs,
  MinioClient,
  RemoveObjectArgs,
  UploadObjectArgs
}

import java.io.ByteArrayOutputStream
import scala.concurrent.{ExecutionContext, Future}
class MinioService @Inject() (implicit ec: ExecutionContext) {
  private val minioClient = MinioClient
    .builder()
    .endpoint("http://172.19.0.2:9000")
    .credentials(
      "Up0zU8fbULAKODuXUd2w",
      "0Pdjp3CLNzp7mfuJAy4FJuHrfIhWYYdSHU1YAINW"
    )
    .build()

  def upload(
      bucketName: String,
      id: String,
      filename: String
  ): Unit = {
    // Create a bucket if it does not exist
    if (
      !minioClient.bucketExists(
        BucketExistsArgs.builder().bucket(bucketName).build()
      )
    ) {
      minioClient.makeBucket(
        MakeBucketArgs
          .builder()
          .bucket(bucketName)
          .build()
      )
    }

    // Upload an object into the bucket
    minioClient.uploadObject(
      UploadObjectArgs
        .builder()
        .bucket(bucketName)
        .`object`(id)
        .filename(filename)
        .build()
    )
  }

  def get(bucketName: String, id: String): Future[Option[Array[Byte]]] = {
    val objectData: Future[Option[Array[Byte]]] = Future {
      try {
        // Get a GetObjectResponse from minio
        val objectData = minioClient
          .getObject(
            GetObjectArgs.builder().bucket(bucketName).`object`(id).build()
          )

        // Read the object data into a byte array
        val byteArrayOutputStream = new ByteArrayOutputStream()
        val buffer = new Array[Byte](8192) // Buffer size for reading data
        var bytesRead = -1
        while ({
          bytesRead = objectData.read(buffer);
          bytesRead
        } != -1) {
          byteArrayOutputStream.write(buffer, 0, bytesRead)
        }

        // Close both streams
        objectData.close()
        byteArrayOutputStream.close()

        // Return the byte array
        Some(byteArrayOutputStream.toByteArray)
      } catch {
        case _: io.minio.errors.MinioException | _: java.io.IOException => None
      }
    }

    objectData
  }

  def remove(bucketName: String, id: String): Future[Option[Int]] = {
    val removalData: Future[Option[Int]] = Future {
      try {
        minioClient.removeObject(
          RemoveObjectArgs.builder().bucket(bucketName).`object`(id).build()
        )
        Some(1)
      } catch {
        case _: io.minio.errors.MinioException | _: java.io.IOException => None
      }
    }
    removalData
  }
}
