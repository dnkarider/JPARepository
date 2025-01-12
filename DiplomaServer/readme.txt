Backend listen to port 8080
There is a test user from the start in Container: login:admin; password:admin

!!!!!This version of project works only from Docker container
To start it from your localmachine you should change a line in application.properties from "spring.datasource.url=jdbc:postgresql://host.docker.internal:5432/postgres" to "spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
" and use docker-compose.yaml from docker/dockercomposeForLocalMachine/docker-compose.yaml



To launch the project you need to make docker-compose.yaml run. To do it you can:
 1. use commands from project directory: docker-compose down -v
                                         docker-compose up --build
                              OR
 2. launch from IDE by "Start" button