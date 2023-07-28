# Use a base image with Java and sbt pre-installed
FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6 AS builder

WORKDIR /app

COPY project /app/project
COPY build.sbt /app/build.sbt

# Cache dependencies by running the sbt update command
RUN sbt update

COPY . /app

# Build the Scala Play application using sbt
RUN sbt clean compile stage

# Final stage to run the application
FROM openjdk:11.0.12-jre

WORKDIR /app

# Copy only the necessary files from the builder stage
COPY --from=builder /app/target/universal/stage /app

COPY --from=builder /app/conf /app/conf

# Copy the contents of /public/images from the project directory to /app/public/images in the container
COPY --from=builder /app/public/images /app/public/images

ENV JAVA_OPTS="-Dconfig.resource=application-docker.conf"

CMD ["./bin/image-website-backend"]
