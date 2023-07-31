package services

import com.google.inject.Inject
import com.typesafe.config.Config
import io.minio._
import _root_.org.slf4j.LoggerFactory

import java.io.ByteArrayOutputStream
import scala.util.Try

class MinioService @Inject() (config: Config) {
  private val minioEndpoint = config.getString("minio.endpoint")
  private val logger = LoggerFactory.getLogger(getClass)
  logger.info(minioEndpoint)

  private val minioAccessKey = config.getString("minio.accessKey")
  private val minioSecretAccessKey = config.getString("minio.secretAccessKey")
  private val minioClient = MinioClient
    .builder()
    .endpoint(minioEndpoint)
    .credentials(minioAccessKey, minioSecretAccessKey)
    .build()

  def upload(
      bucketName: String,
      id: String,
      filename: String
  ): Try[ObjectWriteResponse] =
    Try {
      logger.info(
        "Bucket name: " + bucketName,
        "id: " + id,
        " Filename: " + filename
      )
      // Create a bucket if it does not exist
      if (
        !minioClient.bucketExists(
          BucketExistsArgs.builder().bucket(bucketName).build()
        )
      ) {
        minioClient.makeBucket(
          MakeBucketArgs.builder().bucket(bucketName).build()
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

  def get(bucketName: String, id: String): Try[Array[Byte]] = Try {
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
      bytesRead = objectData.read(buffer)
      bytesRead
    } != -1) {
      byteArrayOutputStream.write(buffer, 0, bytesRead)
    }

    // Close both streams
    objectData.close()
    byteArrayOutputStream.close()

    // Return the byte array
    byteArrayOutputStream.toByteArray
  }

  def remove(bucketName: String, id: String): Try[Unit] = Try {
    // Remove an object from the bucket with the given name
    minioClient.removeObject(
      RemoveObjectArgs.builder().bucket(bucketName).`object`(id).build()
    )
  }
}
