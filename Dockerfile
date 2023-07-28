# Use a base image with Java and sbt pre-installed
FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6 AS dependencies

WORKDIR /app

COPY project /app/project
COPY build.sbt /app/build.sbt

# Cache dependencies by running the sbt update command
RUN sbt update

# Copy only the build.sbt and project files for the next stage
# This way, we avoid invalidating the cache when the application code changes
FROM dependencies AS builder

COPY . /app

# Build the Scala Play application using sbt
RUN sbt clean compile stage

# Final stage to run the application (same as in the previous example)
FROM openjdk:11.0.12-jre

WORKDIR /app

# Copy the contents of /public/images from the project directory to /app/public/images in the container
COPY --from=builder /app/public/images /app/public/images

COPY --from=builder /app/target/universal/stage /app

CMD ["./bin/image-website-backend", "-Dconfig.resource=application-docker.conf"]
