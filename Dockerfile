# Use a base image with Java and sbt pre-installed
FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6

# Set the working directory in the container
WORKDIR /app

# Copy the application files to the container's working directory
COPY . /app

# Build the Scala Play application using sbt
RUN sbt clean compile

# Run the Scala Play application
CMD ["sbt", "run"]
