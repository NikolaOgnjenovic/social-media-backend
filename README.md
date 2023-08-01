# Social media website backend

***

## Description

This project is the backend of a web application that I made during the Scala internship in Novalite doo.
The application is a simple social media prototype which includes authentication using JWT, image uploading & editing,
commenting images, liking images & comments, organizing images into personal folders, enabling other
users to edit your images, basic image and comment-related CRUD operations, search fields related to image titles &
tags, a chart which displays the most popular users...

## Running

To run the postgresql, pgadmin & minio services use

    docker-compose up

Afterward, to run the server locally use

    sbt runLocal

## Usage

Since the documentation for this project is incomplete, refer to the services in the frontend project
to see how data is being sent to the backend.

You can also take a look at the http request routes in app/conf/routes.

I do not guarantee that I will fully document this project as I focused on learning as much as I could during the 3-week
period of the internship instead of creating a 100% complete project.

## Authors and acknowledgment

Author: Nikola Ognjenović https://github.com/NikolaOgnjenovic

A huge thank you to NovaLite doo and their employees, and especially my mentor Marko Stanić for guiding me
through the 3-week development this project (and the Scala backend).

## License

GNU GPLv3, see LICENSE.md